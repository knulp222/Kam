package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessionsFlow(limit: Int = 6): Flow<List<ListeningSessionEntity>>

    @Query("SELECT * FROM listening_sessions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessionsSync(limit: Int = 6): List<ListeningSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ListeningSessionEntity): Long

    @Query("DELETE FROM listening_sessions WHERE id NOT IN (SELECT id FROM listening_sessions ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldSessions(keepCount: Int = 20)

    @Query("SELECT COUNT(*) FROM listening_sessions")
    fun getTotalSessionsCountFlow(): Flow<Int>

    @Query("DELETE FROM listening_sessions")
    suspend fun clearAllSessions()
}
