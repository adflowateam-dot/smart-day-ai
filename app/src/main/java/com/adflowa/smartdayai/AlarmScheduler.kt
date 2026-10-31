package com.adflowa.smartdayai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {
    fun schedule(context: Context, id: String, title: String, time: LocalDateTime) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", id)
            putExtra("alarm_title", title)
        }
        val pending = PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val millis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    fun cancelSaved(context: Context, raw: String?) {
        if (raw == null) return
        try {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val items = JSONArray(raw)
            for (i in 0 until items.length()) {
                val id = items.getJSONObject(i).optString("id")
                val pending = PendingIntent.getBroadcast(
                    context, id.hashCode(), Intent(context, AlarmReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pending != null) manager.cancel(pending)
            }
        } catch (_: Exception) { }
    }
}
