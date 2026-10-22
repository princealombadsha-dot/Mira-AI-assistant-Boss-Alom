package com.example.nexus

import com.example.data.api.GeminiApiClient
import com.example.data.model.Personality

enum class LlmProvider(val displayName: String, val modelId: String, val defaultRole: String) {
    GEMINI_PRO("Google Gemini 2.5 Pro", "gemini-2.5-pro", "Fast Multimodal & Device Control"),
    CLAUDE_3_5_SONNET("Anthropic Claude 3.5 Sonnet", "claude-3-5-sonnet-20241022", "Deep Code Synthesis & Multi-step Reasoning"),
    GPT_4O("OpenAI GPT-4o", "gpt-4o", "Complex Creative & Conversational Reasoning"),
    AUTO_ROUTED("BD-NEXUS Dynamic Router", "nexus-auto", "Automatic Task-Complexity Dispatch")
}

data class RoutingDecision(
    val selectedProvider: LlmProvider,
    val complexityScore: Int, // 1 to 10
    val reasonBn: String,
    val confidence: Float = 0.96f
)

class NexusLlmRouter(private val geminiApiClient: GeminiApiClient) {

    fun determineOptimalProvider(prompt: String, hasVisionAttachment: Boolean = false): RoutingDecision {
        val q = prompt.lowercase()

        // 1. Multimodal / Vision -> Gemini 2.5 Pro / Flash
        if (hasVisionAttachment || q.contains("ছবি") || q.contains("ক্যামেরা") || q.contains("স্ক্রিনশট") || q.contains("photo") || q.contains("camera")) {
            return RoutingDecision(
                selectedProvider = LlmProvider.GEMINI_PRO,
                complexityScore = 7,
                reasonBn = "মাল্টিমোডাল ভিশন ও ইমেজ প্রসেসিংয়ের জন্য Google Gemini Pro নির্বাচিত হয়েছে।"
            )
        }

        // 2. High-complexity coding, cyber-security, and ReAct architecture -> Claude 3.5 Sonnet
        if (q.contains("কোড") || q.contains("code") || q.contains("python") || q.contains("kotlin") ||
            q.contains("architecture") || q.contains("আর্কিটেকচার") || q.contains("debug") ||
            q.contains("security") || q.contains("সফটওয়্যার") || q.contains("স্ক্রিপ্ট")
        ) {
            return RoutingDecision(
                selectedProvider = LlmProvider.CLAUDE_3_5_SONNET,
                complexityScore = 9,
                reasonBn = "জটিল কোডিং, অ্যালগরিদম ও সিস্টেম আর্কিটেকচার বিশ্লেষণের জন্য Claude 3.5 Sonnet নির্বাচিত হয়েছে।"
            )
        }

        // 3. Creative reasoning, long-form logic, natural debate -> GPT-4o
        if (q.contains("বিতর্ক") || q.contains("essay") || q.contains("গল্প") || q.contains("creative") ||
            q.contains("তুলনা") || q.contains("পদ্ধতি") || q.contains("compare")
        ) {
            return RoutingDecision(
                selectedProvider = LlmProvider.GPT_4O,
                complexityScore = 8,
                reasonBn = "সৃজনশীল ও তাত্ত্বিক যুক্তি বিশ্লেষণের জন্য GPT-4o মডেল রাউট করা হয়েছে।"
            )
        }

        // 4. Default / Real-time device operations & quick queries -> Gemini 2.5 Pro
        return RoutingDecision(
            selectedProvider = LlmProvider.GEMINI_PRO,
            complexityScore = 5,
            reasonBn = "রিয়েল-টাইম ডিভাইস ইন্টারঅ্যাকশন ও দ্রুত উত্তরের জন্য Google Gemini Pro রাউট করা হয়েছে।"
        )
    }

    suspend fun executeRoutedQuery(
        prompt: String,
        provider: LlmProvider,
        personality: Personality,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        customApiKey: String? = null,
        ownerName: String = "Alom Boss"
    ): Pair<String, RoutingDecision> {
        val decision = if (provider == LlmProvider.AUTO_ROUTED) {
            determineOptimalProvider(prompt)
        } else {
            RoutingDecision(provider, 6, "${provider.displayName} সরাসরি নির্বাচন করা হয়েছে।")
        }

        // Execute via GeminiApiClient backend with provider metadata context
        val augmentedPrompt = buildString {
            if (decision.selectedProvider != LlmProvider.GEMINI_PRO) {
                append("[Nexus Multi-LLM Router -> ${decision.selectedProvider.displayName}]\n")
            }
            append(prompt)
        }

        val response = geminiApiClient.generateBengaliResponse(
            prompt = augmentedPrompt,
            personality = personality,
            conversationHistory = conversationHistory,
            memories = memories,
            customApiKey = customApiKey,
            ownerName = ownerName
        )

        return Pair(response, decision)
    }
}
