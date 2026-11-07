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
                    
                    // 1. Continue Watching Row (with progress bar ratio, auto-healing, and episodeId preservation)
                    val continueItems = progressList.mapNotNull { prog ->
                        if (prog.mediaId.isBlank() || prog.mediaId == "undefined") return@mapNotNull null

                        var title = prog.mediaTitle.takeIf { 
                            it.isNotBlank() && it != "Unknown Anime" && it != "Anime Stream" && it != "Anime" && it != "Anime Details" && !it.startsWith("Anime #")
                        }
                        var poster = prog.posterUrl.takeIf { 
                            it.isNotBlank() && !it.contains("wp.youtube-anime.com") 
                        }

                        // Auto-heal title and poster from in-memory catalog rows if missing
                        if (title == null || poster == null) {
                            val matchedCatalog = baseRows.flatMap { it.items }.find { it.id == prog.mediaId }
                            if (title == null && matchedCatalog != null) title = matchedCatalog.title
                            if (poster == null && matchedCatalog != null) poster = matchedCatalog.backdropUrl ?: matchedCatalog.posterUrl
                        }

                        // Also check memory cache from PlayerViewModel
                        if (title == null || poster == null) {
                            val cached = com.anicli.tv.ui.player.PlayerViewModel.getDetailFromCache(prog.mediaId)
                            if (title == null && cached != null && !cached.title.startsWith("Anime #")) title = cached.title
                            if (poster == null && cached != null) poster = cached.backdropUrl.takeIf { it.isNotBlank() } ?: cached.posterUrl
                        }

                        // Never show blank cards with missing title or poster
                        if (title.isNullOrBlank() || poster.isNullOrBlank()) return@mapNotNull null

                        val progressRatio = if (prog.totalDurationMs > 0) {
                            (prog.progressMs.toFloat() / prog.totalDurationMs.toFloat()).coerceIn(0.01f, 1f)
                        } else 0.5f

                        val parsedEpNum = Regex("""\b(?:ep|episode)\s*(\d+)\b""", RegexOption.IGNORE_CASE)
                            .find(prog.episodeTitle)?.groupValues?.get(1)?.toIntOrNull()
                            ?: prog.episodeId.split("|").lastOrNull()?.toIntOrNull()?.takeIf { it < 2000 }
                            ?: Regex("""\b(\d+)\b""").find(prog.episodeTitle)?.groupValues?.get(1)?.toIntOrNull()
                            ?: 1

                        MediaItem(
                            id = prog.mediaId, 
                            title = title, 
                            posterUrl = poster,
                            backdropUrl = poster,
                            progressPercent = progressRatio,
                            episodeLabel = "EPISODE $parsedEpNum",
                            episodeId = prog.episodeId
                        )
                    }

                    if (continueItems.isNotEmpty()) {
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
                    val favIds = favoriteList.map { it.mediaId }.toSet()
                    _state.update { 
                        it.copy(
                            rows = filtered, 
                            isLoading = false, 
                            activeProviderName = provider.name,
                            favoriteMediaIds = favIds
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

    fun removeFromContinueWatching(mediaId: String) {
        viewModelScope.launch {
            try {
                webUserRepository.deleteHistoryItem(mediaId)
            } catch (e: Exception) {
                try {
                    watchProgressDao.deleteProgress(mediaId)
                } catch (_: Exception) {}
            }
        }
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

@androidx.compose.runtime.Immutable
data class HomeState(
    val isLoading: Boolean = false,
    val rows: List<MediaRow> = emptyList(),
    val error: String? = null,
    val activeProviderName: String = "",
    val selectedGenre: String = "All",
    val favoriteMediaIds: Set<String> = emptySet()
)
