package com.example.service

import com.example.data.model.EpisodeEntity
import com.example.data.model.ZoneEntity

data class PlaybackState(
    val currentEpisode: EpisodeEntity? = null,
    val currentZone: ZoneEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val sleepTimerDurationMs: Long = 30 * 60 * 1000L, // default 30 min
    val sleepTimerRemainingMs: Long = 0L,
    val isSleepTimerActive: Boolean = false,
    val currentVolume: Float = 1.0f,
    val errorMessage: String? = null
)
