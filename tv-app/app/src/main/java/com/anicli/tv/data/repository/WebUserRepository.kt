package com.anicli.tv.data.repository

import android.content.SharedPreferences
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.local.entity.WatchProgressEntity
import com.anicli.tv.data.remote.WebAnimeService
import com.anicli.tv.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebUserRepository @Inject constructor(
    private val webAnimeService: WebAnimeService,
    private val sharedPreferences: SharedPreferences,
    private val watchProgressDao: WatchProgressDao,
    private val favoriteDao: FavoriteDao
) {
    private val gson = Gson()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val KEY_AUTH_TOKEN = "web_auth_token"
        private const val KEY_USER_JSON = "web_user_json"
        private const val KEY_STATS_JSON = "web_user_stats_json"
        private const val KEY_SETTINGS_JSON = "web_user_settings_json"
    }

    private val _token = MutableStateFlow<String?>(sharedPreferences.getString(KEY_AUTH_TOKEN, null))
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _currentUser = MutableStateFlow<WebUser?>(loadCachedUser())
    val currentUser: StateFlow<WebUser?> = _currentUser.asStateFlow()

    private val _userStats = MutableStateFlow<WebUserStats?>(loadCachedStats())
    val userStats: StateFlow<WebUserStats?> = _userStats.asStateFlow()

    private val _userSettings = MutableStateFlow<WebUserSettings?>(loadCachedSettings())
    val userSettings: StateFlow<WebUserSettings?> = _userSettings.asStateFlow()

    val isLoggedIn: Boolean
        get() = !_token.value.isNullOrBlank()

    init {
        // Validate / refresh user session on init
        val savedToken = _token.value
        if (!savedToken.isNullOrBlank()) {
            repositoryScope.launch {
                refreshProfile()
            }
        }
    }

    private fun loadCachedUser(): WebUser? {
        val json = sharedPreferences.getString(KEY_USER_JSON, null) ?: return null
        return try {
            gson.fromJson(json, WebUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadCachedStats(): WebUserStats? {
        val json = sharedPreferences.getString(KEY_STATS_JSON, null) ?: return null
        return try {
            gson.fromJson(json, WebUserStats::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadCachedSettings(): WebUserSettings? {
        val json = sharedPreferences.getString(KEY_SETTINGS_JSON, null) ?: return null
        return try {
            gson.fromJson(json, WebUserSettings::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun persistSession(tokenVal: String, user: WebUser?, stats: WebUserStats?, settings: WebUserSettings?) {
        _token.value = tokenVal
        _currentUser.value = user
        _userStats.value = stats
        _userSettings.value = settings

        sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, tokenVal)
            .apply {
                if (user != null) putString(KEY_USER_JSON, gson.toJson(user))
                if (stats != null) putString(KEY_STATS_JSON, gson.toJson(stats))
                if (settings != null) putString(KEY_SETTINGS_JSON, gson.toJson(settings))
            }
            .apply()
    }

    private fun clearSession() {
        _token.value = null
        _currentUser.value = null
        _userStats.value = null
        _userSettings.value = null

        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_JSON)
            .remove(KEY_STATS_JSON)
            .remove(KEY_SETTINGS_JSON)
            .apply()
    }

    suspend fun refreshProfile(): Boolean = withContext(Dispatchers.IO) {
        val currentToken = _token.value ?: return@withContext false
        try {
            val response = webAnimeService.getProfile("Bearer $currentToken")
            if (response.success && response.data != null) {
                val data = response.data
                _currentUser.value = data.user
                _userStats.value = data.stats
                _userSettings.value = data.settings

                sharedPreferences.edit()
                    .apply {
                        if (data.user != null) putString(KEY_USER_JSON, gson.toJson(data.user))
                        if (data.stats != null) putString(KEY_STATS_JSON, gson.toJson(data.stats))
                        if (data.settings != null) putString(KEY_SETTINGS_JSON, gson.toJson(data.settings))
                    }
                    .apply()
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * 1-Click Demo Login (Alex Mercer) - ideal for TV remote navigation
     */
    suspend fun demoLogin(): Result<WebUser> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.demoLogin()
            val tokenVal = response.token ?: response.data?.token
            val userVal = response.user ?: response.data?.user

            if (response.success && tokenVal != null && userVal != null) {
                persistSession(tokenVal, userVal, response.data?.stats, response.data?.settings)
                // Bidirectionally sync history & watchlist
                syncWatchHistory()
                syncWatchlist()
                Result.success(userVal)
            } else {
                Result.failure(Exception(response.error ?: "Failed to log in with demo account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Standard Username / Password login
     */
    suspend fun login(username: String, password: String): Result<WebUser> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.login(AuthLoginRequest(username.trim(), password))
            val tokenVal = response.token ?: response.data?.token
            val userVal = response.user ?: response.data?.user

            if (response.success && tokenVal != null && userVal != null) {
                persistSession(tokenVal, userVal, response.data?.stats, response.data?.settings)
                syncWatchHistory()
                syncWatchlist()
                Result.success(userVal)
            } else {
                Result.failure(Exception(response.error ?: "Invalid username or password"))
            }
        } catch (e: Exception) {
            val friendlyMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorJson = e.response()?.errorBody()?.string()
                    val parsed = gson.fromJson(errorJson, Map::class.java)
                    parsed["error"]?.toString() ?: "Invalid username/email or password"
                } catch (_: Exception) {
                    "Invalid username/email or password"
                }
            } else {
                e.message ?: "Network error during login"
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * Account registration
     */
    suspend fun register(username: String, email: String?, password: String): Result<WebUser> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.register(
                AuthRegisterRequest(username.trim(), email?.trim()?.takeIf { it.isNotBlank() }, password)
            )
            val tokenVal = response.token ?: response.data?.token
            val userVal = response.user ?: response.data?.user

            if (response.success && tokenVal != null && userVal != null) {
                persistSession(tokenVal, userVal, response.data?.stats, response.data?.settings)
                syncWatchHistory()
                syncWatchlist()
                Result.success(userVal)
            } else {
                Result.failure(Exception(response.error ?: "Registration failed"))
            }
        } catch (e: Exception) {
            val friendlyMsg = if (e is retrofit2.HttpException) {
                try {
                    val errorJson = e.response()?.errorBody()?.string()
                    val parsed = gson.fromJson(errorJson, Map::class.java)
                    parsed["error"]?.toString() ?: "Registration failed"
                } catch (_: Exception) {
                    "Registration failed"
                }
            } else {
                e.message ?: "Network error during registration"
            }
            Result.failure(Exception(friendlyMsg))
        }
    }

    /**
     * TV Code Pairing: Request 6-digit link code for Android TV
     */
    suspend fun generateTvCode(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.generateTvCode()
            val code = response.code
            if (response.success && !code.isNullOrBlank()) {
                Result.success(code)
            } else {
                Result.failure(Exception(response.error ?: "Failed to generate TV link code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * TV Code Pairing: Poll status of TV link code
     */
    suspend fun checkTvCodeStatus(code: String): Result<TvCodeStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.checkTvCodeStatus(code)
            if (response.success) {
                if (response.status == "paired" && response.token != null && response.user != null) {
                    persistSession(response.token, response.user, response.data?.stats, response.data?.settings)
                    syncWatchHistory()
                    syncWatchlist()
                }
                Result.success(response)
            } else {
                Result.failure(Exception(response.error ?: "Code expired"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch registered public accounts for quick switcher
     */
    suspend fun getAvailableAccounts(): Result<List<WebUser>> = withContext(Dispatchers.IO) {
        try {
            val response = webAnimeService.getAccounts()
            if (response.success) {
                Result.success(response.accounts)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch accounts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            try {
                webAnimeService.logout("Bearer $currentToken")
            } catch (_: Exception) {}
        }
        clearSession()
    }

    // ─── Watch History Sync ──────────────────────────────────────────────────

    suspend fun syncWatchHistory(): List<WatchProgressEntity> = withContext(Dispatchers.IO) {
        val currentToken = _token.value ?: return@withContext emptyList()
        try {
            val response = webAnimeService.getWatchHistory("Bearer $currentToken", limit = 50)
            if (response.success && response.data != null) {
                val cloudEntities = response.data.mapNotNull { item ->
                    val aId = item.animeId?.takeIf { it.isNotBlank() && it != "undefined" } ?: return@mapNotNull null
                    val epNum = item.episodeNum?.toInt() ?: 1
                    val epId = item.episodeId?.takeIf { it.isNotBlank() } ?: "$aId|$epNum"
                    val durationMs = ((item.durationSeconds ?: 0f) * 1000L).toLong()
                    val progressMs = ((item.progressSeconds ?: 0f) * 1000L).toLong()

                    WatchProgressEntity(
                        mediaId = aId,
                        episodeId = epId,
                        mediaTitle = item.animeTitle ?: "Anime",
                        episodeTitle = "Episode $epNum",
                        posterUrl = item.poster ?: "",
                        progressMs = progressMs,
                        totalDurationMs = durationMs,
                        lastUpdated = item.updatedAt ?: System.currentTimeMillis()
                    )
                }

                // Cache in local Room database
                for (entity in cloudEntities) {
                    watchProgressDao.insertProgress(entity)
                }
                return@withContext cloudEntities
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    suspend fun saveHistory(
        animeId: String,
        title: String,
        poster: String,
        epNum: Float,
        episodeId: String,
        progressMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (animeId.isBlank() || animeId == "undefined") return@withContext

        val entity = WatchProgressEntity(
            mediaId = animeId,
            episodeId = episodeId.takeIf { it.isNotBlank() } ?: "$animeId|${epNum.toInt()}",
            mediaTitle = title,
            episodeTitle = "Episode ${epNum.toInt()}",
            posterUrl = poster,
            progressMs = progressMs,
            totalDurationMs = durationMs,
            lastUpdated = System.currentTimeMillis()
        )
        // 1. Immediately store in local Room database
        watchProgressDao.insertProgress(entity)

        // 2. Sync to cloud SQLite backend if authenticated
        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            try {
                webAnimeService.saveWatchHistory(
                    "Bearer $currentToken",
                    SaveHistoryRequest(
                        animeId = animeId,
                        animeTitle = title,
                        poster = poster,
                        episodeNum = epNum,
                        episodeId = episodeId,
                        progressSeconds = progressMs / 1000f,
                        durationSeconds = durationMs / 1000f
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteHistoryItem(animeId: String) = withContext(Dispatchers.IO) {
        watchProgressDao.deleteProgress(animeId)
        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            try {
                webAnimeService.deleteWatchHistoryItem("Bearer $currentToken", animeId)
            } catch (_: Exception) {}
        }
    }

    // ─── Watchlist / Bookmarks Sync ──────────────────────────────────────────

    suspend fun syncWatchlist(): List<FavoriteEntity> = withContext(Dispatchers.IO) {
        val currentToken = _token.value ?: return@withContext emptyList()
        try {
            val response = webAnimeService.getWatchlist("Bearer $currentToken")
            if (response.success && response.data != null) {
                val cloudFavorites = response.data.mapNotNull { item ->
                    val aId = item.animeId?.takeIf { it.isNotBlank() && it != "undefined" } ?: return@mapNotNull null
                    FavoriteEntity(
                        mediaId = aId,
                        title = item.animeTitle ?: "Anime",
                        posterUrl = item.poster ?: "",
                        addedAt = item.updatedAt ?: System.currentTimeMillis()
                    )
                }

                // Cache in local Room database
                for (fav in cloudFavorites) {
                    favoriteDao.insertFavorite(fav)
                }
                return@withContext cloudFavorites
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    suspend fun addToWatchlist(
        animeId: String,
        title: String,
        poster: String,
        format: String = "TV",
        score: Float = 0f
    ) = withContext(Dispatchers.IO) {
        if (animeId.isBlank() || animeId == "undefined") return@withContext

        val entity = FavoriteEntity(
            mediaId = animeId,
            title = title,
            posterUrl = poster,
            addedAt = System.currentTimeMillis()
        )
        favoriteDao.insertFavorite(entity)

        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            try {
                webAnimeService.addToWatchlist(
                    "Bearer $currentToken",
                    SaveWatchlistRequest(
                        animeId = animeId,
                        animeTitle = title,
                        poster = poster,
                        format = format,
                        score = score,
                        status = "Watching"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeFromWatchlist(animeId: String) = withContext(Dispatchers.IO) {
        favoriteDao.deleteFavorite(animeId)
        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            try {
                webAnimeService.removeFromWatchlist("Bearer $currentToken", animeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
