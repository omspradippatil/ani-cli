package com.anicli.tv.domain.model

data class StreamSource(
    val url: String,
    val quality: String,
    val subtitles: List<Subtitle> = emptyList()
)

data class Subtitle(
    val url: String,
    val language: String
)
