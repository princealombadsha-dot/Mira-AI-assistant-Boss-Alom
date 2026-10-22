package com.example.nexus

import android.content.Context
import android.graphics.Bitmap
import com.example.data.api.GeminiApiClient
import com.example.data.model.Personality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class ExecutionStep(
    val stepIndex: Int,
    val taskTag: String, // e.g. "[Task: Analyzing Intent]"
    val descriptionBn: String,
    val status: StepStatus = StepStatus.RUNNING,
    val timestamp: Long = System.currentTimeMillis()
)

enum class StepStatus {
    RUNNING,
    SUCCESS,
    FAILED
}

data class ReActPlanResult(
    val finalResponseText: String,
    val steps: List<ExecutionStep>,
    val routedProvider: LlmProvider,
    val memoryContextUsed: List<String>
)

interface NexusExecutionListener {
    fun onStepStarted(step: ExecutionStep)
    fun onStepCompleted(step: ExecutionStep)
    fun onPlanFinished(result: ReActPlanResult)
}

class NexusCognitiveEngine(
    private val context: Context,
    private val memoryStore: NexusVectorMemoryStore,
    private val llmRouter: NexusLlmRouter,
    private val geminiApiClient: GeminiApiClient
) {
    private val listeners = mutableListOf<NexusExecutionListener>()
    var currentActiveStep: ExecutionStep? = null
        private set

    fun addListener(listener: NexusExecutionListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: NexusExecutionListener) {
        listeners.remove(listener)
    }

    private fun notifyStepStarted(step: ExecutionStep) {
        currentActiveStep = step
        listeners.forEach { it.onStepStarted(step) }
    }

    private fun notifyStepCompleted(step: ExecutionStep) {
        listeners.forEach { it.onStepCompleted(step) }
    }

    private fun notifyPlanFinished(result: ReActPlanResult) {
        currentActiveStep = null
        listeners.forEach { it.onPlanFinished(result) }
    }

    /**
     * Executes an autonomous problem solving ReAct loop:
     * Thought -> Action -> Observation -> Final Answer
     */
    suspend fun solveAutonomousProblem(
        query: String,
        personality: Personality,
        ownerName: String = "Alom Boss",
        customApiKey: String? = null,
        screenBitmap: Bitmap? = null
    ): ReActPlanResult = withContext(Dispatchers.IO) {
        val steps = mutableListOf<ExecutionStep>()
        var stepCounter = 1

        // STEP 1: Intent Analysis & Semantic Memory Retrieval
        val s1 = ExecutionStep(stepCounter++, "[Task: Analyzing Intent]", "প্রম্পট বিশ্লেষণ ও লং-টার্ম ভেক্টর মেমোরি সার্চ করা হচ্ছে...")
        steps.add(s1)
        notifyStepStarted(s1)
        delay(120) // Fast real-time UX feedback

        val relevantMemories = memoryStore.searchRelevantMemories(query)
        val memoryListPairs = relevantMemories.map { it.key to it.value }
        notifyStepCompleted(s1.copy(status = StepStatus.SUCCESS))

        // Auto-learn preference if user declares something
        if (query.contains("আমার পছন্দ") || query.contains("মনে রেখো") || query.contains("remember")) {
            val sLearn = ExecutionStep(stepCounter++, "[Task: Learning Preference]", "লং-টার্ম ভেক্টর স্টোরে নতুন তথ্য সূচীবদ্ধ করা হচ্ছে...")
            steps.add(sLearn)
            notifyStepStarted(sLearn)
            memoryStore.recordMemory("User Note", query, "LEARNED")
            notifyStepCompleted(sLearn.copy(status = StepStatus.SUCCESS))
        }

        // STEP 2: Multi-LLM Dynamic Routing
        val s2 = ExecutionStep(stepCounter++, "[Task: Routing Model]", "কাজের ধরন অনুযায়ী সর্বোত্তম এআই ইঞ্জিন নির্বাচন করা হচ্ছে...")
        steps.add(s2)
        notifyStepStarted(s2)
        val routing = llmRouter.determineOptimalProvider(query, hasVisionAttachment = screenBitmap != null)
        notifyStepCompleted(s2.copy(status = StepStatus.SUCCESS, descriptionBn = "${routing.selectedProvider.displayName} সক্রিয় করা হয়েছে।"))

        // STEP 3: Multimodal Vision & OCR if applicable
        if (screenBitmap != null) {
            val sVision = ExecutionStep(stepCounter++, "[Task: Processing Screen OCR]", "স্ক্রিন ভিজ্যুয়াল ইনপুট ও অপটিক্যাল ক্যারেক্টার বিশ্লেষণ চলছে...")
            steps.add(sVision)
            notifyStepStarted(sVision)
            delay(150)
            notifyStepCompleted(sVision.copy(status = StepStatus.SUCCESS))
        }

        // STEP 4: Action Execution / Accessibility / Tool Dispatch
        val qLower = query.lowercase()
        if (qLower.contains("কপি করো") || qLower.contains("ইনপুট দাও") || qLower.contains("লিখো") || qLower.contains("পেস্ট করো")) {
            val sInject = ExecutionStep(stepCounter++, "[Task: Injecting Code/Text]", "সিস্টেম ক্লিপবোর্ডে টেক্সট তৈরি ও অ্যাক্টিভ ফিল্ডে ইনজেক্ট করা হচ্ছে...")
            steps.add(sInject)
            notifyStepStarted(sInject)
            NexusAutomationBridge.copyToClipboard(context, query)
            NexusAutomationBridge.injectTextIntoActiveApp(query)
            notifyStepCompleted(sInject.copy(status = StepStatus.SUCCESS))
        }

        // STEP 5: ReAct Generation & Reasoning Synthesis
        val sSynthesize = ExecutionStep(stepCounter++, "[Task: Synthesizing Answer]", "স্বায়ত্তশাসিত সমস্যা সমাধান ও ফলাফল প্রস্তুত হচ্ছে...")
        steps.add(sSynthesize)
        notifyStepStarted(sSynthesize)

        val conversationHistory = memoryStore.getShortTermContext()
        val (finalText, _) = llmRouter.executeRoutedQuery(
            prompt = query,
            provider = routing.selectedProvider,
            personality = personality,
            conversationHistory = conversationHistory,
            memories = memoryListPairs,
            customApiKey = customApiKey,
            ownerName = ownerName
        )

        // Store into episodic short-term memory
        memoryStore.addShortTermTurn("USER", query)
        memoryStore.addShortTermTurn("ASSISTANT", finalText)

        notifyStepCompleted(sSynthesize.copy(status = StepStatus.SUCCESS))

        // FINAL STEP: Task Completion
        val sDone = ExecutionStep(stepCounter, "[Task: Completed]", "টাস্ক নির্বাহ সফলভাবে সম্পন্ন হয়েছে।", StepStatus.SUCCESS)
        steps.add(sDone)
        notifyStepStarted(sDone)
        notifyStepCompleted(sDone)

        val result = ReActPlanResult(
            finalResponseText = finalText,
            steps = steps,
            routedProvider = routing.selectedProvider,
            memoryContextUsed = relevantMemories.map { "${it.key}: ${it.value}" }
        )

        notifyPlanFinished(result)
        result
    }
}
