package com.example.nexus

import android.content.Context
import com.example.data.api.GeminiApiClient
import com.example.data.preferences.AssistantPreferences

object NexusEngineProvider {

    @Volatile
    private var memoryStoreInstance: NexusVectorMemoryStore? = null

    @Volatile
    private var llmRouterInstance: NexusLlmRouter? = null

    @Volatile
    private var cognitiveEngineInstance: NexusCognitiveEngine? = null

    fun getMemoryStore(context: Context): NexusVectorMemoryStore {
        return memoryStoreInstance ?: synchronized(this) {
            memoryStoreInstance ?: NexusVectorMemoryStore(
                AssistantPreferences(context.applicationContext)
            ).also { memoryStoreInstance = it }
        }
    }

    fun getLlmRouter(geminiApiClient: GeminiApiClient): NexusLlmRouter {
        return llmRouterInstance ?: synchronized(this) {
            llmRouterInstance ?: NexusLlmRouter(geminiApiClient).also { llmRouterInstance = it }
        }
    }

    fun getCognitiveEngine(
        context: Context,
        geminiApiClient: GeminiApiClient
    ): NexusCognitiveEngine {
        return cognitiveEngineInstance ?: synchronized(this) {
            cognitiveEngineInstance ?: NexusCognitiveEngine(
                context = context.applicationContext,
                memoryStore = getMemoryStore(context),
                llmRouter = getLlmRouter(geminiApiClient),
                geminiApiClient = geminiApiClient
            ).also { cognitiveEngineInstance = it }
        }
    }
}
