package com.anicli.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.repository.FirebaseRepository
import com.anicli.tv.data.repository.WebUserRepository
import com.anicli.tv.domain.model.MediaItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val providerSelector: ProviderSelector,
    private val watchProgressDao: WatchProgressDao,
    private val favoriteDao: FavoriteDao,
    private val firebaseRepository: FirebaseRepository,
    private val webUserRepository: WebUserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private var allLoadedRows: List<MediaRow> = emptyList()

    fun loadFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Sync cloud progress & watchlist from SQLite backend if logged in
            try {
                webUserRepository.syncWatchHistory()
                webUserRepository.syncWatchlist()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Always get the active provider at the time loadFeed() is called
            val provider = providerSelector.getActiveProvider()

            combine(
                provider.getHomeFeed(),
                watchProgressDao.getAllProgress(),
                favoriteDao.getFavorites()
            ) { feedResult, progressList, favoriteList ->
                feedResult.onSuccess { baseRows ->
                    val finalRows = mutableListOf<MediaRow>()
                    
                    // Filter out corrupt legacy "Unknown Anime" placeholders
                    val validProgress = progressList.filter { 
                        it.mediaTitle != "Unknown Anime" && !it.posterUrl.contains("wp.youtube-anime.com") 
                    }

                    val corruptProgress = progressList.filter { 
                        it.mediaTitle == "Unknown Anime" || it.posterUrl.contains("wp.youtube-anime.com") 
                    }
                    if (corruptProgress.isNotEmpty()) {
                        viewModelScope.launch {
                            for (corrupt in corruptProgress) {
                                watchProgressDao.deleteProgress(corrupt.mediaId)
                            }
                        }
                    }
                    
                    // 1. Continue Watching Row (with progress bar ratio)
                    if (validProgress.isNotEmpty()) {
                        val continueItems = validProgress.map { 
                            val progressRatio = if (it.totalDurationMs > 0) {
                                it.progressMs.toFloat() / it.totalDurationMs.toFloat()
                            } else null
                            MediaItem(
                                id = it.mediaId, 
                                title = it.mediaTitle, 
                                posterUrl = it.posterUrl,
                                backdropUrl = it.posterUrl,
                                progressPercent = progressRatio,
                                episodeLabel = it.episodeTitle.ifBlank { "EP 1" }
                            )
                        }
                        finalRows.add(MediaRow("Continue Watching", continueItems))
                    }

                    // 2. Base rows from web cloud feed (Trending, Popular, Top Rated)
                    finalRows.addAll(baseRows)

                    // 3. My Watchlist Row (if favorites exist)
                    if (favoriteList.isNotEmpty()) {
                        val watchlistItems = favoriteList.map { fav ->
                            MediaItem(
                                id = fav.mediaId,
                                title = fav.title,
                                posterUrl = fav.posterUrl
                            )
                        }
                        finalRows.add(MediaRow("My Watchlist", watchlistItems))
                    }

                    allLoadedRows = finalRows
                    val filtered = applyGenreFilter(finalRows, _state.value.selectedGenre)
                    _state.update { 
                        it.copy(
                            rows = filtered, 
                            isLoading = false, 
                            activeProviderName = provider.name 
                        ) 
                    }
                }.onFailure { error ->
                    _state.update { it.copy(error = error.message, isLoading = false) }
                }
            }.catch { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
            }.collect { }
        }
    }

    fun selectGenre(genre: String) {
        _state.update { current ->
            val updatedGenre = if (current.selectedGenre == genre && genre != "All") "All" else genre
            val filtered = applyGenreFilter(allLoadedRows, updatedGenre)
            current.copy(selectedGenre = updatedGenre, rows = filtered)
        }
    }

    private fun applyGenreFilter(rows: List<MediaRow>, genre: String): List<MediaRow> {
        if (genre == "All" || genre.isBlank()) return rows

        val normalized = genre.lowercase().trim()
        return rows.mapNotNull { row ->
            // Keep Continue Watching & Watchlist as-is or filter
            if (row.title == "Continue Watching" || row.title == "My Watchlist") {
                row
            } else {
                val matched = row.items.filter { item ->
                    item.tags.any { tag -> tag.lowercase().contains(normalized) } ||
                    item.description?.lowercase()?.contains(normalized) == true ||
                    item.title.lowercase().contains(normalized)
                }
                if (matched.isNotEmpty()) MediaRow(row.title, matched) else null
            }
        }.ifEmpty { rows } // fallback to full rows if no exact tag matches to prevent empty screen
    }

    fun toggleWatchlist(item: MediaItem) {
        viewModelScope.launch {
            val isFav = _state.value.favoriteMediaIds.contains(item.id)
            if (isFav) {
                webUserRepository.removeFromWatchlist(item.id)
                _state.update { it.copy(favoriteMediaIds = it.favoriteMediaIds - item.id) }
            } else {
                webUserRepository.addToWatchlist(item.id, item.title, item.posterUrl)
                _state.update { it.copy(favoriteMediaIds = it.favoriteMediaIds + item.id) }
            }
        }
    }
}

data class HomeState(
    val isLoading: Boolean = false,
    val rows: List<MediaRow> = emptyList(),
    val error: String? = null,
    val activeProviderName: String = "",
    val selectedGenre: String = "All",
    val favoriteMediaIds: Set<String> = emptySet()
)
