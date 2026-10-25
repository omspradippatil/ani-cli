package com.anicli.tv.provider

import android.content.SharedPreferences
import com.anicli.tv.data.remote.WebAnimeService
import com.anicli.tv.data.remote.dto.WebAnimeItem
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.model.Subtitle
import com.anicli.tv.domain.provider.MediaProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebApiMediaProvider @Inject constructor(
    private val webAnimeService: WebAnimeService,
    private val sharedPreferences: SharedPreferences
) : MediaProvider {

    override val name: String = "Ani-Cli Web Cloud"

    companion object {
        const val PREF_BACKEND_URL = "pref_backend_url"
        const val DEFAULT_BACKEND_URL = "https://ani-cli-steel.vercel.app"
    }

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        try {
            val rows = coroutineScope {
                val trendingDeferred = async {
                    try {
                        webAnimeService.getTrending(1, 20).data ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val popularDeferred = async {
                    try {
                        webAnimeService.getPopular(1, 20).data ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val topRatedDeferred = async {
                    try {
                        webAnimeService.getTopRated(1, 20).data ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val trendingList = trendingDeferred.await()
                val popularList = popularDeferred.await()
                val topRatedList = topRatedDeferred.await()

                val resultRows = mutableListOf<MediaRow>()

                if (trendingList.isNotEmpty()) {
                    resultRows.add(MediaRow("Trending Now", trendingList.map { it.toMediaItem() }))
                }
                if (popularList.isNotEmpty()) {
                    resultRows.add(MediaRow("Most Popular", popularList.map { it.toMediaItem() }))
                }
                if (topRatedList.isNotEmpty()) {
                    resultRows.add(MediaRow("Top Rated", topRatedList.map { it.toMediaItem() }))
                }

                resultRows
            }

            if (rows.isNotEmpty()) {
                emit(Result.success(rows))
            } else {
                emit(Result.failure(Exception("Failed to load anime feed from web backend")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.search(query)
            val results = response.data ?: emptyList()
            Result.success(results.map { it.toMediaItem() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.getAnimeDetails(mediaId)
            val detailData = response.data ?: return@withContext Result.failure(Exception("Anime not found"))

            val title = detailData.title?.preferredTitle() ?: "Anime Details"
            val posterUrl = detailData.coverImage?.bestImageUrl() ?: ""
            val backdropUrl = detailData.bannerImage?.takeIf { it.isNotBlank() } ?: posterUrl
            val cleanDesc = cleanHtmlText(detailData.description)

            // Resolve episodes from streamEpisodes, episodes, or fallback endpoint
            var rawEpisodes = detailData.streamEpisodes?.takeIf { it.isNotEmpty() }
                ?: detailData.episodes?.takeIf { it.isNotEmpty() }
                ?: emptyList()
            if (rawEpisodes.isEmpty()) {
                try {
                    val epResponse = webAnimeService.getEpisodes(mediaId)
                    rawEpisodes = epResponse.data ?: emptyList()
                } catch (e: Exception) {
                    // ignore
                }
            }

            val episodes = rawEpisodes.map { ep ->
                Episode(
                    id = "${mediaId}|${ep.id}|${ep.number}",
                    title = ep.title ?: "Episode ${ep.number}",
                    number = ep.number,
                    thumbnail = backdropUrl
                )
            }

            Result.success(
                MediaDetail(
                    id = mediaId,
                    title = title,
                    description = cleanDesc,
                    posterUrl = posterUrl,
                    backdropUrl = backdropUrl,
                    genres = detailData.genres ?: emptyList(),
                    episodes = episodes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> {
        val defaultMode = sharedPreferences.getString("pref_language", "sub") ?: "sub"
        return getStreamSources(episodeId, defaultMode)
    }

    override suspend fun getStreamSources(episodeId: String, mode: String): Result<List<StreamSource>> = withContext(Dispatchers.IO) {
        try {
            // Resolve streamEpisodeId from episodeId format
            // Can be: "mediaId|streamEpId|epNum" OR "mediaId|epNum" OR "streamEpId"
            var realStreamEpId = episodeId
            if (episodeId.contains("|")) {
                val parts = episodeId.split("|")
                if (parts.size >= 3) {
                    realStreamEpId = parts[1]
                } else if (parts.size == 2) {
                    val mediaId = parts[0]
                    val epNum = parts[1].toIntOrNull() ?: 1
                    val detailsRes = webAnimeService.getAnimeDetails(mediaId)
                    val matchedEp = detailsRes.data?.streamEpisodes?.find { it.number == epNum }
                    realStreamEpId = matchedEp?.id ?: parts[1]
                }
            }

            val streamRes = webAnimeService.getStream(realStreamEpId, mode)
            val streamData = streamRes.data ?: return@withContext Result.failure(Exception("Stream not available for episode $realStreamEpId"))

            val sources = mutableListOf<StreamSource>()
            val referer = streamData.referer ?: "https://zokoanime.video/"
            val defaultHeaders = mapOf(
                "Referer" to referer,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )

            val backendUrl = sharedPreferences.getString(PREF_BACKEND_URL, DEFAULT_BACKEND_URL)
                ?.trim()?.trimEnd('/') ?: DEFAULT_BACKEND_URL

            val parsedSubtitles = (streamData.subtitles ?: emptyList()).map { sub ->
                val finalSubUrl = when {
                    !sub.proxySrc.isNullOrBlank() -> {
                        if (sub.proxySrc.startsWith("http")) sub.proxySrc else "$backendUrl${sub.proxySrc}"
                    }
                    !sub.src.isNullOrBlank() -> sub.src
                    else -> ""
                }
                Subtitle(
                    url = finalSubUrl,
                    language = sub.label ?: sub.lang ?: "English"
                )
            }

            // 1. Direct CDN Stream (Fastest, zero proxy latency)
            if (!streamData.rawM3u8.isNullOrBlank()) {
                sources.add(
                    StreamSource(
                        url = streamData.rawM3u8,
                        title = "${streamData.server ?: "CDN"} (${streamData.type?.uppercase() ?: mode.uppercase()})",
                        quality = "Auto HD",
                        subtitles = parsedSubtitles,
                        headers = defaultHeaders
                    )
                )
            }

            // 2. Web Backend Proxy Stream (Fallback if direct CDN blocks client ISP)
            if (!streamData.proxyM3u8.isNullOrBlank()) {
                val proxyUrl = if (streamData.proxyM3u8.startsWith("http")) {
                    streamData.proxyM3u8
                } else {
                    "$backendUrl${streamData.proxyM3u8}"
                }

                sources.add(
                    StreamSource(
                        url = proxyUrl,
                        title = "Web Proxy (${streamData.type?.uppercase() ?: mode.uppercase()})",
                        quality = "Proxy HLS",
                        subtitles = parsedSubtitles,
                        headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        )
                    )
                )
            }

            if (sources.isNotEmpty()) {
                Result.success(sources)
            } else {
                Result.failure(Exception("No playable streams found for episode $realStreamEpId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun WebAnimeItem.toMediaItem(): MediaItem {
        val titleText = title?.preferredTitle() ?: "Unknown Anime"
        val poster = coverImage?.bestImageUrl() ?: ""
        val backdrop = bannerImage?.takeIf { it.isNotBlank() } ?: poster
        val ratingText = averageScore?.let { "${String.format("%.1f", it / 10.0)} ★" } ?: ""

        return MediaItem(
            id = id.toString(),
            title = titleText,
            posterUrl = poster,
            backdropUrl = backdrop,
            description = cleanHtmlText(description),
            releaseYear = seasonYear?.toString(),
            rating = ratingText,
            tags = genres ?: emptyList()
        )
    }

    private fun cleanHtmlText(raw: String?): String {
        if (raw.isNullOrBlank()) return "No description available."
        return raw
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<i>", "")
            .replace("</i>", "")
            .replace("<b>", "")
            .replace("</b>", "")
            .replace("<em>", "")
            .replace("</em>", "")
            .replace("<strong>", "")
            .replace("</strong>", "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }
}
