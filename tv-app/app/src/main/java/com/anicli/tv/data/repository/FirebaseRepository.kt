package com.anicli.tv.data.repository

import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.local.entity.WatchProgressEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy Adapter redirecting Firebase calls to the SQLite / Cloudflare D1 Backend Repository
 */
@Singleton
class FirebaseRepository @Inject constructor(
    private val webUserRepository: WebUserRepository
) {
    fun getCurrentUserId(): String? = webUserRepository.currentUser.value?.id?.toString()
    fun isAnonymous(): Boolean = !webUserRepository.isLoggedIn

    suspend fun signInAnonymously() {
        // No-op for SQLite backend; guest browsing is supported natively
    }

    suspend fun syncFavoritesToCloud(favorites: List<FavoriteEntity>) {
        for (fav in favorites) {
            webUserRepository.addToWatchlist(fav.mediaId, fav.title, fav.posterUrl)
        }
    }

    suspend fun removeFavoriteFromCloud(favorite: FavoriteEntity) {
        webUserRepository.removeFromWatchlist(favorite.mediaId)
    }

    suspend fun syncWatchProgressToCloud(progressList: List<WatchProgressEntity>) {
        for (prog in progressList) {
            webUserRepository.saveHistory(
                animeId = prog.mediaId,
                title = prog.mediaTitle,
                poster = prog.posterUrl,
                epNum = 1f,
                episodeId = prog.episodeId,
                progressMs = prog.progressMs,
                durationMs = prog.totalDurationMs
            )
        }
    }

    suspend fun fetchCloudFavorites(): List<FavoriteEntity> = webUserRepository.syncWatchlist()

    suspend fun fetchCloudWatchProgress(): List<WatchProgressEntity> = webUserRepository.syncWatchHistory()
}
