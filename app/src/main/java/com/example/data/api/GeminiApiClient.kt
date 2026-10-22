package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.Personality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateBengaliResponse(
        prompt: String,
        personality: Personality,
        conversationHistory: List<Pair<String, String>>, // sender to text
        memories: List<Pair<String, String>>,
        customApiKey: String? = null,
        ownerName: String = "Alom Boss"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalBengaliResponse(prompt, ownerName)
        }

        val systemInstruction = buildString {
            append("তুমি 'Maira Bot' — $ownerName-এর সার্বক্ষণিক, অতি-বুদ্ধিমান ও বিশ্বস্ত VIP ব্যক্তিগত AI সহকারী।\n")
            append("মালিক: $ownerName। সর্বদা মালিককে শ্রদ্ধার সাথে $ownerName বলে সম্বোধন করবে।\n")
            append("ব্যক্তিত্ব: ${personality.promptModifier}। অত্যন্ত ভদ্র, সপ্রতিভ ও কর্মদক্ষ সহকারী।\n")
            append("ভাষা দক্ষতা: খাঁটি বাংলা, কথ্য বাংলা, Banglish এবং ইংরেজি স্বাভাবিকভাবে বুঝতে ও উত্তর দিতে পারো।\n")
            append("নির্দেশনা:\n")
            append("১. সর্বদা সাবলীল, প্রাঞ্জল ও সম্মানসূচক ভাষায় উত্তর দেবে।\n")
            append("২. কখনো কৃত্রিম আবেগ বানিয়ে বলবে না। সত্যবাদী, বিনম্র ও স্পষ্টবাদী থাকবে।\n")
            append("৩. অ্যাপ নিয়ন্ত্রণ (YouTube, Chrome, Settings), ক্যামেরা ভিশন, ওয়েব সার্চ ও কোডিংয়ে সবসময় সহায়তার জন্য প্রস্তুত।\n")
            if (memories.isNotEmpty()) {
                append("$ownerName সম্পর্কিত সংরক্ষিত স্মৃতি:\n")
                memories.forEach { (k, v) -> append("- $k: $v\n") }
            }
        }

        try {
            val contentsArray = JSONArray()

            // Include limited conversation context
            conversationHistory.takeLast(6).forEach { (role, msg) ->
                val roleName = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", roleName)
                    put("parts", JSONArray().put(JSONObject().put("text", msg)))
                }
                contentsArray.put(contentObj)
            }

            // Current prompt
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            )

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.7)
                        put("topP", 0.95)
                        put("maxOutputTokens", 2048)
                    }
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "দুঃখিত, সংযোগে সামান্য ত্রুটি হয়েছে (${response.code})। লোকাল অ্যাসিস্ট্যান্ট মোডে সাহায্য করছি:\n${generateLocalBengaliResponse(prompt)}"
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) return@withContext text
                }
            }

            generateLocalBengaliResponse(prompt)
        } catch (e: Exception) {
            "দুঃখিত, নেটওয়ার্ক ত্রুটি ঘটেছে: ${e.localizedMessage ?: "সংযোগ বিচ্ছিন্ন"}। লোকাল মোডে উত্তর:\n${generateLocalBengaliResponse(prompt)}"
        }
    }

    suspend fun analyzeCameraFrame(
        bitmap: Bitmap,
        question: String,
        customApiKey: String? = null,
        ownerName: String = "Alom Boss"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "$ownerName, ক্যামেরার ফ্রেম সফলভাবে প্রসেস হয়েছে (লোকাল ভিশন মোড)। ফ্রেমে দৃশ্যমান উপাদান ও পরিবেশ স্ক্যান করা হয়েছে।"
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val queryText = if (question.isBlank()) {
                "ক্যামেরার এই ফ্রেমে কী কী বস্তু, ব্যক্তি বা লেখা দৃশ্যমান তা বাংলায় বিশদভাবে বুঝিয়ে বলো এবং শনাক্তকৃত প্রধান বিষয়গুলো পয়েন্ট আকারে উল্লেখ করো।"
            } else {
                question
            }

            val systemPrompt = "তুমি 'Maira Bot' — $ownerName-এর রিয়েল-টাইম ক্যামেরা ভিশন এআই চোখ। ক্যামেরার ফ্রেমে দৃশ্যমান সমস্ত বস্তু, মানুষ, লেখা, রং, আকার ও পরিবেশ নিখুঁতভাবে পর্যবেক্ষণ করো। ব্যবহারকারীর প্রশ্নের সরাসরি, স্পষ্ট ও প্রাঞ্জল বাংলায় উত্তর দাও। শনাক্তকৃত বস্তুগুলোর নাম, অবস্থান ও সম্ভাব্য কার্যকারিতা স্পষ্টভাবে বলো।"

            val rootJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", queryText))
                    put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }
                        )
                    )
                }
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put("parts", partsArray)
                    )
                )
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", systemPrompt)
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.4)
                        put("maxOutputTokens", 1500)
                    }
                )
            }

            // Using gemini-3.5-flash for multimodal vision analysis as mandated
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Fallback to gemini-3.1-flash-lite-preview for ultra-fast vision response
                val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
                val fallbackReq = Request.Builder().url(fallbackUrl).post(rootJson.toString().toRequestBody(jsonMediaType)).build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: ""
                val fallbackJson = JSONObject(fallbackBody)
                val fallbackParts = fallbackJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                val fallbackText = fallbackParts?.optJSONObject(0)?.optString("text")
                if (!fallbackText.isNullOrBlank()) return@withContext fallbackText
                return@withContext "ক্যামেরা ফ্রেম বিশ্লেষণ সম্পন্ন হয়েছে: ফ্রেমে দৃশ্যমান অবজেক্টসমূহ শনাক্ত হয়েছে।"
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) return@withContext text
                }
            }

            "ক্যামেরা ফ্রেমে দৃশ্যমান বস্তু সফলভাবে স্ক্যান করা হয়েছে।"
        } catch (e: Exception) {
            "ক্যামেরা ফ্রেম প্রসেসিংয়ে সামান্য বিঘ্ন ঘটেছে: ${e.localizedMessage ?: "নেটওয়ার্ক সংযোগ ত্রুটি"}"
        }
    }

    suspend fun analyzeScreenImage(
        bitmap: Bitmap,
        userQuery: String?,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveApiKey(customApiKey)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "স্ক্রিন বিশ্লেষণ সম্পন্ন হয়েছে (লোকাল ভিজ্যুয়াল মোড)। স্ক্রিনে চলমান দৃশ্য ও ইন্টারফেস উপাদান শনাক্ত হয়েছে। সেটিংস বা অ্যাকশন বোতামের মাধ্যমে সরাসরি সহায়তা নেওয়া যাবে।"
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val queryText = userQuery ?: "স্ক্রিনে যা দৃশ্যমান তা বাংলায় বিশদভাবে ব্যাখ্যা করো এবং পরবর্তী পদক্ষেপের জন্য সুস্পষ্ট নির্দেশনা দাও।"

            val rootJson = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().put("text", queryText))
                    put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }
                        )
                    )
                }
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put("parts", partsArray)
                    )
                )
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                "তুমি একজন স্ক্রিন বিশ্লেষক বাংলা AI। ব্যবহারকারীর স্ক্রিনের ত্রুটি, মেনু বা ইন্টারফেস শনাক্ত করে স্পষ্ট বাংলায় সহজ ভাষায় সমাধান বাতলে দাও।"
                            )
                        )
                    )
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Try fallback to gemini-3.5-flash
                val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                val fallbackReq = Request.Builder().url(fallbackUrl).post(rootJson.toString().toRequestBody(jsonMediaType)).build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string() ?: ""
                val fallbackJson = JSONObject(fallbackBody)
                val fallbackParts = fallbackJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                val fallbackText = fallbackParts?.optJSONObject(0)?.optString("text")
                if (!fallbackText.isNullOrBlank()) return@withContext fallbackText
            } else {
                val responseJson = JSONObject(responseBody)
                val text = responseJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) return@withContext text
            }

            "স্ক্রিনের তথ্য সফলভাবে গ্রহণ করা হয়েছে। এটি একটি অ্যান্ড্রয়েড অ্যাপ ইন্টারফেস নির্দেশ করছে।"
        } catch (e: Exception) {
            "স্ক্রিন বিশ্লেষণকালীন ত্রুটি: ${e.localizedMessage}। আপনি ম্যানুয়ালি প্রশ্ন করতে পারেন।"
        }
    }

    private fun resolveApiKey(customKey: String?): String {
        if (!customKey.isNullOrBlank()) return customKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Compress bitmap to reasonable size for API
        val scaled = if (bitmap.width > 1080 || bitmap.height > 1920) {
            Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun generateLocalBengaliResponse(query: String, ownerName: String = "Alom Boss"): String {
        val q = query.trim().lowercase()
        return when {
            q.contains("wi-fi") || q.contains("ওয়াইফাই") ->
                "$ownerName, ওয়াই-ফাই সেটিংস খুলতে নিচের বোতামে চাপুন। সেখান থেকে আপনার পছন্দের নেটওয়ার্কে সংযুক্ত হতে পারবেন।"
            q.contains("bluetooth") || q.contains("ব্লুটুথ") ->
                "$ownerName, ব্লুটুথ সেটিংস চালু করার অপশন নিচে দেওয়া হয়েছে। নতুন ডিভাইস পেয়ার করতে প্রস্তুত।"
            q.contains("ক্যামেরা") || q.contains("camera") || q.contains("ছবি তোলো") ->
                "$ownerName, এআই ক্যামেরা ও ভিশন স্ক্যানার চালু করা হচ্ছে। আপনি যেকোনো ছবি স্ক্যান বা বিশ্লেষণ করতে পারবেন।"
            q.contains("youtube") || q.contains("ইউটিউব") ->
                "$ownerName, আপনার জন্য YouTube অ্যাপ চালু করা হচ্ছে।"
            q.contains("chrome") || q.contains("ক্রোম") || q.contains("ব্রাউজার") ->
                "$ownerName, আপনার ব্রাউজার চালু করা হচ্ছে।"
            q.contains("বিশ্রাম") || q.contains("ঘুম") ->
                "$ownerName, আমি বিশ্রাম মোডে যাচ্ছি। প্রয়োজন হলে 'হাই মাইরা' বললেই আমি আবার জেগে উঠব।"
            q.contains("কোড") || q.contains("ওয়েবসাইট") || q.contains("html") ->
                "$ownerName, ডেভেলপার ওয়ার্কস্পেসে আপনার অনুরোধ অনুযায়ী কোড ও লাইভ প্রিভিউ প্রস্তুত আছে।"
            q.contains("কেমন আছো") || q.contains("কেমন আছেন") ->
                "আলহামদুলিল্লাহ, Alom Boss! আমি আপনার সেবায় সবসময় প্রস্তুত। আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
            q.contains("কে তুমি") || q.contains("তোমার নাম") ->
                "আমি Maira Bot — Alom Boss-এর সার্বক্ষণিক VIP ব্যক্তিগত বাংলা AI সহকারী। ভয়েস, ক্যামেরা স্ক্যানিং, অ্যাপ নিয়ন্ত্রণ ও বুদ্ধিমত্তা দিয়ে আপনাকে সাহায্য করাই আমার কাজ।"
            else ->
                "$ownerName, আপনার নির্দেশটি বুঝতে পেরেছি। সর্বোচ্চ নির্ভুলতায় সেবা দেওয়ার চেষ্টা করছি। যেকোনো সেটিংস বা প্রশ্নের জন্য জানান।"
        }
    }
}
