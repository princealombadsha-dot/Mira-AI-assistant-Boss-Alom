package com.nexus.aiagent.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Google AI Studio Gemini API client for BD-NEXUS AI Agent.
 * Handles autonomous ReAct reasoning, bilingual Bengali/English prompt synthesis,
 * and command parsing with full system control capabilities.
 */
class GeminiApiClient(private val apiKey: String = "") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun resolveKey(): String {
        if (apiKey.isNotBlank()) return apiKey
        return try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendCommandToGemini(prompt: String): String = withContext(Dispatchers.IO) {
        val activeKey = resolveKey()
        if (activeKey.isBlank()) {
            return@withContext "ত্রুটি: Gemini API কী কনফিগার করা হয়নি। অনুগ্রহ করে Settings প্যানেলে API কী প্রদান করুন।"
        }

        // Using standard gemini-2.5-flash or gemini-2.5-pro for high-speed mobile execution
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$activeKey"

        val systemInstruction = """
            You are BD-NEXUS AI, an elite autonomous system controller, expert developer, and cybersecurity analyst with full Android OS control capabilities.
            You have autonomous access to the device Accessibility Service, screen inspector, app launcher, and system clipboard.
            You natively understand and speak both Bengali (বাংলা) and English.
            Always respond helpfully, authoritatively, and concisely.
            When a user asks to perform an action (like open YouTube, click a button, go home, or copy code), specify the clear steps and state the action being executed.
        """.trimIndent()

        try {
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\n\nUser Command: $prompt"))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                // Optional generation config
                val genConfig = JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 1500)
                }
                put("generationConfig", genConfig)
            }

            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("GeminiApiClient", "API error: ${response.code} $responseBody")
                    return@withContext "ত্রুটি: এআই সার্ভার সাড়া দেয়নি (কোড: ${response.code})।"
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "সঠিক উত্তর পাওয়া যায়নি।")
                    }
                }
                return@withContext "উত্তর বিশ্লেষণ করা সম্ভব হয়নি।"
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Network or parsing exception", e)
            return@withContext "ত্রুটি: ${e.localizedMessage ?: "সংযোগ বিচ্ছিন্ন"}"
        }
    }
}
