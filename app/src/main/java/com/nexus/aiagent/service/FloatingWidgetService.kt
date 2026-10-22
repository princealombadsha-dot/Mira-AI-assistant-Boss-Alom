package com.nexus.aiagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nexus.aiagent.data.GeminiApiClient
import com.nexus.aiagent.data.NexusDatabase
import com.nexus.aiagent.data.VoiceInteractionEntity
import com.nexus.aiagent.utils.AudioVisualizerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Persistent overlay bubble & continuous STT/TTS manager for BD-NEXUS AI Agent.
 * Features drag navigation, audio-reactive sphere visualizer, autonomous OS command execution,
 * and persistent Room database logging of voice interaction history.
 */
class FloatingWidgetService : Service(), TextToSpeech.OnInitListener {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: FrameLayout
    private lateinit var visualizerView: AudioVisualizerView
    private lateinit var speechRecognizer: SpeechRecognizer
    private var textToSpeech: TextToSpeech? = null
    private var isListeningContinuous = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val geminiApiClient = GeminiApiClient()
    private val channelId = "nexus_floating_service_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()

        // Initialize Text to Speech
        textToSpeech = TextToSpeech(this, this)

        // 1. Inflate floating glowing sphere container
        floatingView = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        // 2. Add custom interactive glowing visualizer sphere (75dp)
        val sphereSizePx = (75 * resources.displayMetrics.density).toInt()
        visualizerView = AudioVisualizerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(sphereSizePx, sphereSizePx)
        }
        floatingView.addView(visualizerView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 180
        }

        // 3. Drag touch listener for moving the floating ball & tap to toggle listening
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0.0f
            private var initialTouchY = 0.0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (dx * dx + dy * dy > 25) {
                            isClick = false
                        }
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(floatingView, params)
                        } catch (e: Exception) {
                            Log.e("FloatingWidgetService", "Error updating layout", e)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleContinuousListening()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e("FloatingWidgetService", "Failed to add floating view", e)
        }

        setupSpeechRecognizer()
    }

    private fun startAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nexus AI Floating Agent",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BD-NEXUS AI Agent")
            .setContentText("Autonomous System Controller is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2026, notification)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("bn", "BD"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                visualizerView.setState(AudioVisualizerView.State.LISTENING)
            }

            override fun onBeginningOfSpeech() {
                visualizerView.setState(AudioVisualizerView.State.LISTENING)
            }

            override fun onRmsChanged(rmsdB: Float) {
                visualizerView.setRms(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                visualizerView.setState(AudioVisualizerView.State.THINKING)
                if (isListeningContinuous) {
                    serviceScope.launch {
                        kotlinx.coroutines.delay(1000)
                        startListening()
                    }
                }
            }

            override fun onError(error: Int) {
                Log.w("FloatingWidgetService", "Speech recognition error: $error, restarting automatically in 1000ms...")
                visualizerView.setState(AudioVisualizerView.State.IDLE)
                // Always-on guarantee: automatically restart on error after 1 second
                if (isListeningContinuous) {
                    serviceScope.launch {
                        kotlinx.coroutines.delay(1000)
                        startListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    Log.i("FloatingWidgetService", "Spoken voice command: $spokenText")
                    if (spokenText.contains("থামো", ignoreCase = true) ||
                        spokenText.contains("stop", ignoreCase = true) ||
                        spokenText.contains("বন্ধ", ignoreCase = true)
                    ) {
                        isListeningContinuous = false
                        visualizerView.setState(AudioVisualizerView.State.IDLE)
                        speakFeedback("মাইক্রোফোন বন্ধ করা হলো।")
                    } else {
                        processAiCommand(spokenText)
                    }
                } else {
                    visualizerView.setState(AudioVisualizerView.State.IDLE)
                    if (isListeningContinuous) {
                        serviceScope.launch {
                            kotlinx.coroutines.delay(1000)
                            startListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun toggleContinuousListening() {
        isListeningContinuous = !isListeningContinuous
        if (isListeningContinuous) {
            speakFeedback("MYRA AI অলওয়েজ-অন মাইক্রোফোন সক্রিয় আছে, আমি শুনছি।")
            startListening()
        } else {
            speechRecognizer.stopListening()
            visualizerView.setState(AudioVisualizerView.State.IDLE)
            speakFeedback("লিসেনিং বন্ধ করা হয়েছে।")
        }
    }

    private fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e("FloatingWidgetService", "Failed to start listening", e)
        }
    }

    private fun processAiCommand(command: String) {
        visualizerView.setState(AudioVisualizerView.State.THINKING)

        serviceScope.launch {
            val lower = command.lowercase(Locale.ROOT)
            val aiResponse: String
            var executedAction: String? = null

            // Specific Myra voice intents & ReAct device automation
            if (lower.contains("শায়রি") || lower.contains("shayari")) {
                aiResponse = "আচ্ছা শুনুন, বন্ধু কত খারাপ কেন না হোক, তার থেকে বন্ধুত্ব ভাঙিস না..."
                executedAction = "SAY_SHAYARI"
            } else if (lower.contains("গান") || lower.contains("song")) {
                aiResponse = "আপনার পছন্দের গানটি ইউটিউব বা ফেসবুক থেকে খুঁজে প্লে করা হচ্ছে..."
                val launched = NexusAccessibilityService.instance?.openAppSettings("com.google.android.youtube") ?: false
                executedAction = "LAUNCH_SONG_YOUTUBE: $launched"
            } else if (lower.contains("home") || lower.contains("হোম")) {
                val success = NexusAccessibilityService.instance?.performGlobalActionHome() ?: false
                executedAction = "GLOBAL_ACTION_HOME: $success"
                aiResponse = "হোম স্ক্রিনে যাওয়া হয়েছে।"
            } else if (lower.contains("youtube") || lower.contains("ইউটিউব")) {
                val success = NexusAccessibilityService.instance?.openAppSettings("com.google.android.youtube") ?: false
                executedAction = "LAUNCH_YOUTUBE: $success"
                aiResponse = "ইউটিউব অ্যাপ চালু করা হচ্ছে।"
            } else if (lower.contains("chrome") || lower.contains("ব্রাউজার")) {
                val success = NexusAccessibilityService.instance?.openAppSettings("com.android.chrome") ?: false
                executedAction = "LAUNCH_CHROME: $success"
                aiResponse = "ক্রোম ব্রাউজার চালু করা হয়েছে।"
            } else if (lower.contains("copy") || lower.contains("কপি")) {
                aiResponse = "টেক্সট ক্লিপবোর্ডে কপি করা হয়েছে।"
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nexus AI Copied", aiResponse)
                clipboard.setPrimaryClip(clip)
                executedAction = "CLIPBOARD_COPY"
            } else if (lower.contains("টাইপ") || lower.contains("type") || lower.contains("inject")) {
                aiResponse = "অ্যাক্টিভ অ্যাপ্লিকেশনে টেক্সট ইনপুট করা হয়েছে।"
                val textToInject = aiResponse.take(150)
                val success = NexusAccessibilityService.instance?.injectTextIntoFocused(textToInject) ?: false
                executedAction = "INJECT_TEXT: $success"
            } else {
                aiResponse = geminiApiClient.sendCommandToGemini(command)
            }

            // Persist voice interaction history to Room Database
            try {
                withContext(Dispatchers.IO) {
                    val db = NexusDatabase.getDatabase(applicationContext)
                    val entity = VoiceInteractionEntity(
                        spokenPrompt = command,
                        aiResponse = aiResponse,
                        language = if (command.any { it.code in 0x0980..0x09FF }) "bn-BD" else "en-US",
                        status = "SUCCESS",
                        executedAction = executedAction,
                        timestamp = System.currentTimeMillis()
                    )
                    db.voiceInteractionDao().insertInteraction(entity)
                }
            } catch (e: Exception) {
                Log.e("FloatingWidgetService", "Error persisting voice interaction to Room", e)
            }

            // Speak feedback through TTS and Visualizer
            visualizerView.setState(AudioVisualizerView.State.SPEAKING)
            speakFeedback(aiResponse)

            // Continue listening if continuous mode is active
            if (isListeningContinuous) {
                startListening()
            } else {
                visualizerView.setState(AudioVisualizerView.State.IDLE)
            }
        }
    }

    private fun speakFeedback(text: String) {
        val clean = text.replace(Regex("[*#_`\\[\\]]"), "").take(250)
        textToSpeech?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts_utterance")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        if (::floatingView.isInitialized) {
            try {
                windowManager.removeView(floatingView)
            } catch (e: Exception) {
                Log.w("FloatingWidgetService", "Error removing floating view on destroy", e)
            }
        }
        speechRecognizer.destroy()
    }
}
