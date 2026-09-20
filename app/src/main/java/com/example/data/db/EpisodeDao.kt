package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes ORDER BY title ASC")
    fun getAllEpisodesFlow(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes ORDER BY title ASC")
    suspend fun getAllEpisodesSync(): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    suspend fun getEpisodeById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    fun getEpisodeByIdFlow(id: String): Flow<EpisodeEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Query("UPDATE episodes SET lastPlaybackPositionMs = :positionMs WHERE id = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long)

    @Query("UPDATE episodes SET listenCount = listenCount + 1, lastListenedTimestamp = :timestamp WHERE id = :episodeId")
    suspend fun incrementListenCount(episodeId: String, timestamp: Long)

    @Query("UPDATE episodes SET durationMs = :durationMs WHERE id = :episodeId AND durationMs <= 0")
    suspend fun updateDurationIfZero(episodeId: String, durationMs: Long)

    @Query("UPDATE episodes SET isExcluded = :isExcluded WHERE id = :episodeId")
    suspend fun updateEpisodeExcluded(episodeId: String, isExcluded: Boolean)

    @Query("SELECT * FROM episodes WHERE isExcluded = 0 ORDER BY title ASC")
    fun getAvailableEpisodesFlow(): Flow<List<EpisodeEntity>>

    @Query("DELETE FROM episodes WHERE id NOT IN (:validIds)")
    suspend fun deleteEpisodesNotIn(validIds: List<String>)

    @Query("DELETE FROM episodes")
    suspend fun clearAll()

    @Query("UPDATE episodes SET listenCount = 0, lastPlaybackPositionMs = 0, lastListenedTimestamp = 0")
    suspend fun resetAllListenCounts()
}
