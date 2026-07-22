package com.anicli.tv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val mediaId: String,
    val episodeId: String,
    val mediaTitle: String,
    val episodeTitle: String,
    val posterUrl: String,
    val progressMs: Long,
    val totalDurationMs: Long,
    val lastUpdated: Long
)
