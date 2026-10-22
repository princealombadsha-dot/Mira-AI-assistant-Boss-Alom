package com.nexus.aiagent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity to persist the history of voice interactions
 * between the user and the autonomous BD-NEXUS AI assistant for future context reference.
 */
@Entity(tableName = "voice_interactions")
data class VoiceInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spokenPrompt: String,
    val aiResponse: String,
    val language: String = "bn-BD",
    val status: String = "SUCCESS",
    val executedAction: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
