package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "zones",
    indices = [Index(value = ["episodeId", "zoneIndex"], unique = true)]
)
data class ZoneEntity(
    @PrimaryKey
    val id: String, // format: "${episodeId}_zone_${zoneIndex}"
    val episodeId: String,
    val zoneIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val listenCount: Int = 0,
    val isAvoided: Boolean = false,
    val lastListenedTimestamp: Long = 0L
)
