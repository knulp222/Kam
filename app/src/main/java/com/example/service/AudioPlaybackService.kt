package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.KaamelottApp
import com.example.MainActivity
import com.example.data.model.EpisodeEntity
import com.example.data.model.ZoneEntity
import com.example.domain.SmartEpisodeSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlaybackService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSessionCompat? = null

    private val audioProcessor = NightAudioProcessor()
    private var wasPlayingBeforeTransientLoss = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var sessionStartTimestamp: Long = 0L
    private var sessionStartPositionMs: Long = 0L
    private var currentEpisode: EpisodeEntity? = null
    private var currentZone: ZoneEntity? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initMediaSession()

        // Listen for voice softening / EQ setting changes
        serviceScope.launch {
            val repo = (application as? KaamelottApp)?.repository ?: return@launch
            repo.voiceSofteningEnabled.collectLatest { enabled ->
                val sessionId = mediaPlayer?.audioSessionId ?: 0
                audioProcessor.setEnabled(enabled, sessionId)
            }
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "KaamelottNightMediaSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    togglePlayPause()
                }

                override fun onPause() {
                    togglePlayPause()
                }

                override fun onSkipToNext() {
                    addSleepTimerMinutes(5)
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }
            })

            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_PAUSE -> if (_playbackState.value.isPlaying) togglePlayPause()
            ACTION_PLAY -> if (!_playbackState.value.isPlaying) togglePlayPause()
            ACTION_STOP -> stopPlayback()
            ACTION_ADD_5_MIN -> addSleepTimerMinutes(5)
        }
        return START_NOT_STICKY
    }

    fun playEpisodeAtZone(
        episode: EpisodeEntity,
        zone: ZoneEntity,
        startPositionMs: Long = zone.startTimeMs,
        timerDurationMs: Long = if (_playbackState.value.sleepTimerDurationMs > 0) _playbackState.value.sleepTimerDurationMs else 30 * 60 * 1000L
    ) {
        serviceScope.launch {
            finalizeCurrentSession()

            currentEpisode = episode
            currentZone = zone
            sessionStartTimestamp = System.currentTimeMillis()
            sessionStartPositionMs = startPositionMs
            wasPlayingBeforeTransientLoss = false

            requestAudioFocus()

            try {
                audioProcessor.release()
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    val uri = Uri.parse(episode.uriString)
                    val pfd = contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        setDataSource(pfd.fileDescriptor)
                        pfd.close()
                    } else {
                        setDataSource(applicationContext, uri)
                    }

                    prepare()
                    seekTo(startPositionMs.toInt())

                    // Attach Night Audio Equalizer (Feutré / Réduction trompettes)
                    val repo = (application as? KaamelottApp)?.repository
                    val softeningEnabled = repo?.voiceSofteningEnabled?.value ?: true
                    audioProcessor.attach(audioSessionId, softeningEnabled)

                    val introFactor = audioProcessor.calculateIntroFactor(startPositionMs)
                    setVolume(introFactor, introFactor)

                    setOnCompletionListener {
                        onTrackCompleted()
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e("AudioPlaybackService", "MediaPlayer error: what=$what, extra=$extra")
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            errorMessage = "Erreur de lecture ($what)"
                        )
                        updateMediaSessionPlaybackState(false)
                        updateNotification()
                        true
                    }

                    start()
                }

                val actualDuration = mediaPlayer?.duration?.toLong() ?: episode.durationMs
                if (episode.durationMs <= 0 && actualDuration > 0) {
                    (application as KaamelottApp).repository.updateDurationIfZero(episode.id, actualDuration)
                }

                _playbackState.value = _playbackState.value.copy(
                    currentEpisode = episode,
                    currentZone = zone,
                    isPlaying = true,
                    currentPositionMs = startPositionMs,
                    durationMs = actualDuration,
                    sleepTimerDurationMs = timerDurationMs,
                    sleepTimerRemainingMs = timerDurationMs,
                    isSleepTimerActive = true,
                    currentVolume = 1.0f,
                    errorMessage = null
                )

                updateMediaMetadata(episode, actualDuration)
                updateMediaSessionPlaybackState(true)

                startForeground(NOTIFICATION_ID, buildNotification())
                startProgressTicker()
                startSleepTimer(timerDurationMs)

            } catch (e: Exception) {
                Log.e("AudioPlaybackService", "Error starting playback", e)
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    errorMessage = "Erreur lecture : ${e.localizedMessage ?: "Fichier inaccessible"}"
                )
                updateMediaSessionPlaybackState(false)
            }
        }
    }

    private fun onTrackCompleted() {
        serviceScope.launch {
            finalizeCurrentSession()
            val remainingTimer = _playbackState.value.sleepTimerRemainingMs

            if (remainingTimer > 15_000L) {
                // Sleep timer still has plenty of time: chain to next smart episode!
                val repo = (application as KaamelottApp).repository
                val episodes = repo.allEpisodes.first()
                val zones = repo.allZones.first()
                val sessions = repo.recentSessions.first()

                val nextPick = SmartEpisodeSelector.pickSmartTarget(episodes, zones, sessions)
                if (nextPick != null) {
                    playEpisodeAtZone(
                        episode = nextPick.episode,
                        zone = nextPick.targetZone,
                        startPositionMs = nextPick.startPositionMs,
                        timerDurationMs = remainingTimer
                    )
                    return@launch
                }
            }

            // Otherwise stop gracefully
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                currentPositionMs = 0L
            )
            updateMediaSessionPlaybackState(false)
            updateNotification()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val ep = currentEpisode
        val z = currentZone

        if (player == null) {
            if (ep != null && z != null) {
                val timerMs = if (_playbackState.value.sleepTimerRemainingMs > 0) {
                    _playbackState.value.sleepTimerRemainingMs
                } else {
                    if (_playbackState.value.sleepTimerDurationMs > 0) _playbackState.value.sleepTimerDurationMs else 30 * 60 * 1000L
                }
                playEpisodeAtZone(
                    episode = ep,
                    zone = z,
                    startPositionMs = _playbackState.value.currentPositionMs,
                    timerDurationMs = timerMs
                )
            }
            return
        }

        if (_playbackState.value.isPlaying) {
            // User explicitly paused -> pause without automatic phantom restart
            wasPlayingBeforeTransientLoss = false
            try {
                player.pause()
            } catch (_: Exception) {}
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            updateMediaSessionPlaybackState(false)
            updateNotification()
        } else {
            // Resume immediately on 1st click
            requestAudioFocus()
            wasPlayingBeforeTransientLoss = false

            val currentPos = try { player.currentPosition } catch (_: Exception) { -1 }
            val dur = try { player.duration } catch (_: Exception) { 0 }
            if (currentPos < 0 || (dur > 0 && currentPos >= dur - 1000)) {
                try {
                    player.seekTo(0)
                } catch (_: Exception) {}
            }

            val introFactor = audioProcessor.calculateIntroFactor(currentPos.toLong())
            setPlayerVolume(introFactor)

            try {
                player.start()
            } catch (e: Exception) {
                if (ep != null && z != null) {
                    playEpisodeAtZone(
                        episode = ep,
                        zone = z,
                        startPositionMs = 0L,
                        timerDurationMs = 30 * 60 * 1000L
                    )
                    return
                }
            }

            val remaining = if (_playbackState.value.sleepTimerRemainingMs <= 0) {
                if (_playbackState.value.sleepTimerDurationMs > 0) _playbackState.value.sleepTimerDurationMs else 30 * 60 * 1000L
            } else {
                _playbackState.value.sleepTimerRemainingMs
            }

            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                sleepTimerRemainingMs = remaining,
                isSleepTimerActive = true,
                currentVolume = 1.0f
            )

            updateMediaSessionPlaybackState(true)
            startSleepTimer(remaining)
            startProgressTicker()
            updateNotification()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
        updateMediaSessionPlaybackState(_playbackState.value.isPlaying)
    }

    fun setSleepTimerDirect(minutes: Int) {
        val durationMs = minutes * 60 * 1000L
        val introFactor = audioProcessor.calculateIntroFactor(_playbackState.value.currentPositionMs)
        setPlayerVolume(introFactor)

        _playbackState.value = _playbackState.value.copy(
            sleepTimerDurationMs = durationMs,
            sleepTimerRemainingMs = durationMs,
            isSleepTimerActive = true,
            currentVolume = 1.0f
        )

        if (_playbackState.value.isPlaying) {
            startSleepTimer(durationMs)
        }
        updateNotification()
    }

    fun addSleepTimerMinutes(minutes: Int) {
        val addedMs = minutes * 60 * 1000L
        val currentRemaining = _playbackState.value.sleepTimerRemainingMs
        val newRemaining = (maxOf(0L, currentRemaining) + addedMs).coerceIn(10_000L, 180 * 60 * 1000L)
        val newTotal = (_playbackState.value.sleepTimerDurationMs + addedMs).coerceIn(10_000L, 180 * 60 * 1000L)

        val introFactor = audioProcessor.calculateIntroFactor(_playbackState.value.currentPositionMs)
        setPlayerVolume(introFactor)

        _playbackState.value = _playbackState.value.copy(
            sleepTimerDurationMs = newTotal,
            sleepTimerRemainingMs = newRemaining,
            isSleepTimerActive = true,
            currentVolume = 1.0f
        )

        val player = mediaPlayer
        if (player == null) {
            val ep = currentEpisode
            val z = currentZone
            if (ep != null && z != null) {
                playEpisodeAtZone(
                    episode = ep,
                    zone = z,
                    startPositionMs = _playbackState.value.currentPositionMs,
                    timerDurationMs = newRemaining
                )
                return
            }
        } else {
            if (!player.isPlaying) {
                requestAudioFocus()
                try {
                    player.start()
                } catch (_: Exception) {}
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                updateMediaSessionPlaybackState(true)
                startProgressTicker()
            }
            startSleepTimer(newRemaining)
        }

        updateNotification()
    }

    fun setSleepTimerDuration(durationMs: Long) {
        _playbackState.value = _playbackState.value.copy(
            sleepTimerDurationMs = durationMs
        )
    }

    fun stopPlayback() {
        serviceScope.launch {
            wasPlayingBeforeTransientLoss = false
            finalizeCurrentSession()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            audioProcessor.release()

            progressJob?.cancel()
            sleepTimerJob?.cancel()

            abandonAudioFocus()

            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                sleepTimerRemainingMs = 0L,
                isSleepTimerActive = false,
                currentVolume = 1.0f
            )

            updateMediaSessionPlaybackState(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    val pos = try { player.currentPosition.toLong() } catch (_: Exception) { 0L }
                    val ep = currentEpisode
                    if (ep != null && pos >= 0) {
                        val repository = (application as KaamelottApp).repository
                        val zones = repository.getZonesForEpisode(ep.id)
                        val zone = SmartEpisodeSelector.findZoneForPosition(zones, pos) ?: currentZone

                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = pos,
                            currentZone = zone
                        )

                        // If within first 13s, dynamically adjust intro fanfare dampener
                        if (pos < 13_000L && _playbackState.value.sleepTimerRemainingMs > 60_000L) {
                            val introVol = audioProcessor.calculateIntroFactor(pos)
                            setPlayerVolume(introVol)
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        var remainingMs = durationMs

        sleepTimerJob = serviceScope.launch {
            while (isActive && remainingMs > 0) {
                delay(1000L)
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    remainingMs -= 1000L

                    // Fade out logic: last 60 seconds (60,000 ms)
                    val baseVolume = if (remainingMs <= 60_000L) {
                        (remainingMs / 60_000f).coerceIn(0.01f, 1.0f)
                    } else {
                        1.0f
                    }

                    val pos = try { player.currentPosition.toLong() } catch (_: Exception) { 0L }
                    val introFactor = if (pos < 13_000L) audioProcessor.calculateIntroFactor(pos) else 1.0f
                    val finalVolume = baseVolume * introFactor

                    setPlayerVolume(finalVolume)

                    _playbackState.value = _playbackState.value.copy(
                        sleepTimerRemainingMs = maxOf(0L, remainingMs),
                        currentVolume = finalVolume
                    )

                    if (remainingMs <= 0) {
                        // Timer finished: pause player gently and save position
                        setPlayerVolume(0.0f)
                        try {
                            player.pause()
                        } catch (_: Exception) {}
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            sleepTimerRemainingMs = 0L,
                            isSleepTimerActive = false,
                            currentVolume = 0.0f
                        )
                        updateMediaSessionPlaybackState(false)
                        finalizeCurrentSession()
                        updateNotification()
                        break
                    }
                }
            }
        }
    }

    private fun setPlayerVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (_: Exception) {}
    }

    private suspend fun finalizeCurrentSession() {
        val ep = currentEpisode ?: return
        val player = mediaPlayer ?: return
        val currentPos = try { player.currentPosition.toLong() } catch (_: Exception) { 0L }
        val durationListened = maxOf(0L, System.currentTimeMillis() - sessionStartTimestamp)

        val repository = (application as KaamelottApp).repository
        val zoneIdx = currentZone?.zoneIndex ?: 0

        repository.recordSessionAndListen(
            episodeId = ep.id,
            episodeTitle = ep.title,
            zoneIndex = zoneIdx,
            startPosMs = sessionStartPositionMs,
            endPosMs = currentPos,
            listenedMs = durationListened
        )
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            wasPlayingBeforeTransientLoss = false
                            stopPlayback()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            if (_playbackState.value.isPlaying) {
                                wasPlayingBeforeTransientLoss = true
                                try { mediaPlayer?.pause() } catch (_: Exception) {}
                                _playbackState.value = _playbackState.value.copy(isPlaying = false)
                                updateMediaSessionPlaybackState(false)
                                updateNotification()
                            }
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            // CRITICAL FIX: Only resume if it was playing right before the transient interruption
                            // AND user hasn't explicitly paused or sleep timer hasn't expired!
                            if (wasPlayingBeforeTransientLoss && _playbackState.value.sleepTimerRemainingMs > 0) {
                                wasPlayingBeforeTransientLoss = false
                                val introFactor = audioProcessor.calculateIntroFactor(_playbackState.value.currentPositionMs)
                                setPlayerVolume(introFactor)
                                try {
                                    mediaPlayer?.start()
                                    _playbackState.value = _playbackState.value.copy(isPlaying = true)
                                    updateMediaSessionPlaybackState(true)
                                    updateNotification()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
                .build()
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        }
    }

    private fun updateMediaMetadata(episode: EpisodeEntity, durationMs: Long) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, episode.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Kaamelott • Audio Nocturne")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Livre I-VI")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun updateMediaSessionPlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val pos = _playbackState.value.currentPositionMs
        val playbackStateCompat = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, pos, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackStateCompat)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kaamelott Nuit Lecture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Contrôles de lecture audio nocturne"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePending = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val add5Intent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_ADD_5_MIN
        }
        val add5Pending = PendingIntent.getService(
            this, 2, add5Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = currentEpisode?.title ?: "Kaamelott Nuit"
        val remainingMin = (_playbackState.value.sleepTimerRemainingMs / 60000).toInt()
        val isPlaying = _playbackState.value.isPlaying
        val contentText = if (remainingMin > 0) {
            "Minuteur de sommeil : ${remainingMin} min restante(s)"
        } else {
            "Lecture en cours"
        }

        val sessionToken = mediaSession?.sessionToken

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("Kaamelott")
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentIntent(openPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)

        // Action 0: Play / Pause toggle
        builder.addAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Lecture",
            playPausePending
        )

        // Action 1: +5 min extension
        builder.addAction(
            android.R.drawable.ic_input_add,
            "+5 min",
            add5Pending
        )

        // Action 2: Stop
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Arrêter",
            stopPending
        )

        if (sessionToken != null) {
            val mediaStyle = MediaStyle()
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(0, 1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(stopPending)
            builder.setStyle(mediaStyle)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.release()
        audioProcessor.release()
        mediaSession?.isActive = false
        mediaSession?.release()
    }

    companion object {
        const val CHANNEL_ID = "kaamelott_night_playback"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_ADD_5_MIN = "com.example.action.ADD_5_MIN"
    }
}
