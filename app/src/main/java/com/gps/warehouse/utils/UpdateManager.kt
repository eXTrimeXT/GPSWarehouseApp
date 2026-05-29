package com.gps.warehouse.utils

import android.content.Context
import android.content.Intent
import android.util.Log
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

    // Файл всегда хранится во внутренней памяти приложения
    val cachedApkFile = File(context.cacheDir, apkFileName)

    data class VersionInfo(
        val jobId: Int,
        val version: Int,
        val versionCode: Int,
        val versionName: String
    )

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
                        }
                    }
                }
            }
        }
        connection.disconnect()
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