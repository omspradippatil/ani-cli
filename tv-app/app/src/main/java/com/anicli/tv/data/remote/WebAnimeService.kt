package com.anicli.tv.data.remote

import com.anicli.tv.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WebAnimeService {

    @GET("api/trending")
    suspend fun getTrending(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 20
    ): WebApiResponse<List<WebAnimeItem>>

    @GET("api/popular")
    suspend fun getPopular(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 20
    ): WebApiResponse<List<WebAnimeItem>>

    @GET("api/top-rated")
    suspend fun getTopRated(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 20
    ): WebApiResponse<List<WebAnimeItem>>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String
    ): WebApiResponse<List<WebAnimeItem>>

    @GET("api/anime/{id}")
    suspend fun getAnimeDetails(
        @Path("id") id: String
    ): WebApiResponse<WebAnimeDetail>

    @GET("api/episodes/{idOrSlug}")
    suspend fun getEpisodes(
        @Path("idOrSlug") idOrSlug: String
    ): WebApiResponse<List<WebEpisode>>

    @GET("api/stream/{episodeId}")
    suspend fun getStream(
        @Path("episodeId") episodeId: String,
        @Query("mode") mode: String = "sub"
    ): WebApiResponse<WebStreamData>
}
