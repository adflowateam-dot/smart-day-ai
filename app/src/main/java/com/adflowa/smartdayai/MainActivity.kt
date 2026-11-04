
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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingVoiceLanguage = "en-US"

    companion object {
        private const val AUDIO_PERMISSION_CODE = 201
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

    // NATIVE ANDROID VOICE RECOGNITION

    fun startNativeVoiceRecognition(language: String) {
        pendingVoiceLanguage = language.ifBlank {
            Locale.getDefault().toLanguageTag()
        }

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_CODE
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sendVoiceResult(
                "",
                "Speech recognition service is not available on this device."
            )
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        sendVoiceStatus("listening")
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "No speech recognized. Please try again."

                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "No speech detected. Please try again."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission is required."

                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Network problem. Please check your internet."

                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Voice recognition is busy. Try again."

                            else ->
                                "Voice recognition error. Please try again."
                        }

                        sendVoiceResult("", message)
                    }

                    override fun onResults(results: Bundle?) {
                        val spokenText = results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()
                            .orEmpty()

                        if (spokenText.isNotBlank()) {
                            sendVoiceResult(spokenText, "")
                        } else {
                            sendVoiceResult(
                                "",
                                "No speech was recognized."
                            )
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    pendingVoiceLanguage
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    pendingVoiceLanguage
                )
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }

            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            sendVoiceResult(
                "",
                "Could not start voice recognition."
            )
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

    private fun sendVoiceStatus(status: String) {
        if (!::webView.isInitialized) return

        runOnUiThread {
            webView.evaluateJavascript(
                "if (window.onNativeVoiceStatus) {" +
                    "window.onNativeVoiceStatus(" +
                    "${JSONObject.quote(status)});" +
                    "}",
                null
            )
        }
    }

    // MICROPHONE PERMISSION RESULT

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == AUDIO_PERMISSION_CODE) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startNativeVoiceRecognition(pendingVoiceLanguage)
            } else {
                sendVoiceResult(
                    "",
                    "Microphone permission was denied."
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::bridge.isInitialized) {
            bridge.restoreSavedAlarms()
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

// JAVASCRIPT VOICE BRIDGE

class VoiceBridge(private val activity: MainActivity) {

    @JavascriptInterface
    fun startListening(language: String) {
        activity.runOnUiThread {
            activity.startNativeVoiceRecognition(
                language.ifBlank {
                    Locale.getDefault().toLanguageTag()
                }
            )
        }
    }
}

// EXISTING ALARM BRIDGE - PRESERVED

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
