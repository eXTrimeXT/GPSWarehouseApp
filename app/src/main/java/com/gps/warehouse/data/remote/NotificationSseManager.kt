package com.gps.warehouse.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.gps.warehouse.data.local.LocalStorage
import com.gps.warehouse.data.remote.assets_dto.NotificationResponseDto
import com.gps.warehouse.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSseManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localStorage: LocalStorage
) {
    private var eventSource: EventSource? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var reconnectAttempts = 0
    private val maxReconnectDelayMs = 30_000L  // максимум 30 сек
    private val baseReconnectDelayMs = 1_000L   // начинаем с 1 сек

    private var isListening = false

    private val seenNotificationIds = mutableSetOf<Int>()
    private val gson = Gson()

    // Callback для UI — эмитит новые уведомления
    private var onNotificationReceived: ((NotificationResponseDto) -> Unit)? = null

    fun setOnNotificationReceived(callback: (NotificationResponseDto) -> Unit) {
        onNotificationReceived = callback
    }

    suspend fun startListening() {
        if (isListening) {
            Log.d("SSE_MANAGER", "Уже слушаем — выходим")
            return
        }
        isListening = true
        reconnectAttempts = 0
        connect()
    }

    private suspend fun connect() {
        val token = localStorage.getToken()
        if (token.isNullOrEmpty()) {
            Log.w("SSE_MANAGER", "Токен отсутствует, SSE не запущен")
            isListening = false
            return
        }

        eventSource?.cancel()

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val baseUrl = com.gps.warehouse.utils.Constants.ASSET_URL
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val fullUrl = "${safeBaseUrl}notifications/stream"

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .build()

        Log.d("SSE_MANAGER", "Подключение к SSE (попытка ${reconnectAttempts + 1}): ${request.url}")

        eventSource = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                Log.d("SSE_MANAGER", "SSE соединение открыто (HTTP ${response.code})")
                // Сбрасываем счётчик при успешном подключении
                reconnectAttempts = 0
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val responseDto = gson.fromJson(data, NotificationResponseDto::class.java)

                    // Показываем системные уведомления только для новых входящих
                    val newNotifications = responseDto.items.filter {
                        !seenNotificationIds.contains(it.notificationId)
                    }
                    seenNotificationIds.addAll(responseDto.items.map { it.notificationId })

                    newNotifications
                        .filter { it.direction == "incoming" && it.status == "unread" }
                        .forEach { newNotif ->
                            Log.d("SSE_MANAGER", "Новое уведомление: ${newNotif.eventTypeRu}")
                            val title = "Входящее уведомление"
                            val message = newNotif.eventTypeRu +
                                    if (!newNotif.assetName.isNullOrEmpty()) " (${newNotif.assetName})" else ""

                            NotificationHelper.showNotification(
                                context = context,
                                title = title,
                                message = message,
                                notificationId = newNotif.notificationId
                            )
                        }

                    // Эмитим в UI
                    onNotificationReceived?.invoke(responseDto)
                } catch (e: Exception) {
                    Log.e("SSE_MANAGER", "Ошибка парсинга SSE: ${e.message}", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("SSE_MANAGER", "SSE поток закрыт сервером")
                scheduleReconnect()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("SSE_MANAGER", "Ошибка SSE: ${t?.message}, HTTP=${response?.code}")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isListening) return
        reconnectJob?.cancel()

        reconnectAttempts++
        // Экспоненциальный backoff: 1s, 2s, 4s, 8s, 16s, 30s, 30s...
        val delayMs = minOf(
            baseReconnectDelayMs * (1 shl (reconnectAttempts - 1).coerceAtMost(5)),
            maxReconnectDelayMs
        )

        Log.d("SSE_MANAGER", "Переподключение через ${delayMs}ms (попытка $reconnectAttempts)")

        reconnectJob = scope.launch {
            delay(delayMs)
            if (isListening) {
                connect()
            }
        }
    }

    fun stopListening() {
        isListening = false
        reconnectJob?.cancel()
        reconnectJob = null
        eventSource?.cancel()
        eventSource = null
        seenNotificationIds.clear()
        Log.d("SSE_MANAGER", "SSE прослушивание остановлено")
    }
}