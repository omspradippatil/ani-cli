package com.anicli.tv.data.remote

import com.anicli.tv.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnimeApiService {
    @GET("anime/gogoanime/top-airing")
    suspend fun getTopAiring(): AnimeListResponse

    @GET("anime/gogoanime/recent-release")
    suspend fun getRecentRelease(): AnimeListResponse

    @GET("anime/gogoanime/{query}")
    suspend fun searchAnime(@Path("query") query: String, @Query("page") page: Int = 1): AnimeListResponse

    @GET("anime/gogoanime/info/{id}")
    suspend fun getAnimeInfo(@Path("id") id: String): AnimeInfoResponse

    @GET("anime/gogoanime/watch/{episodeId}")
    suspend fun getWatchInfo(@Path("episodeId") episodeId: String): WatchResponse
}
