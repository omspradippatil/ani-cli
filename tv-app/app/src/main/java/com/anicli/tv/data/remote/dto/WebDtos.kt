package com.anicli.tv.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WebApiResponse<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null
)

data class WebAnimeItem(
    @SerializedName("id") val id: Int,
    @SerializedName("idMal") val idMal: Int? = null,
    @SerializedName("title") val title: WebTitle? = null,
    @SerializedName("coverImage") val coverImage: WebCoverImage? = null,
    @SerializedName("bannerImage") val bannerImage: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("episodes") val episodes: Int? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("seasonYear") val seasonYear: Int? = null,
    @SerializedName("season") val season: String? = null,
    @SerializedName("averageScore") val averageScore: Int? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("description") val description: String? = null
)

data class WebTitle(
    @SerializedName("romaji") val romaji: String? = null,
    @SerializedName("english") val english: String? = null,
    @SerializedName("native") val native: String? = null,
    @SerializedName("userPreferred") val userPreferred: String? = null
) {
    fun preferredTitle(): String {
        return english?.takeIf { it.isNotBlank() }
            ?: romaji?.takeIf { it.isNotBlank() }
            ?: userPreferred?.takeIf { it.isNotBlank() }
            ?: native?.takeIf { it.isNotBlank() }
            ?: "Unknown Anime"
    }
}

data class WebCoverImage(
    @SerializedName("extraLarge") val extraLarge: String? = null,
    @SerializedName("large") val large: String? = null,
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("color") val color: String? = null
) {
    fun bestImageUrl(): String {
        return extraLarge?.takeIf { it.isNotBlank() }
            ?: large?.takeIf { it.isNotBlank() }
            ?: medium?.takeIf { it.isNotBlank() }
            ?: ""
    }
}

data class WebSearchData(
    @SerializedName("anilist") val anilist: List<WebAnimeItem>? = null,
    @SerializedName("hianime") val hianime: List<WebHiAnimeMatch>? = null
)

data class WebHiAnimeMatch(
    @SerializedName("id") val id: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("title") val title: String? = null
)

data class WebAnimeDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("idMal") val idMal: Int? = null,
    @SerializedName("title") val title: WebTitle? = null,
    @SerializedName("coverImage") val coverImage: WebCoverImage? = null,
    @SerializedName("bannerImage") val bannerImage: String? = null,
    @SerializedName("genres") val genres: List<String>? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("format") val format: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("episodes") val episodes: List<WebEpisode>? = null,
    @SerializedName("totalEpisodes") val totalEpisodes: Int? = null,
    @SerializedName("averageScore") val averageScore: Int? = null,
    @SerializedName("streamEpisodes") val streamEpisodes: List<WebEpisode>? = null
)

data class WebEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("number") val number: Int,
    @SerializedName("title") val title: String? = null
)

data class WebStreamData(
    @SerializedName("rawM3u8") val rawM3u8: String? = null,
    @SerializedName("proxyM3u8") val proxyM3u8: String? = null,
    @SerializedName("referer") val referer: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("availableServers") val availableServers: List<WebAvailableServer>? = null,
    @SerializedName("subtitles") val subtitles: List<WebSubtitle>? = null,
    @SerializedName("skip") val skip: WebSkipInfo? = null
)

data class WebAvailableServer(
    @SerializedName("type") val type: String? = null,
    @SerializedName("name") val name: String? = null
)

data class WebSubtitle(
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("label") val label: String? = null,
    @SerializedName("src") val src: String? = null,
    @SerializedName("proxySrc") val proxySrc: String? = null,
    @SerializedName("default") val default: Boolean = false
)

data class WebSkipInfo(
    @SerializedName("intro") val intro: WebSkipRange? = null,
    @SerializedName("outro") val outro: WebSkipRange? = null
)

data class WebSkipRange(
    @SerializedName("start") val start: Int? = null,
    @SerializedName("end") val end: Int? = null
)
