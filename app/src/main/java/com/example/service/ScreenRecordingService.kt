package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.BanglaAIApplication
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ScreenRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.RECORDING_START"
        const val ACTION_PAUSE = "com.example.service.RECORDING_PAUSE"
        const val ACTION_RESUME = "com.example.service.RECORDING_RESUME"
        const val ACTION_STOP = "com.example.service.RECORDING_STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0)
        val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

        private val _lastSavedVideoUri = MutableStateFlow<String?>(null)
        val lastSavedVideoUri: StateFlow<String?> = _lastSavedVideoUri.asStateFlow()
    }

    private val tag = "ScreenRecordingService"
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var videoFile: File? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                if (resultCode != 0 && resultData != null) {
                    startForeground(2002, buildNotification("রেকর্ডিং শুরু হচ্ছে...", 0))
                    startRecording(resultCode, resultData)
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val screenDensity = metrics.densityDpi

        // Safe width & height for encoder
        val width = (screenWidth / 2) * 2
        val height = (screenHeight / 2) * 2

        val dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: filesDir
        videoFile = File(dir, "BanglaAI_Recording_${System.currentTimeMillis()}.mp4")

        try {
            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(width, height)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(4 * 1024 * 1024)
                setOutputFile(videoFile?.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "BanglaAIScreenRecord",
                width,
                height,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            _isRecording.value = true
            _isPaused.value = false
            _elapsedSeconds.value = 0
            startTimer()
        } catch (e: Exception) {
            Log.e(tag, "Failed to start media recorder", e)
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && !_isPaused.value) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
                pauseTimer()
                updateNotification("রেকর্ডিং বিরতিতে আছে", _elapsedSeconds.value)
            } catch (e: Exception) {
                Log.e(tag, "Failed to pause recording", e)
            }
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && _isPaused.value) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
                startTimer()
                updateNotification("রেকর্ডিং চলছে", _elapsedSeconds.value)
            } catch (e: Exception) {
                Log.e(tag, "Failed to resume recording", e)
            }
        }
    }

    private fun stopRecording() {
        stopTimer()
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(tag, "Error stopping recorder", e)
        }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        mediaProjection?.stop()
        mediaProjection = null

        _isRecording.value = false
        _isPaused.value = false

        videoFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                _lastSavedVideoUri.value = file.absolutePath
                copyToMediaStore(file)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun copyToMediaStore(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, file.name)
                put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/BanglaAI")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
            }
            contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            Log.w(tag, "Unable to insert into media store", e)
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                _elapsedSeconds.value = _elapsedSeconds.value + 1
                val timeStr = formatSeconds(_elapsedSeconds.value)
                updateNotification("রেকর্ড হচ্ছে ($timeStr)", _elapsedSeconds.value)
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 1000)
    }

    private fun pauseTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
    }

    private fun stopTimer() {
        pauseTimer()
        timerRunnable = null
    }

    private fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun buildNotification(status: String, seconds: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BanglaAIApplication.RECORDING_CHANNEL_ID)
            .setContentTitle("Bangla AI স্ক্রিন রেকর্ডার")
            .setContentText("$status • ${formatSeconds(seconds)}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_media_pause, "থামান", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification(status: String, seconds: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(2002, buildNotification(status, seconds))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_isRecording.value) {
            stopRecording()
        }
    }
}
