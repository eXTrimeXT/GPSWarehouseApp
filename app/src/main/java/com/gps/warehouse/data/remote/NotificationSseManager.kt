package com.gps.warehouse.data.remote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.gps.warehouse.data.local.LocalStorage
import com.gps.warehouse.data.remote.assets_dto.NotificationResponseDto
import com.gps.warehouse.utils.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val seenNotificationIds = mutableSetOf<Int>()
    private val gson = Gson()

    suspend fun startListening() {
        val token = localStorage.getToken()
        if (token.isNullOrEmpty()) {
            Log.w("SSE_MANAGER", "Токен отсутствует, SSE не запущен")
            return
        }

        eventSource?.cancel()

        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val baseUrl = com.gps.warehouse.utils.Constants.ASSET_URL
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val fullUrl = "${safeBaseUrl}notifications/stream?direction=all"

        val request = Request.Builder()
            .url(fullUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .build()

        Log.d("SSE_MANAGER", "🔌 Глобальное подключение к SSE: ${request.url}")

        eventSource = EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val responseDto = gson.fromJson(data, NotificationResponseDto::class.java)

                    // Находим уведомления, которых мы еще не видели
                    val newNotifications = responseDto.items.filter {
                        !seenNotificationIds.contains(it.notificationId)
                    }

                    // Добавляем все текущие ID в набор "виденных"
                    seenNotificationIds.addAll(responseDto.items.map { it.notificationId })

                    // ФИЛЬТРУЕМ: показываем системное уведомление ТОЛЬКО если оно входящее И непрочитанное
                    val notificationsToShow = newNotifications.filter {
                        it.direction == "incoming" && it.status == "unread"
                    }

                    // Показываем системное уведомление для каждого НОВОГО события
                    notificationsToShow.forEach { newNotif ->
                        Log.d("SSE_MANAGER", "Новое уведомление: ${newNotif.eventTypeRu}")

                        // Формируем текст всплывающего уведомления
                        val title = if (newNotif.direction == "incoming") "Входящее уведомление" else "Исходящее уведомление"
                        val message = newNotif.eventTypeRu + if (!newNotif.assetName.isNullOrEmpty()) " (${newNotif.assetName})" else ""

                        NotificationHelper.showNotification(
                            context = context,
                            title = title,
                            message = message,
                            notificationId = newNotif.notificationId
                        )
                    }

                } catch (e: Exception) {
                    Log.e("SSE_MANAGER", "Ошибка парсинга SSE: ${e.message}", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d("SSE_MANAGER", "SSE поток закрыт")
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e("SSE_MANAGER", "Ошибка SSE: ${t?.message}")
            }
        })
    }

    fun stopListening() {
        eventSource?.cancel()
        eventSource = null
        Log.d("SSE_MANAGER", "SSE прослушивание остановлено")
    }
}