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
        @Header("Referer") referer: String = "https://mkissa.to",
        @Header("Origin") origin: String = "https://mkissa.to",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0",
        @Header("x-build-id") buildId: String? = null
    ): GraphQLResponse<SearchData>

    @POST("api")
    suspend fun getAnimeInfo(
        @Body request: GraphQLRequest,
        @Header("Referer") referer: String = "https://mkissa.to",
        @Header("Origin") origin: String = "https://mkissa.to",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0",
        @Header("x-build-id") buildId: String? = null
    ): GraphQLResponse<ShowData>

    @GET("api")
    suspend fun getWatchInfo(
        @Query("variables") variables: String,
        @Query("extensions") extensions: String,
        @Header("Referer") referer: String = "https://mkissa.to",
        @Header("Origin") origin: String = "https://mkissa.to",
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0",
        @Header("x-build-id") buildId: String? = null
    ): GraphQLResponse<EpisodeData>
}
