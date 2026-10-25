package com.anicli.tv.domain.provider

import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.model.StreamSource
import kotlinx.coroutines.flow.Flow

interface MediaProvider {
    val name: String
    
    suspend fun getHomeFeed(): Flow<Result<List<MediaRow>>>
    suspend fun search(query: String): Result<List<MediaItem>>
    suspend fun getDetails(mediaId: String): Result<MediaDetail>
    suspend fun getStreamSources(episodeId: String): Result<List<StreamSource>>
    suspend fun getStreamSources(episodeId: String, mode: String): Result<List<StreamSource>> = getStreamSources(episodeId)
}
