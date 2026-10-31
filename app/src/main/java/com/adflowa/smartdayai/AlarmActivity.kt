package com.adflowa.smartdayai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskTitle = intent.getStringExtra("alarm_title") ?: "Your task is starting"
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(0xFF101322.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "Smart Day AI"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
        })
        layout.addView(TextView(this).apply {
            text = taskTitle
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 28, 0, 30)
        })
        layout.addView(Button(this).apply {
            text = "STOP"
            setOnClickListener {
                stopService(Intent(this@AlarmActivity, AlarmService::class.java))
                finish()
            }
        })
        layout.addView(Button(this).apply {
            text = "SNOOZE 5 MIN"
            setOnClickListener {
                startService(Intent(this@AlarmActivity, AlarmService::class.java)
                    .setAction("SNOOZE").putExtra("alarm_title", taskTitle))
                finish()
            }
        })
        setContentView(layout)
    }
}
