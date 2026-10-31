package com.adflowa.smartdayai

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime

class AlarmService : Service() {
    private var ringtone: Ringtone? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val title = intent?.getStringExtra("alarm_title") ?: "Your task is starting"

        if (action == "STOP") {
            stopAlarm()
            return START_NOT_STICKY
        }
        if (action == "SNOOZE") {
            stopAlarm()
            AlarmScheduler.schedule(this, "snooze_${System.currentTimeMillis()}", title, LocalDateTime.now().plusMinutes(5))
            return START_NOT_STICKY
        }

        val openAlarm = PendingIntent.getActivity(
            this, 10, Intent(this, AlarmActivity::class.java).putExtra("alarm_title", title)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = PendingIntent.getService(
            this, 11, Intent(this, AlarmService::class.java).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeAction = PendingIntent.getService(
            this, 12, Intent(this, AlarmService::class.java).setAction("SNOOZE").putExtra("alarm_title", title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, AlarmNotifications.CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Smart Day AI")
            .setContentText(title)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(openAlarm, true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopAction)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 5 min", snoozeAction)
            .build()
        startForeground(AlarmNotifications.ID, notification)

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)
            if (Build.VERSION.SDK_INT >= 28) ringtone?.isLooping = true
            ringtone?.play()
        } catch (_: Exception) { }
        return START_NOT_STICKY
    }

    private fun stopAlarm() {
        try { ringtone?.stop() } catch (_: Exception) { }
        ringtone = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try { ringtone?.stop() } catch (_: Exception) { }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
