package com.anicli.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaDetail(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val genres: List<String>,
    val episodes: List<Episode>
)
