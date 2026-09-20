package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Query("SELECT * FROM zones WHERE episodeId = :episodeId ORDER BY zoneIndex ASC")
    fun getZonesForEpisodeFlow(episodeId: String): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones WHERE episodeId = :episodeId ORDER BY zoneIndex ASC")
    suspend fun getZonesForEpisodeSync(episodeId: String): List<ZoneEntity>

    @Query("SELECT * FROM zones")
    fun getAllZonesFlow(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones")
    suspend fun getAllZonesSync(): List<ZoneEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertZones(zones: List<ZoneEntity>)

    @Query("UPDATE zones SET isAvoided = :isAvoided WHERE episodeId = :episodeId AND zoneIndex = :zoneIndex")
    suspend fun setZoneAvoided(episodeId: String, zoneIndex: Int, isAvoided: Boolean)

    @Query("UPDATE zones SET listenCount = listenCount + 1, lastListenedTimestamp = :timestamp WHERE episodeId = :episodeId AND zoneIndex = :zoneIndex")
    suspend fun incrementZoneListen(episodeId: String, zoneIndex: Int, timestamp: Long)

    @Query("DELETE FROM zones WHERE episodeId NOT IN (:validEpisodeIds)")
    suspend fun deleteZonesForMissingEpisodes(validEpisodeIds: List<String>)

    @Query("UPDATE zones SET listenCount = 0, lastListenedTimestamp = 0")
    suspend fun resetAllZoneCounts()
}
