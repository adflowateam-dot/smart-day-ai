package com.adflowa.smartdayai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var bridge: AlarmBridge

    companion object {
        private const val VOICE_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlarmNotifications.createChannel(this)
        requestNotificationPermission()

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        bridge = AlarmBridge(this)

        webView.addJavascriptInterface(bridge, "AndroidAlarms")
        webView.addJavascriptInterface(VoiceBridge(this), "AndroidVoice")

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    fun startNativeVoiceRecognition(language: String) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            startActivityForResult(intent, VOICE_REQUEST_CODE)

        } catch (_: Exception) {
            sendVoiceResult("", "Voice recognition is not available on this device")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

                val spokenText = results?.firstOrNull().orEmpty()

                if (spokenText.isNotBlank()) {
                    sendVoiceResult(spokenText, "")
                } else {
                    sendVoiceResult("", "No speech was recognized")
                }
            } else {
                sendVoiceResult("", "Voice recognition cancelled")
            }
        }
    }

    private fun sendVoiceResult(text: String, error: String) {
        if (!::webView.isInitialized) return

        val safeText = JSONObject.quote(text)
        val safeError = JSONObject.quote(error)

        runOnUiThread {
            webView.evaluateJavascript(
                "if (window.onNativeVoiceResult) {" +
                    "window.onNativeVoiceResult($safeText, $safeError);" +
                    "}",
                null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::bridge.isInitialized) bridge.restoreSavedAlarms()
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

class VoiceBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun startListening(language: String) {
        activity.runOnUiThread {
            activity.startNativeVoiceRecognition(
                language.ifBlank { Locale.getDefault().toLanguageTag() }
            )
        }
    }
}

class AlarmBridge(private val context: Context) {

    private val prefs = context.getSharedPreferences(
        "smart_day_alarms",
        Context.MODE_PRIVATE
    )

    @JavascriptInterface
    fun scheduleTasks(json: String) {
        prefs.edit().putString("tasks", json).apply()
        restoreSavedAlarms()

        val manager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as android.app.AlarmManager

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !manager.canScheduleExactAlarms()
        ) {
            try {
                val request = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                ).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(request)

                Toast.makeText(
                    context,
                    "Allow Alarms & reminders for Smart Day AI",
                    Toast.LENGTH_LONG
                ).show()

            } catch (_: Exception) {
            }
        }
    }

    @JavascriptInterface
    fun cancelAll() {
        AlarmScheduler.cancelSaved(
            context,
            prefs.getString("tasks", null)
        )

        prefs.edit().remove("tasks").apply()
    }

    fun restoreSavedAlarms() {
        val raw = prefs.getString("tasks", null) ?: return

        try {
            val items = JSONArray(raw)

            AlarmScheduler.cancelSaved(context, raw)

            val now = LocalDateTime.now()

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)

                val date = LocalDate.parse(item.getString("date"))
                val time = LocalTime.of(
                    item.getInt("hour"),
                    item.getInt("minute")
                )

                val whenAt = LocalDateTime.of(date, time)

                if (whenAt.isAfter(now)) {
                    AlarmScheduler.schedule(
                        context,
                        item.getString("id"),
                        item.getString("title"),
                        whenAt
                    )
                }
            }

        } catch (_: Exception) {
        }
    }
}
