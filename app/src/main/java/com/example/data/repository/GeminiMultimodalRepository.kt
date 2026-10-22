package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// ============================================================================
// 1. Retrofit Request and Response Models
// ============================================================================

@JsonClass(generateAdapter = true)
data class GeminiVisionRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.4f,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 1500
)

@JsonClass(generateAdapter = true)
data class GeminiVisionResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

// ============================================================================
// 2. Retrofit API Service Definition
// ============================================================================

interface GeminiMultimodalRetrofitService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiVisionRequest
    ): GeminiVisionResponse
}

// ============================================================================
// 3. Retrofit Client Singleton Factory
// ============================================================================

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiMultimodalRetrofitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiMultimodalRetrofitService::class.java)
    }
}

// ============================================================================
// 4. GeminiGenerativeModel Class & Provider Instance
// ============================================================================

class GeminiGenerativeModel(
    val modelName: String = "gemini-3.5-flash",
    private val service: GeminiMultimodalRetrofitService = GeminiRetrofitClient.service,
    private val defaultApiKeyProvider: () -> String = {
        try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }
) {
    /**
     * Sends a multimodal request with text prompt and an image bitmap to Gemini API.
     */
    suspend fun generateContentWithImage(
        bitmap: Bitmap,
        prompt: String,
        systemInstructionText: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val resolvedKey = customApiKey?.trim()?.takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
            ?: defaultApiKeyProvider().trim().takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
            ?: ""

        if (resolvedKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY to AI Studio Secrets panel.")
            )
        }

        try {
            val base64Data = bitmap.toGeminiBase64()
            val parts = listOf(
                GeminiPart(text = prompt),
                GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data))
            )

            val systemInstruction = systemInstructionText?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }

            val request = GeminiVisionRequest(
                contents = listOf(GeminiContent(parts = parts, role = "user")),
                systemInstruction = systemInstruction,
                generationConfig = GeminiGenerationConfig(temperature = 0.4f, maxOutputTokens = 1500)
            )

            // Primary call using specified model (gemini-3.5-flash)
            val response = try {
                service.generateContent(
                    model = modelName,
                    apiKey = resolvedKey,
                    request = request
                )
            } catch (e: Exception) {
                // Low-latency fallback to gemini-3.1-flash-lite-preview as mandated by skill
                service.generateContent(
                    model = "gemini-3.1-flash-lite-preview",
                    apiKey = resolvedKey,
                    request = request
                )
            }

            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.text.isNullOrBlank() }
                ?.text

            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response returned from Gemini Multimodal API."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object GeminiGenerativeModelProvider {
    val instance: GeminiGenerativeModel by lazy {
        GeminiGenerativeModel(modelName = "gemini-3.5-flash")
    }
}

// ============================================================================
// 5. GeminiMultimodalRepository to handle image bitmap frames
// ============================================================================

class GeminiMultimodalRepository(
    private val generativeModel: GeminiGenerativeModel = GeminiGenerativeModelProvider.instance
) {
    /**
     * Process a camera bitmap frame with a specific user question about objects seen in the frame.
     */
    suspend fun analyzeFrameQuestion(
        bitmap: Bitmap,
        question: String,
        ownerName: String = "Alom Boss",
        customApiKey: String? = null
    ): Result<String> {
        val systemInstruction = "তুমি 'Maira Bot' — $ownerName-এর রিয়েল-টাইম ক্যামেরা ভিশন চোখ। ক্যামেরার ফ্রেমে দৃশ্যমান সমস্ত বস্তু, মানুষ, লেখা, রং ও পরিবেশ নিখুঁতভাবে বিশ্লেষণ করো এবং ব্যবহারকারীর প্রশ্নের স্পষ্ট ও প্রাঞ্জল বাংলায় উত্তর দাও।"
        val prompt = if (question.isBlank()) {
            "ক্যামেরার এই ফ্রেমে কী কী বস্তু বা ঘটনা দৃশ্যমান তা বিশদভাবে বাংলায় বুঝিয়ে বলো।"
        } else {
            question
        }

        return generativeModel.generateContentWithImage(
            bitmap = bitmap,
            prompt = prompt,
            systemInstructionText = systemInstruction,
            customApiKey = customApiKey
        )
    }

    /**
     * Real-time description of all objects and scenes detected in the camera frame.
     */
    suspend fun describeObjectsInFrame(
        bitmap: Bitmap,
        ownerName: String = "Alom Boss",
        customApiKey: String? = null
    ): Result<String> {
        val prompt = "ক্যামেরার ফ্রেমে বর্তমানে কী কী বস্তু বা পরিবর্তন দৃশ্যমান তা শনাক্ত করো এবং প্রধান উপাদানগুলো পয়েন্ট আকারে সংক্ষেপে বলো।"
        return analyzeFrameQuestion(
            bitmap = bitmap,
            question = prompt,
            ownerName = ownerName,
            customApiKey = customApiKey
        )
    }

    /**
     * Extract and read all OCR text and documents visible in the camera frame.
     */
    suspend fun ocrReadFrameText(
        bitmap: Bitmap,
        customApiKey: String? = null
    ): Result<String> {
        val prompt = "এই ফ্রেমে দৃশ্যমান সমস্ত লেখা, নম্বর বা সাইনবোর্ড হুবহু ওসিআর (OCR) করে বাংলায় উপস্থাপন ও অনুবাদ করো।"
        return generativeModel.generateContentWithImage(
            bitmap = bitmap,
            prompt = prompt,
            systemInstructionText = "তুমি একজন অত্যন্ত নিখুঁত ওসিআর টেক্সট বিশ্লেষক। ফ্রেমে যা লেখা দেখতে পাচ্ছ তা নির্ভুলভাবে তুলে ধরো।",
            customApiKey = customApiKey
        )
    }
}

// ============================================================================
// 6. Bitmap Extension Helper
// ============================================================================

fun Bitmap.toGeminiBase64(maxDimension: Int = 1080, quality: Int = 80): String {
    val scaled = if (width > maxDimension || height > maxDimension) {
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth = if (width > height) maxDimension else (maxDimension * ratio).toInt()
        val targetHeight = if (height > width) maxDimension else (maxDimension / ratio).toInt()
        Bitmap.createScaledBitmap(this, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
    } else {
        this
    }

    val outputStream = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
