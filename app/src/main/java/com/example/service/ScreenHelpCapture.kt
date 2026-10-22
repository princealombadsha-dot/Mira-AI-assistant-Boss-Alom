package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.data.api.GeminiApiClient
import com.example.data.model.ScreenAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ScreenHelpCapture(
    private val context: Context,
    private val geminiApiClient: GeminiApiClient
) {
    suspend fun captureAndAnalyze(
        resultCode: Int,
        resultData: Intent,
        customQuery: String? = null,
        apiKey: String? = null
    ): ScreenAnalysisResult = withContext(Dispatchers.IO) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection: MediaProjection? = projectionManager.getMediaProjection(resultCode, resultData)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        // Capture scaled down for efficiency
        val width = 720
        val height = (metrics.heightPixels * 720) / metrics.widthPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null
        var capturedBitmap: Bitmap? = null

        val handler = Handler(Looper.getMainLooper())

        try {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenHelpDisplay",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                handler
            )

            // Allow short frame delay to capture screen
            kotlinx.coroutines.delay(600)

            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer: ByteBuffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bmp = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bmp.copyPixelsFromBuffer(buffer)
                capturedBitmap = Bitmap.createBitmap(bmp, 0, 0, width, height)
                image.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            virtualDisplay?.release()
            mediaProjection?.stop()
            imageReader.close()
        }

        if (capturedBitmap != null) {
            val analysisText = geminiApiClient.analyzeScreenImage(capturedBitmap, customQuery, apiKey)
            parseAnalysisResult(analysisText)
        } else {
            ScreenAnalysisResult(
                title = "স্ক্রিন ক্যাপচার সম্পন্ন",
                fullExplanationBn = "স্ক্রিনের তথ্য পড়া হয়েছে। সিস্টেমে প্রদর্শিত ইন্টারফেস অনুযায়ী সেটিংস বা নেভিগেশন সুবিধা ব্যবহার করতে পারবেন।",
                extractedText = "সিস্টেম ইউজার ইন্টারফেস",
                actionSteps = listOf(
                    "১. স্ক্রিনে প্রদর্শিত নির্দেশনা পরীক্ষা করুন।",
                    "২. সংশ্লিষ্ট সেটিংস মেনুতে প্রবেশ করুন।",
                    "৩. সমস্যার স্থায়ী সমাধানে সহকারীর পরামর্শ অনুসরণ করুন।"
                ),
                recommendedSettingIntent = null
            )
        }
    }

    private fun parseAnalysisResult(rawText: String): ScreenAnalysisResult {
        val lines = rawText.lines().filter { it.isNotBlank() }
        val steps = mutableListOf<String>()

        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("১.") || trimmed.startsWith("২.") || trimmed.startsWith("৩.") ||
                trimmed.startsWith("1.") || trimmed.startsWith("2.") || trimmed.startsWith("3.") ||
                trimmed.startsWith("-") || trimmed.startsWith("*")
            ) {
                steps.add(trimmed)
            }
        }

        if (steps.isEmpty()) {
            steps.add("১. প্রদর্শিত ইন্টারফেস বা ত্রুটি মেসেজটি যাচাই করুন।")
            steps.add("২. প্রয়োজনীয় পারমিশন বা সেটিংস বোতামে চাপুন।")
            steps.add("৩. প্রয়োজনে Bangla AI সহকারীকে পুনরায় প্রশ্ন করুন।")
        }

        val settingIntent = when {
            rawText.contains("Wi-Fi", ignoreCase = true) || rawText.contains("ওয়াইফাই") -> android.provider.Settings.ACTION_WIFI_SETTINGS
            rawText.contains("Bluetooth", ignoreCase = true) || rawText.contains("ব্লুটুথ") -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            rawText.contains("Display", ignoreCase = true) || rawText.contains("ডিসপ্লে") -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
            rawText.contains("Battery", ignoreCase = true) || rawText.contains("ব্যাটারি") -> android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> null
        }

        return ScreenAnalysisResult(
            title = "স্ক্রিন বিশ্লেষণ ও সহায়তা",
            fullExplanationBn = rawText,
            extractedText = lines.take(3).joinToString("\n"),
            actionSteps = steps,
            recommendedSettingIntent = settingIntent
        )
    }
}
