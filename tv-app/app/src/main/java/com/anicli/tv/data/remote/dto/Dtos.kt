package com.anicli.tv.data.remote.dto

data class AnimeListResponse(
    val currentPage: Int,
    val hasNextPage: Boolean,
    val results: List<AnimeDto>
)

data class AnimeDto(
    val id: String,
    val title: String,
    val image: String,
    val url: String?,
    val genres: List<String>?,
    val releaseDate: String?
)

data class AnimeInfoResponse(
    val id: String,
    val title: String,
    val url: String,
    val genres: List<String>?,
    val totalEpisodes: Int,
    val image: String,
    val releaseDate: String?,
    val description: String?,
    val subOrDub: String?,
    val type: String?,
    val status: String?,
    val episodes: List<EpisodeDto>
)

data class EpisodeDto(
    val id: String,
    val number: Int,
    val url: String?
)

data class WatchResponse(
    val headers: Map<String, String>?,
    val sources: List<SourceDto>,
    val download: String?
)

data class SourceDto(
    val url: String,
    val isM3U8: Boolean,
    val quality: String
)
