package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val episodeId: String,
    val episodeTitle: String,
    val zoneIndex: Int,
    val startPositionMs: Long,
    val endPositionMs: Long,
    val durationListenedMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
