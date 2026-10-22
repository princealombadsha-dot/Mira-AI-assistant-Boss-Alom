package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
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
// 1. Retrofit Request and Response Data Classes
// ============================================================================

@JsonClass(generateAdapter = true)
data class GeminiApiRequest(
    @Json(name = "contents") val contents: List<GeminiApiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiApiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiApiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiContent(
    @Json(name = "parts") val parts: List<GeminiApiPart>,
    @Json(name = "role") val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiApiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiApiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiApiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.4f,
    @Json(name = "topP") val topP: Float? = null,
    @Json(name = "topK") val topK: Int? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiApiResponse(
    @Json(name = "candidates") val candidates: List<GeminiApiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiCandidate(
    @Json(name = "content") val content: GeminiApiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

// ============================================================================
// 2. Retrofit Service Interface
// ============================================================================

interface GeminiRetrofitService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiApiRequest
    ): GeminiApiResponse
}

// ============================================================================
// 3. Retrofit Client Singleton
// ============================================================================

object GeminiRepositoryRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val service: GeminiRetrofitService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRetrofitService::class.java)
    }
}

// ============================================================================
// 4. GeminiRepository Implementation
// ============================================================================

/**
 * Repository implementing both Google Generative AI SDK's GenerativeModel
 * and a direct Retrofit service to handle image bitmap frames and multimodal requests.
 */
class GeminiRepository(
    val modelName: String = "gemini-3.5-flash",
    private val retrofitService: GeminiRetrofitService = GeminiRepositoryRetrofitClient.service
) {
    private fun resolveApiKey(customApiKey: String?): String {
        return customApiKey?.trim()?.takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
            ?: runCatching { BuildConfig.GEMINI_API_KEY.trim() }.getOrDefault("")
                .takeIf { it.isNotEmpty() && it != "MY_GEMINI_API_KEY" }
            ?: ""
    }

    /**
     * Creates an instance of GenerativeModel from google-generative-ai library.
     */
    fun createGenerativeModel(
        apiKey: String,
        systemInstructionText: String? = null
    ): GenerativeModel {
        val config = generationConfig {
            temperature = 0.4f
            maxOutputTokens = 2048
        }
        val sysInstruction = systemInstructionText?.let {
            content { text(it) }
        }

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            systemInstruction = sysInstruction
        )
    }

    /**
     * Primary method to analyze an image bitmap with a prompt using GenerativeModel,
     * with graceful fallback to the Retrofit service if needed.
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String,
        systemInstructionText: String? = "তুমি 'Maira Bot' — অত্যন্ত বুদ্ধিমান এআই ভিশন সহকারী। ফ্রেমে যা দৃশ্যমান তা নিখুঁতভাবে বাংলায় বর্ণনা করো।",
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY in the AI Studio Secrets panel.")
            )
        }

        // Attempt using google-generative-ai SDK GenerativeModel first
        try {
            val sdkResult = analyzeWithGenerativeModel(bitmap, prompt, systemInstructionText, apiKey)
            if (sdkResult.isSuccess) {
                return@withContext sdkResult
            }
        } catch (_: Exception) {
            // Fall through to Retrofit service
        }

        // Fallback to direct Retrofit Service call
        analyzeWithRetrofit(bitmap, prompt, systemInstructionText, apiKey)
    }

    /**
     * Analyzes image using GenerativeModel from the google-generative-ai library.
     */
    suspend fun analyzeWithGenerativeModel(
        bitmap: Bitmap,
        prompt: String,
        systemInstructionText: String? = null,
        apiKey: String = resolveApiKey(null)
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key missing.")
            )
        }

        try {
            val generativeModel = createGenerativeModel(apiKey, systemInstructionText)
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val response = generativeModel.generateContent(inputContent)
            val text = response.text

            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from GenerativeModel"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyzes image using direct Retrofit service.
     */
    suspend fun analyzeWithRetrofit(
        bitmap: Bitmap,
        prompt: String,
        systemInstructionText: String? = null,
        apiKey: String = resolveApiKey(null)
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key missing.")
            )
        }

        try {
            val base64Data = bitmap.toBase64String()
            val parts = listOf(
                GeminiApiPart(text = prompt),
                GeminiApiPart(inlineData = GeminiApiInlineData(mimeType = "image/jpeg", data = base64Data))
            )

            val sysInstruction = systemInstructionText?.let {
                GeminiApiContent(parts = listOf(GeminiApiPart(text = it)))
            }

            val request = GeminiApiRequest(
                contents = listOf(GeminiApiContent(parts = parts)),
                systemInstruction = sysInstruction,
                generationConfig = GeminiApiGenerationConfig(temperature = 0.4f, maxOutputTokens = 2048)
            )

            val response = retrofitService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull { !it.text.isNullOrBlank() }
                ?.text

            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Retrofit Gemini API"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Describes all objects visible in the camera frame.
     */
    suspend fun describeObjects(
        bitmap: Bitmap,
        customApiKey: String? = null
    ): Result<String> {
        val prompt = "ক্যামেরার এই ফ্রেমে দৃশ্যমান সমস্ত প্রধান বস্তু, মানুষ, পরিবেশ এবং কার্যকলাপ বিস্তারিতভাবে বাংলায় পয়েন্ট আকারে বলো।"
        return analyzeImage(bitmap, prompt, customApiKey = customApiKey)
    }

    /**
     * Performs OCR to read text and signage visible in the camera frame.
     */
    suspend fun ocrText(
        bitmap: Bitmap,
        customApiKey: String? = null
    ): Result<String> {
        val prompt = "এই ফ্রেমে দৃশ্যমান সমস্ত টেক্সট, সাইনবোর্ড বা নথি চিহ্নিত করে বাংলায় হুবহু তুলে ধরো।"
        return analyzeImage(
            bitmap = bitmap,
            prompt = prompt,
            systemInstructionText = "তুমি একজন অত্যন্ত নিখুঁত ওসিআর বিশ্লেষক। ফ্রেমে লেখা টেক্সট যথাযথভাবে উদ্ধার করো।",
            customApiKey = customApiKey
        )
    }

    private fun Bitmap.toBase64String(maxDimension: Int = 1080, quality: Int = 80): String {
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
}
