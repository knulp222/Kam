package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KaamelottApp
import com.example.data.model.EpisodeEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.ZoneEntity
import com.example.domain.SmartEpisodeSelector
import com.example.domain.SmartPickResult
import com.example.service.AudioPlaybackService
import com.example.service.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KaamelottApp).repository

    val selectedFolderUri: StateFlow<String?> = repository.selectedFolderUri
    val isScanning: StateFlow<Boolean> = repository.isScanning

    val allEpisodes: StateFlow<List<EpisodeEntity>> = repository.allEpisodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allZones: StateFlow<List<ZoneEntity>> = repository.allZones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<ListeningSessionEntity>> = repository.recentSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appMode: StateFlow<String> = repository.appMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "standard")

    val voiceSofteningEnabled: StateFlow<Boolean> = repository.voiceSofteningEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _selectedTimerMinutes = MutableStateFlow(30)
    val selectedTimerMinutes = _selectedTimerMinutes.asStateFlow()

    private val _isNightModeActive = MutableStateFlow(false)
    val isNightModeActive = _isNightModeActive.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage = _userFeedbackMessage.asStateFlow()

    private var playbackService: AudioPlaybackService? = null
    private val _isServiceBound = MutableStateFlow(false)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Dynamically calculate the next Smart Pick recommendation
    val smartPickPreview: StateFlow<SmartPickResult?> = combine(
        allEpisodes,
        allZones,
        recentSessions
    ) { episodes, zones, sessions ->
        SmartEpisodeSelector.pickSmartTarget(episodes, zones, sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as AudioPlaybackService.LocalBinder
            val service = localBinder.getService()
            playbackService = service
            _isServiceBound.value = true

            viewModelScope.launch {
                service.playbackState.collect { state ->
                    _playbackState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindPlaybackService()
    }

    private fun bindPlaybackService() {
        val intent = Intent(getApplication(), AudioPlaybackService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}

            val uriString = uri.toString()
            repository.saveSelectedFolderUri(uriString)
            val count = repository.scanAndSyncFolder(uriString)
            _userFeedbackMessage.value = "$count épisodes Kaamelott chargés"
        }
    }

    fun rescanFolder() {
        val uriStr = selectedFolderUri.value ?: return
        viewModelScope.launch {
            val count = repository.scanAndSyncFolder(uriStr)
            _userFeedbackMessage.value = "$count épisodes synchronisés"
        }
    }

    fun setAppMode(mode: String, autoStartRandom: Boolean = true) {
        viewModelScope.launch {
            repository.setAppMode(mode)
            if (mode == "random" && autoStartRandom) {
                launchPureRandomZap()
            }
        }
    }

    fun setVoiceSofteningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVoiceSofteningEnabled(enabled)
        }
    }

    fun resetAllListeningStats() {
        viewModelScope.launch {
            repository.resetAllListeningStats()
            _userFeedbackMessage.value = "Compteurs et historique réinitialisés à zéro"
        }
    }

    fun toggleEpisodeExcluded(episodeId: String, isExcluded: Boolean) {
        viewModelScope.launch {
            repository.toggleEpisodeExcluded(episodeId, isExcluded)
        }
    }

    fun setTimerMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        _selectedTimerMinutes.value = clamped
        playbackService?.setSleepTimerDuration(clamped * 60 * 1000L)
    }

    fun setSleepTimerDirect(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        _selectedTimerMinutes.value = clamped
        playbackService?.setSleepTimerDirect(clamped)
    }

    fun adjustTimerMinutes(delta: Int) {
        val current = if (_playbackState.value.isPlaying && _playbackState.value.sleepTimerRemainingMs > 0) {
            ((_playbackState.value.sleepTimerRemainingMs / (60 * 1000L)).toInt()).coerceAtLeast(1)
        } else {
            _selectedTimerMinutes.value
        }
        val target = (current + delta).coerceIn(1, 180)
        setSleepTimerDirect(target)
    }

    fun launchPureRandomZap() {
        val episodes = allEpisodes.value
        if (episodes.isEmpty()) {
            _userFeedbackMessage.value = "Veuillez d'abord sélectionner un dossier avec vos épisodes."
            return
        }

        val target = SmartEpisodeSelector.pickPureRandomSpot(
            episodes = episodes,
            allZones = allZones.value,
            currentEpisodeId = playbackState.value.currentEpisode?.id
        )

        if (target == null) {
            _userFeedbackMessage.value = "Aucun épisode disponible."
            return
        }

        val timerMs = if (_playbackState.value.sleepTimerRemainingMs > 10_000L) {
            _playbackState.value.sleepTimerRemainingMs
        } else {
            _selectedTimerMinutes.value * 60 * 1000L
        }

        startPlayback(
            episode = target.episode,
            zone = target.targetZone,
            startPositionMs = target.startPositionMs
        )
        _userFeedbackMessage.value = "⚡ Passage aléatoire : ${target.episode.title}"
    }

    fun launchSmartPlay() {
        val episodes = allEpisodes.value
        if (episodes.isEmpty()) {
            _userFeedbackMessage.value = "Veuillez d'abord sélectionner un dossier avec vos épisodes."
            return
        }

        val target = SmartEpisodeSelector.pickSmartTarget(
            episodes = episodes,
            allZones = allZones.value,
            recentSessions = recentSessions.value
        ) ?: return

        startPlayback(
            episode = target.episode,
            zone = target.targetZone,
            startPositionMs = target.startPositionMs
        )
        _isNightModeActive.value = true
    }

    fun playEpisodeDirect(
        episode: EpisodeEntity,
        startFromBeginning: Boolean = false,
        specificZone: ZoneEntity? = null
    ) {
        viewModelScope.launch {
            val zones = repository.getZonesForEpisode(episode.id)
            val zoneToPlay = specificZone ?: if (startFromBeginning) {
                zones.firstOrNull() ?: ZoneEntity(
                    id = "${episode.id}_zone_0",
                    episodeId = episode.id,
                    zoneIndex = 0,
                    startTimeMs = 0L,
                    endTimeMs = 510_000L
                )
            } else {
                if (episode.lastPlaybackPositionMs > 0) {
                    SmartEpisodeSelector.findZoneForPosition(zones, episode.lastPlaybackPositionMs)
                        ?: zones.firstOrNull()
                } else {
                    SmartEpisodeSelector.pickBestZone(zones)
                } ?: ZoneEntity(
                    id = "${episode.id}_zone_0",
                    episodeId = episode.id,
                    zoneIndex = 0,
                    startTimeMs = 0L,
                    endTimeMs = 510_000L
                )
            }

            val startPos = if (startFromBeginning) {
                0L
            } else if (specificZone != null) {
                specificZone.startTimeMs
            } else {
                episode.lastPlaybackPositionMs
            }

            startPlayback(episode, zoneToPlay, startPos)
            _isNightModeActive.value = true
        }
    }

    private fun startPlayback(episode: EpisodeEntity, zone: ZoneEntity, startPositionMs: Long) {
        val app = getApplication<Application>()
        val serviceIntent = Intent(app, AudioPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(serviceIntent)
        } else {
            app.startService(serviceIntent)
        }

        val timerDurationMs = _selectedTimerMinutes.value * 60 * 1000L
        playbackService?.playEpisodeAtZone(
            episode = episode,
            zone = zone,
            startPositionMs = startPositionMs,
            timerDurationMs = timerDurationMs
        )
    }

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
    }

    fun addFiveMinutesToSleepTimer() {
        playbackService?.addSleepTimerMinutes(5)
        _userFeedbackMessage.value = "+5 min ajoutées"
    }

    fun toggleCurrentZoneAvoid() {
        val currentEp = playbackState.value.currentEpisode ?: return
        val currentZ = playbackState.value.currentZone ?: return
        val newAvoid = !currentZ.isAvoided

        viewModelScope.launch {
            repository.toggleZoneAvoid(currentEp.id, currentZ.zoneIndex, newAvoid)
            _userFeedbackMessage.value = if (newAvoid) {
                "Zone #${currentZ.zoneIndex + 1} marquée à éviter"
            } else {
                "Zone #${currentZ.zoneIndex + 1} réactivée"
            }
        }
    }

    fun toggleZoneAvoidForEpisode(episodeId: String, zoneIndex: Int, currentAvoided: Boolean) {
        viewModelScope.launch {
            repository.toggleZoneAvoid(episodeId, zoneIndex, !currentAvoided)
        }
    }

    fun openNightMode() {
        _isNightModeActive.value = true
    }

    fun exitNightMode() {
        _isNightModeActive.value = false
    }

    fun stopAndExitSession() {
        playbackService?.stopPlayback()
        _isNightModeActive.value = false
    }

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_isServiceBound.value) {
            getApplication<Application>().unbindService(serviceConnection)
            _isServiceBound.value = false
        }
    }
}
