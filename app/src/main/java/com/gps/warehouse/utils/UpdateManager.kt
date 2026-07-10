package com.gps.warehouse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {
    private val TAG = "UpdateManager"
    private val apkFileName = "app_update.apk"

    // Параметры для уведомления
    private val channelId = "app_update_channel"
    private val notificationId = 1001

    // Файл всегда хранится во внутренней памяти приложения
    val cachedApkFile = File(context.cacheDir, apkFileName)

    data class VersionInfo(
        val jobId: Int,
        val version: Int,
        val versionCode: Int,
        val versionName: String
    )

    // Создание канала уведомлений (обязательно для Android 8+)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Обновления приложения",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Уведомления о прогрессе скачивания обновлений"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // 1. Проверка наличия обновлений на сервере
    suspend fun checkForUpdates(baseUrl: String): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl/version-info")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 15000
                readTimeout = 30000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned code: ${connection.responseCode}")
                return@withContext null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(responseBody)
            VersionInfo(
                jobId = json.getInt("job_id"),
                version = json.getInt("version"),
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Check update failed", e)
            null
        }
    }

    // 2. Скачивание APK в кэш. АВТОМАТИЧЕСКАЯ УСТАНОВКА ОТКЛЮЧЕНА.
    suspend fun downloadUpdate(baseUrl: String, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val apkUrl = "$baseUrl/download/apk"
            downloadApk(apkUrl, onProgress)
            onProgress(100)
            installApk(cachedApkFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            false
        }
    }

    // 3. Проверка, готов ли файл к установке
    fun isApkDownloaded(): Boolean = cachedApkFile.exists() && cachedApkFile.length() > 1024 * 1024

    // 4. Ручной запуск установки скачанного файла
    fun installUpdate(): Boolean {
        return try {
            if (!isApkDownloaded()) {
                Log.w(TAG, "APK not found or invalid")
                return false
            }
            installApk(cachedApkFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            false
        }
    }

    // Вспомогательный метод скачивания
    private fun downloadApk(url: String, onProgress: (Int) -> Unit = {}): File {
        createNotificationChannel()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Инициализируем уведомление
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Системная иконка скачивания
            .setContentTitle("Скачивание обновления")
            .setContentText("0%")
            .setOngoing(true) // Нельзя смахнуть во время скачивания
            .setProgress(100, 0, false)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        notificationManager.notify(notificationId, builder.build())

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.android.package-archive")
            connectTimeout = 30000
            readTimeout = 300000
            setRequestProperty("Accept-Encoding", "identity")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IllegalStateException("Download failed: HTTP ${connection.responseCode}")
        }

        val contentLength = connection.contentLengthLong
        var downloadedBytes = 0L
        val bufferSize = 8192
        val buffer = ByteArray(bufferSize)

        var lastProgressUpdate = 0L
        var lastProgress = -1
        val PROGRESS_UPDATE_INTERVAL_MS = 200L

        connection.inputStream.use { input ->
            FileOutputStream(cachedApkFile).use { output ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloadedBytes += read

                    if (contentLength > 0) {
                        val progress = ((downloadedBytes * 100) / contentLength).toInt().coerceIn(0, 100)
                        val now = System.currentTimeMillis()
                        if (progress != lastProgress && now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                            onProgress(progress)
                            lastProgress = progress
                            lastProgressUpdate = now

                            // === ОБНОВЛЯЕМ ПРОГРЕСС В УВЕДОМЛЕНИИ ===
                            builder.setProgress(100, progress, false)
                                .setContentText("$progress%")
                            notificationManager.notify(notificationId, builder.build())
                        }
                    }
                }
            }
        }
        connection.disconnect()

        // === УВЕДОМЛЕНИЕ О ЗАВЕРШЕНИИ ===
        builder.setContentText("Скачивание завершено. Запуск установки...")
            .setProgress(0, 0, false)
            .setOngoing(false)
        notificationManager.notify(notificationId, builder.build())

        return cachedApkFile
    }

    // Запуск системного установщика
    private fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}