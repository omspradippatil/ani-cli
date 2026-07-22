package com.anicli.tv.provider

import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MockMediaProvider @Inject constructor() : MediaProvider {
    override val name: String = "MockProvider"

    override suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>> = flow {
        delay(500)
        val items = List(10) { i ->
            MediaItem(
                id = "id_$i",
                title = "Anime Title $i",
                posterUrl = "https://picsum.photos/seed/${i}/400/600",
                backdropUrl = "https://picsum.photos/seed/${i}/1280/720"
            )
        }
        val rows = listOf(
            MediaRow("Trending Now", items),
            MediaRow("Recently Added", items.shuffled()),
            MediaRow("Top Rated", items.shuffled())
        )
        emit(Result.success(rows))
    }

    override suspend fun search(query: String): Result<List<MediaItem>> {
        delay(500)
        return Result.success(List(5) { i ->
            MediaItem(
                id = "search_$i",
                title = "Search Result $i",
                posterUrl = "https://picsum.photos/seed/search$i/400/600"
            )
        })
    }

    override suspend fun getDetails(mediaId: String): Result<MediaDetail> {
        delay(500)
        val episodes = List(12) { i ->
            Episode(
                id = "${mediaId}_ep_$i",
                title = "Episode ${i + 1}",
                number = i + 1
            )
        }
        return Result.success(
            MediaDetail(
                id = mediaId,
                title = "Detailed Anime Title",
                description = "This is a detailed description of the anime. It is very interesting and you should watch it.",
                posterUrl = "https://picsum.photos/seed/detail/400/600",
                backdropUrl = "https://picsum.photos/seed/detail/1280/720",
                genres = listOf("Action", "Adventure", "Comedy"),
                episodes = episodes
            )
        )
    }

    override suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>> {
        delay(500)
        return Result.success(listOf(
            StreamSource(
                url = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                quality = "1080p"
            )
        ))
    }
}
