package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey
    val id: String, // Document file uri or unique filename
    val title: String,
    val rawFileName: String,
    val uriString: String,
    val durationMs: Long,
    val listenCount: Int = 0,
    val lastPlaybackPositionMs: Long = 0L,
    val lastListenedTimestamp: Long = 0L,
    val isExcluded: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

