package com.anicli.tv.data.remote

import com.anicli.tv.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface AnimeApiService {

    @POST("api")
    suspend fun searchAnime(
        @Body request: GraphQLRequest,
        @Header("Referer") referer: String = "https://youtu-chan.com",
        @Header("Origin") origin: String = "https://youtu-chan.com"
    ): GraphQLResponse<SearchData>

    @POST("api")
    suspend fun getAnimeInfo(
        @Body request: GraphQLRequest,
        @Header("Referer") referer: String = "https://youtu-chan.com",
        @Header("Origin") origin: String = "https://youtu-chan.com"
    ): GraphQLResponse<ShowData>

    @GET("api")
    suspend fun getWatchInfo(
        @Query("variables") variables: String,
        @Query("extensions") extensions: String,
        @Header("Referer") referer: String = "https://youtu-chan.com",
        @Header("Origin") origin: String = "https://youtu-chan.com"
    ): GraphQLResponse<EpisodeData>
}
