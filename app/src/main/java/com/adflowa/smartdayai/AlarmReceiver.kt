package com.adflowa.smartdayai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", intent.getStringExtra("alarm_id") ?: "task")
            putExtra("alarm_title", intent.getStringExtra("alarm_title") ?: "Scheduled task")
        }
        ContextCompat.startForegroundService(context, service)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmBridge(context).restoreSavedAlarms()
    }
}
