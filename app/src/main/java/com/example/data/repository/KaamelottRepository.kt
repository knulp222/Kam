package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.EpisodeEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.ZoneEntity
import com.example.domain.AudioScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class KaamelottRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kaamelott_prefs", Context.MODE_PRIVATE)

    private val _selectedFolderUri = MutableStateFlow<String?>(
        prefs.getString(KEY_FOLDER_URI, null)
    )
    val selectedFolderUri = _selectedFolderUri.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    val allEpisodes: Flow<List<EpisodeEntity>> = database.episodeDao().getAllEpisodesFlow()
    val allZones: Flow<List<ZoneEntity>> = database.zoneDao().getAllZonesFlow()
    val recentSessions: Flow<List<ListeningSessionEntity>> = database.sessionDao().getRecentSessionsFlow(6)

    suspend fun saveSelectedFolderUri(uriString: String) {
        prefs.edit().putString(KEY_FOLDER_URI, uriString).apply()
        _selectedFolderUri.value = uriString
    }

    suspend fun clearFolder() {
        prefs.edit().remove(KEY_FOLDER_URI).apply()
        _selectedFolderUri.value = null
    }

    suspend fun scanAndSyncFolder(uriString: String): Int = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val uri = Uri.parse(uriString)
            val (scannedEpisodes, scannedZones) = AudioScanner.scanFolder(context, uri)

            if (scannedEpisodes.isNotEmpty()) {
                val existingEpisodes = database.episodeDao().getAllEpisodesSync().associateBy { it.id }
                val existingZones = database.zoneDao().getAllZonesSync().associateBy { it.id }

                // Merge with existing stats so user's listen count, last position and exclusion are preserved
                val mergedEpisodes = scannedEpisodes.map { scanned ->
                    val existing = existingEpisodes[scanned.id]
                    if (existing != null) {
                        scanned.copy(
                            listenCount = existing.listenCount,
                            lastPlaybackPositionMs = existing.lastPlaybackPositionMs,
                            lastListenedTimestamp = existing.lastListenedTimestamp,
                            isExcluded = existing.isExcluded,
                            durationMs = if (scanned.durationMs > 0) scanned.durationMs else existing.durationMs
                        )
                    } else {
                        scanned
                    }
                }

                val mergedZones = scannedZones.map { scannedZone ->
                    val existing = existingZones[scannedZone.id]
                    if (existing != null) {
                        scannedZone.copy(
                            listenCount = existing.listenCount,
                            isAvoided = existing.isAvoided,
                            lastListenedTimestamp = existing.lastListenedTimestamp
                        )
                    } else {
                        scannedZone
                    }
                }

                database.episodeDao().insertAll(mergedEpisodes)
                database.zoneDao().insertZones(mergedZones)

                // Clean up removed files
                val validIds = mergedEpisodes.map { it.id }
                database.episodeDao().deleteEpisodesNotIn(validIds)
                database.zoneDao().deleteZonesForMissingEpisodes(validIds)
            }

            scannedEpisodes.size
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun getEpisode(id: String): EpisodeEntity? = withContext(Dispatchers.IO) {
        database.episodeDao().getEpisodeById(id)
    }

    suspend fun getZonesForEpisode(episodeId: String): List<ZoneEntity> = withContext(Dispatchers.IO) {
        database.zoneDao().getZonesForEpisodeSync(episodeId)
    }

    suspend fun updatePlaybackPosition(episodeId: String, positionMs: Long) = withContext(Dispatchers.IO) {
        database.episodeDao().updatePlaybackPosition(episodeId, positionMs)
    }

    suspend fun updateDurationIfZero(episodeId: String, durationMs: Long) = withContext(Dispatchers.IO) {
        if (durationMs > 0) {
            database.episodeDao().updateDurationIfZero(episodeId, durationMs)
        }
    }

    suspend fun recordSessionAndListen(
        episodeId: String,
        episodeTitle: String,
        zoneIndex: Int,
        startPosMs: Long,
        endPosMs: Long,
        listenedMs: Long
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (listenedMs > 10_000L) { // Only count if listened for at least 10 seconds
            database.episodeDao().incrementListenCount(episodeId, now)
            database.zoneDao().incrementZoneListen(episodeId, zoneIndex, now)

            val session = ListeningSessionEntity(
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                zoneIndex = zoneIndex,
                startPositionMs = startPosMs,
                endPositionMs = endPosMs,
                durationListenedMs = listenedMs,
                timestamp = now
            )
            database.sessionDao().insertSession(session)
            database.sessionDao().pruneOldSessions(20)
        }
        database.episodeDao().updatePlaybackPosition(episodeId, endPosMs)
    }

    private val _appMode = MutableStateFlow(
        prefs.getString(KEY_APP_MODE, "standard") ?: "standard"
    )
    val appMode = _appMode.asStateFlow()

    private val _voiceSofteningEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_VOICE_SOFTENING, true)
    )
    val voiceSofteningEnabled = _voiceSofteningEnabled.asStateFlow()

    suspend fun setAppMode(mode: String) {
        prefs.edit().putString(KEY_APP_MODE, mode).apply()
        _appMode.value = mode
    }

    suspend fun setVoiceSofteningEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_SOFTENING, enabled).apply()
        _voiceSofteningEnabled.value = enabled
    }

    suspend fun resetAllListeningStats() = withContext(Dispatchers.IO) {
        database.episodeDao().resetAllListenCounts()
        database.zoneDao().resetAllZoneCounts()
        database.sessionDao().clearAllSessions()
    }

    suspend fun toggleEpisodeExcluded(episodeId: String, isExcluded: Boolean) = withContext(Dispatchers.IO) {
        database.episodeDao().updateEpisodeExcluded(episodeId, isExcluded)
    }

    suspend fun toggleZoneAvoid(episodeId: String, zoneIndex: Int, isAvoided: Boolean) = withContext(Dispatchers.IO) {
        database.zoneDao().setZoneAvoided(episodeId, zoneIndex, isAvoided)
    }

    companion object {
        private const val KEY_FOLDER_URI = "saved_folder_uri"
        private const val KEY_APP_MODE = "app_ui_mode"
        private const val KEY_VOICE_SOFTENING = "voice_softening_eq_enabled"
    }
}
