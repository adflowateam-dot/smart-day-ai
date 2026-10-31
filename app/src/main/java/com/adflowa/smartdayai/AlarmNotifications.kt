package com.adflowa.smartdayai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AlarmNotifications {
    const val CHANNEL = "smart_day_ai_alarm"
    const val ID = 7319

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL, "Smart Day AI alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled task alarms"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
