package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.preferences.AssistantPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    RESTING,
    ERROR
}

class VoiceAssistantManager(
    private val context: Context,
    private val preferences: AssistantPreferences
) : TextToSpeech.OnInitListener {

    private val tag = "VoiceAssistantManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    var onCommandRecognized: ((String) -> Unit)? = null
    var onSessionDismissRequested: (() -> Unit)? = null
    var onRestModeRequested: (() -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var idleTimeoutRunnable: Runnable? = null

    init {
        initTts()
        initRecognizer()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            // Attempt Bengali locale, fallback to Indian Bengali or default
            val bengaliBd = Locale("bn", "BD")
            val bengaliIn = Locale("bn", "IN")
            val result = textToSpeech?.setLanguage(bengaliBd)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val fallbackResult = textToSpeech?.setLanguage(bengaliIn)
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.language = Locale.getDefault()
                }
            }

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        if (_voiceState.value == VoiceState.SPEAKING) {
                            if (preferences.handsFreeEnabled) {
                                startListening()
                            } else {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            })
        }
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.LISTENING
            resetIdleTimeout()
        }

        override fun onBeginningOfSpeech() {
            cancelIdleTimeout()
            // Barge-in: if Maira is speaking and owner speaks, interrupt Maira immediately!
            if (_voiceState.value == VoiceState.SPEAKING) {
                stopSpeaking()
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize rmsdB (-2 to 10 typical) to 0.0 - 1.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioAmplitude.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _audioAmplitude.value = 0f
            _voiceState.value = VoiceState.THINKING
        }

        override fun onError(error: Int) {
            _audioAmplitude.value = 0f
            Log.w(tag, "SpeechRecognizer error: $error")
            if ((preferences.handsFreeEnabled || preferences.alwaysOnMicEnabled) && _voiceState.value != VoiceState.RESTING) {
                mainHandler.postDelayed({
                    if ((preferences.handsFreeEnabled || preferences.alwaysOnMicEnabled) && _voiceState.value != VoiceState.RESTING) {
                        startListening()
                    } else {
                        _voiceState.value = VoiceState.IDLE
                    }
                }, 500)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }

        override fun onResults(results: Bundle?) {
            _audioAmplitude.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            _recognizedText.value = text

            if (text.isNotBlank()) {
                handleRecognizedVoice(text)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrBlank()) {
                _recognizedText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleRecognizedVoice(query: String) {
        val trimmed = query.trim().lowercase()
        val owner = preferences.ownerTitle

        // Wake-word recognition if enabled
        if (preferences.wakeWordEnabled && (trimmed == "হাই মাইরা" || trimmed == "মাইরা" || trimmed == "hey maira" || trimmed == "maira")) {
            _voiceState.value = VoiceState.SPEAKING
            speak("জি $owner, আমি প্রস্তুত আছি! বলুন কী করতে পারি?")
            return
        }

        // Check for specific commands
        when {
            trimmed.contains("বিশ্রাম মোডে যাও") || trimmed.contains("বিশ্রাম নাও") || trimmed.contains("ঘুমিয়ে পড়") -> {
                speak("$owner, আমি বিশ্রাম মোডে যাচ্ছি। প্রয়োজনে 'হাই মাইরা' বললেই আমি আবার হাজির হব।")
                _voiceState.value = VoiceState.RESTING
                onRestModeRequested?.invoke()
                return
            }
            trimmed.contains("বন্ধ হও") || trimmed.contains("চলে যাও") || trimmed.contains("বিদায়") -> {
                speak("বিদায় $owner! যেকোনো সময় ডাকবেন।")
                onSessionDismissRequested?.invoke()
                return
            }
            trimmed.contains("থামো") || trimmed.contains("চুপ করো") || trimmed.contains("কথা বন্ধ করো") || trimmed.contains("stop") -> {
                stopSpeaking()
                _voiceState.value = VoiceState.IDLE
                return
            }
            trimmed.contains("শুনতে থাকো") || trimmed.contains("শোনো") -> {
                startListening()
                return
            }
            trimmed.contains("গান") || trimmed.contains("ইউটিউব") || trimmed.contains("song") || trimmed.contains("youtube") -> {
                speak("ইউটিউব থেকে আপনার কাঙ্ক্ষিত গানটি খুঁজে বের করে প্লে করা হচ্ছে...")
                onCommandRecognized?.invoke(query)
                return
            }
            trimmed.contains("লাইক") || trimmed.contains("কমেন্ট") || trimmed.contains("like") || trimmed.contains("comment") -> {
                speak("স্ক্রিনের সোশ্যাল মিডিয়া বাটনে লাইক ও কমেন্ট অ্যাকশন সম্পন্ন করা হচ্ছে...")
                onCommandRecognized?.invoke(query)
                return
            }
            trimmed.contains("অ্যাপ") || trimmed.contains("ইনস্টল") || trimmed.contains("app") || trimmed.contains("install") -> {
                speak("প্লে-স্টোর ওপেন করে অ্যাপ ইনস্টলেশন প্রসেস শুরু করা হচ্ছে...")
                onCommandRecognized?.invoke(query)
                return
            }
            trimmed.contains("সাইরি") || trimmed.contains("শায়রি") || trimmed.contains("শায়রি") || trimmed.contains("shayari") -> {
                speak("একটি চমৎকার শায়রি শুনুন: পানির থেকে শীতল কিছু নেই, আর ভালো বন্ধুর সম্পর্কের চেয়ে বিশ্বস্ত কিছু নেই!")
                return
            }
        }

        // Strip wake-word prefix if user said "মাইরা, YouTube খোলো" or "হাই মাইরা, ক্যামেরা খোলো"
        val cleanQuery = query
            .replace("হাই মাইরা,", "", ignoreCase = true)
            .replace("হাই মাইরা", "", ignoreCase = true)
            .replace("মাইরা,", "", ignoreCase = true)
            .replace("মাইরা", "", ignoreCase = true)
            .replace("hey maira,", "", ignoreCase = true)
            .replace("hey maira", "", ignoreCase = true)
            .trim()

        onCommandRecognized?.invoke(if (cleanQuery.isNotBlank()) cleanQuery else query)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.ERROR
            return
        }

        stopSpeaking()
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-IN", "en-US"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
                _voiceState.value = VoiceState.LISTENING
                resetIdleTimeout()
            } catch (e: Exception) {
                Log.e(tag, "Failed to start listening", e)
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun stopListening() {
        cancelIdleTimeout()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop listening", e)
        }
        _audioAmplitude.value = 0f
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun speak(text: String) {
        if (preferences.isMuted || !isTtsInitialized) {
            return
        }
        stopListening()
        textToSpeech?.setSpeechRate(preferences.speakingRate)
        textToSpeech?.setPitch(preferences.speakingPitch)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "BanglaAIUtterance_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "BanglaAIUtterance")
    }

    fun stopSpeaking() {
        if (isTtsInitialized) {
            textToSpeech?.stop()
        }
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun enterRestMode() {
        stopSpeaking()
        stopListening()
        _voiceState.value = VoiceState.RESTING
    }

    fun resumeFromRest() {
        _voiceState.value = VoiceState.IDLE
        speak("আমি জেগে উঠেছি। কী সাহায্য করতে পারি বলুন?")
    }

    fun triggerManualListening() {
        speak("আমি প্রস্তুত! বলুন কী করতে হবে?")
        mainHandler.postDelayed({
            startListening()
        }, 2000)
    }

    private fun resetIdleTimeout() {
        cancelIdleTimeout()
        val timeoutMs = (preferences.idleTimeoutSeconds * 1000).toLong()
        idleTimeoutRunnable = Runnable {
            if (_voiceState.value == VoiceState.LISTENING) {
                stopListening()
            }
        }
        mainHandler.postDelayed(idleTimeoutRunnable!!, timeoutMs)
    }

    private fun cancelIdleTimeout() {
        idleTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        idleTimeoutRunnable = null
    }

    fun destroy() {
        cancelIdleTimeout()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error destroying speech recognizer", e)
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }
}
