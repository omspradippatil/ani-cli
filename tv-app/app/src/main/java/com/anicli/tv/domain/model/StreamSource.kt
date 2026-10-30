package com.anicli.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class StreamSource(
    val url: String,
    val title: String = "",
    val quality: String = "Auto",
    val subtitles: List<Subtitle> = emptyList(),
    val headers: Map<String, String>? = null
)

@Immutable
data class Subtitle(
    val url: String,
    val language: String
)
