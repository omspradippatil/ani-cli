package com.anicli.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String? = null,
    val description: String? = null,
    val releaseYear: String? = null,
    val rating: String? = null,
    val tags: List<String> = emptyList(),
    val progressPercent: Float? = null,
    val episodeLabel: String? = null
)
