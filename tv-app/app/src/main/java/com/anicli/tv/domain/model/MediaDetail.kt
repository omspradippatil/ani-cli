package com.anicli.tv.domain.model

data class MediaDetail(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val genres: List<String>,
    val episodes: List<Episode>
)
