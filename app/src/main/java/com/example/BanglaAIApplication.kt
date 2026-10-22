package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

import android.webkit.WebView

class BanglaAIApplication : Application() {

    companion object {
        const val FLOATING_CHANNEL_ID = "bangla_ai_floating_service"
        const val RECORDING_CHANNEL_ID = "bangla_ai_recording_service"
        lateinit var instance: BanglaAIApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initWebViewSafeguards()
        createNotificationChannels()
    }

    private fun initWebViewSafeguards() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val currentProcess = getProcessName()
                if (packageName != currentProcess) {
                    WebView.setDataDirectorySuffix(currentProcess)
                }
            }
        } catch (e: Exception) {
            // Process name or data directory already set
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val floatingChannel = NotificationChannel(
                FLOATING_CHANNEL_ID,
                "Bangla AI Floating Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the floating overlay assistant bubble"
                setShowBadge(false)
            }

            val recordingChannel = NotificationChannel(
                RECORDING_CHANNEL_ID,
                "Bangla AI Screen Recorder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Displays active screen recording timer and controls"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(floatingChannel)
            notificationManager.createNotificationChannel(recordingChannel)
        }
    }
}
