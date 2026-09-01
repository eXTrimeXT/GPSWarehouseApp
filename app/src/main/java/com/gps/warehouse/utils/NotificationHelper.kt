package com.gps.warehouse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.gps.warehouse.R
import com.gps.warehouse.ui.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "gps_warehouse_notifications"
    private const val CHANNEL_NAME = "Уведомления склада"

    // Ключи для Intent
    const val EXTRA_HIGHLIGHT_NOTIFICATION_ID = "highlight_notification_id"
    const val EXTRA_NAVIGATE_TO = "navigate_to"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления об изменении статусов активов и инвентаризации"
            enableVibration(false)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Создаем Intent для открытия MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HIGHLIGHT_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_NAVIGATE_TO, "asset_notifications")
        }

        // Создаем PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // Уникальный requestCode для каждого уведомления
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // <-- Привязываем клик
            .build()

        notificationManager.notify(notificationId, notification)
    }
}