package com.anicli.tv.data.remote

import com.anicli.tv.data.remote.dto.*
import retrofit2.http.*

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
        @Path("id") id: String,
        @Query("title") title: String? = null
    ): WebApiResponse<WebAnimeDetail>

    @GET("api/episodes/{idOrSlug}")
    suspend fun getEpisodes(
        @Path("idOrSlug") idOrSlug: String
    ): WebApiResponse<List<WebEpisode>>

    @GET("api/episodes")
    suspend fun getEpisodesByQuery(
        @Query("query") query: String
    ): WebApiResponse<List<WebEpisode>>

    @GET("api/stream/{episodeId}")
    suspend fun getStream(
        @Path("episodeId") episodeId: String,
        @Query("mode") mode: String = "sub"
    ): WebApiResponse<WebStreamData>

    // ─── SQLite / Cloudflare D1 Backend Auth & Session ──────────────────────

    @POST("api/auth/login")
    suspend fun login(
        @Body body: AuthLoginRequest
    ): AuthResponse

    @POST("api/auth/register")
    suspend fun register(
        @Body body: AuthRegisterRequest
    ): AuthResponse

    @POST("api/auth/demo")
    suspend fun demoLogin(): AuthResponse

    @GET("api/auth/accounts")
    suspend fun getAccounts(): WebAccountsResponse

    @POST("api/auth/tv-code/generate")
    suspend fun generateTvCode(): TvCodeGenerateResponse

    @GET("api/auth/tv-code/status")
    suspend fun checkTvCodeStatus(
        @Query("code") code: String
    ): TvCodeStatusResponse

    @GET("api/auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): WebApiResponse<UserProfileData>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): WebApiResponse<Any>

    // ─── User Profile & Playback Settings ───────────────────────────────────

    @GET("api/user/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): WebApiResponse<UserProfileData>

    @GET("api/user/settings")
    suspend fun getUserSettings(
        @Header("Authorization") token: String
    ): WebApiResponse<WebUserSettings>

    @POST("api/user/settings")
    suspend fun updateUserSettings(
        @Header("Authorization") token: String,
        @Body body: Map<String, Any>
    ): WebApiResponse<WebUserSettings>

    // ─── Watch History (SQLite Cloud Sync) ─────────────────────────────────

    @GET("api/user/history")
    suspend fun getWatchHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50
    ): WebApiResponse<List<WebHistoryItem>>

    @POST("api/user/history")
    suspend fun saveWatchHistory(
        @Header("Authorization") token: String,
        @Body body: SaveHistoryRequest
    ): WebApiResponse<Any>

    @DELETE("api/user/history/{animeId}")
    suspend fun deleteWatchHistoryItem(
        @Header("Authorization") token: String,
        @Path("animeId") animeId: String
    ): WebApiResponse<Any>

    @DELETE("api/user/history")
    suspend fun clearWatchHistory(
        @Header("Authorization") token: String
    ): WebApiResponse<Any>

    // ─── Watchlist / Bookmarks (SQLite Cloud Sync) ─────────────────────────

    @GET("api/user/watchlist")
    suspend fun getWatchlist(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null
    ): WebApiResponse<List<WebWatchlistItem>>

    @POST("api/user/watchlist")
    suspend fun addToWatchlist(
        @Header("Authorization") token: String,
        @Body body: SaveWatchlistRequest
    ): WebApiResponse<Any>

    @DELETE("api/user/watchlist/{animeId}")
    suspend fun removeFromWatchlist(
        @Header("Authorization") token: String,
        @Path("animeId") animeId: String
    ): WebApiResponse<Any>

    @GET("api/user/watchlist/check/{animeId}")
    suspend fun checkWatchlist(
        @Header("Authorization") token: String,
        @Path("animeId") animeId: String
    ): CheckWatchlistResponse
}
