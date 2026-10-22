package com.example.nexus

import android.content.Context
import com.example.data.model.UserMemoryItem
import com.example.data.preferences.AssistantPreferences
import java.util.Locale
import java.util.UUID
import kotlin.math.sqrt

data class SemanticMemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val category: String = "PREFERENCE",
    val timestamp: Long = System.currentTimeMillis(),
    val vector: Map<String, Double> = emptyMap()
)

class NexusVectorMemoryStore(private val preferences: AssistantPreferences) {

    private val episodicShortTermBuffer = mutableListOf<Pair<String, String>>()
    private val longTermStore = mutableListOf<SemanticMemoryEntry>()

    init {
        loadPersistedMemories()
    }

    @Synchronized
    fun addShortTermTurn(sender: String, message: String) {
        episodicShortTermBuffer.add(Pair(sender, message))
        if (episodicShortTermBuffer.size > 20) {
            episodicShortTermBuffer.removeAt(0)
        }
    }

    @Synchronized
    fun getShortTermContext(limit: Int = 8): List<Pair<String, String>> {
        return episodicShortTermBuffer.takeLast(limit)
    }

    @Synchronized
    fun recordMemory(key: String, value: String, category: String = "PREFERENCE") {
        val vector = computeVector("$key $value")
        val entry = SemanticMemoryEntry(
            key = key.trim(),
            value = value.trim(),
            category = category,
            vector = vector
        )
        longTermStore.removeAll { it.key.equals(key.trim(), ignoreCase = true) }
        longTermStore.add(entry)
        persist()
    }

    @Synchronized
    fun searchRelevantMemories(query: String, topK: Int = 4, threshold: Double = 0.15): List<SemanticMemoryEntry> {
        if (longTermStore.isEmpty()) return emptyList()
        val queryVector = computeVector(query)

        return longTermStore
            .map { entry -> entry to cosineSimilarity(queryVector, entry.vector) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    @Synchronized
    fun getAllMemories(): List<SemanticMemoryEntry> {
        return longTermStore.toList()
    }

    @Synchronized
    fun deleteMemory(id: String) {
        longTermStore.removeAll { it.id == id }
        persist()
    }

    @Synchronized
    fun clearShortTerm() {
        episodicShortTermBuffer.clear()
    }

    private fun persist() {
        val userItems = longTermStore.map {
            UserMemoryItem(
                id = it.id,
                key = it.key,
                value = it.value,
                timestamp = it.timestamp
            )
        }
        preferences.saveMemories(userItems)
    }

    private fun loadPersistedMemories() {
        val saved = preferences.getMemories()
        longTermStore.clear()
        if (saved.isEmpty()) {
            // Seed defaults for Alom Boss VIP Persona
            recordMemory("Owner Name", "Alom Boss", "IDENTITY")
            recordMemory("Assistant Name", "BD-NEXUS AI", "IDENTITY")
            recordMemory("Primary Language", "Bengali (বাংলা) + English", "LANGUAGE")
            recordMemory("Coding Preferences", "Kotlin, Python, JavaScript, Shell", "DEVELOPER")
            recordMemory("Security Level", "VIP High Privacy Protection", "SECURITY")
        } else {
            saved.forEach {
                longTermStore.add(
                    SemanticMemoryEntry(
                        id = it.id,
                        key = it.key,
                        value = it.value,
                        vector = computeVector("${it.key} ${it.value}")
                    )
                )
            }
        }
    }

    /**
     * Compute term-frequency embedding vector with char 3-grams for robust cross-lingual
     * (Bengali + English) semantic similarity matching.
     */
    private fun computeVector(text: String): Map<String, Double> {
        val clean = text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}\\s]"), " ")
        val tokens = clean.split(Regex("\\s+")).filter { it.length >= 2 }
        val counts = mutableMapOf<String, Double>()

        for (token in tokens) {
            counts[token] = (counts[token] ?: 0.0) + 1.5
            // Add character 3-grams for morphological/subword matching in Bengali & English
            if (token.length >= 3) {
                for (i in 0..token.length - 3) {
                    val gram = token.substring(i, i + 3)
                    counts[gram] = (counts[gram] ?: 0.0) + 0.5
                }
            }
        }

        // Normalize vector to unit length
        val norm = sqrt(counts.values.sumOf { it * it })
        if (norm == 0.0) return emptyMap()
        return counts.mapValues { it.value / norm }
    }

    private fun cosineSimilarity(v1: Map<String, Double>, v2: Map<String, Double>): Double {
        if (v1.isEmpty() || v2.isEmpty()) return 0.0
        var dotProduct = 0.0
        for ((term, weight1) in v1) {
            val weight2 = v2[term] ?: continue
            dotProduct += weight1 * weight2
        }
        return dotProduct.coerceIn(0.0, 1.0)
    }
}
