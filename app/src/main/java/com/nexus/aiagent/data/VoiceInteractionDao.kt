package com.nexus.aiagent.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for querying and persisting voice interaction history.
 */
@Dao
interface VoiceInteractionDao {

    @Query("SELECT * FROM voice_interactions ORDER BY timestamp DESC")
    fun getAllInteractions(): Flow<List<VoiceInteractionEntity>>

    @Query("SELECT * FROM voice_interactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentInteractions(limit: Int): List<VoiceInteractionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: VoiceInteractionEntity): Long

    @Query("DELETE FROM voice_interactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_interactions")
    suspend fun clearAll()
}
