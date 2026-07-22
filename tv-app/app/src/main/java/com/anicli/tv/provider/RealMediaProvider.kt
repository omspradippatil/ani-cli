package com.anicli.tv.provider

import com.anicli.tv.data.remote.AnimeApiService
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealMediaProvider @Inject constructor(
    private val apiService: AnimeApiService
) : MediaProvider {

    override val name: String = "Consumet"

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        try {
            val topAiring = apiService.getTopAiring()
            val recent = apiService.getRecentRelease()

            val rows = mutableListOf<MediaRow>()
            
            if (topAiring.results.isNotEmpty()) {
                rows.add(
                    MediaRow(
                        title = "Top Airing Anime",
                        items = topAiring.results.map {
                            MediaItem(
                                id = it.id,
                                title = it.title,
                                posterUrl = it.image,
                                backdropUrl = it.image, // fallback
                                tags = it.genres ?: emptyList()
                            )
                        }
                    )
                )
            }

            if (recent.results.isNotEmpty()) {
                rows.add(
                    MediaRow(
                        title = "Recently Updated",
                        items = recent.results.map {
                            MediaItem(
                                id = it.id,
                                title = it.title,
                                posterUrl = it.image,
                                backdropUrl = it.image,
                                tags = it.genres ?: emptyList()
                            )
                        }
                    )
                )
            }
            emit(Result.success(rows))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> {
        return try {
            val info = apiService.getAnimeInfo(mediaId)
            Result.success(
                MediaDetail(
                    id = info.id,
                    title = info.title,
                    description = info.description ?: "No description available.",
                    posterUrl = info.image,
                    backdropUrl = info.image,
                    genres = info.genres ?: emptyList(),
                    episodes = info.episodes.map { ep ->
                        Episode(
                            id = ep.id,
                            title = "Episode ${ep.number}",
                            number = ep.number,
                            thumbnail = info.image,
                            isFiller = false
                        )
                    }
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return try {
            val searchResponse = apiService.searchAnime(query)
            Result.success(
                searchResponse.results.map {
                    MediaItem(
                        id = it.id,
                        title = it.title,
                        posterUrl = it.image,
                        backdropUrl = it.image,
                        tags = it.genres ?: emptyList()
                    )
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> {
        return try {
            val watchData = apiService.getWatchInfo(episodeId)
            val sources = watchData.sources.map {
                StreamSource(
                    url = it.url,
                    title = "Streaming $episodeId",
                    quality = it.quality,
                    subtitles = emptyList(),
                    headers = watchData.headers
                )
            }
            Result.success(sources)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
