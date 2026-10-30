package com.anicli.tv.ui.player

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import okhttp3.OkHttpClient
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.local.entity.WatchProgressEntity
import com.anicli.tv.data.repository.FirebaseRepository
import com.anicli.tv.data.repository.WebUserRepository

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val providerSelector: ProviderSelector,
    private val watchProgressDao: WatchProgressDao,
    private val firebaseRepository: FirebaseRepository,
    private val webUserRepository: WebUserRepository,
    private val sharedPreferences: SharedPreferences,
    @javax.inject.Named("VideoClient") val okHttpClient: OkHttpClient
) : ViewModel() {

    companion object {
        private val mediaDetailCache = java.util.concurrent.ConcurrentHashMap<String, MediaDetail>()
    }

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Universal audio mode initialized from SharedPreferences (persisted across app & cloud)
    private val _audioMode = MutableStateFlow(
        sharedPreferences.getString("pref_language", "dub") ?: "dub"
    )
    val audioMode: StateFlow<String> = _audioMode.asStateFlow()

    private var currentEpisodeId: String = ""

    private val _cachedMediaDetail = MutableStateFlow<MediaDetail?>(null)
    val cachedMediaDetail: StateFlow<MediaDetail?> = _cachedMediaDetail.asStateFlow()

    private val _cachedEpisodes = MutableStateFlow<List<com.anicli.tv.domain.model.Episode>>(emptyList())
    val cachedEpisodes: StateFlow<List<com.anicli.tv.domain.model.Episode>> = _cachedEpisodes.asStateFlow()

    private val _cachedAnimeTitle = MutableStateFlow<String>("")
    val cachedAnimeTitle: StateFlow<String> = _cachedAnimeTitle.asStateFlow()

    fun loadStreamSources(episodeId: String, mode: String? = null) {
        currentEpisodeId = episodeId
        // Always respect universal language setting unless explicitly requested
        val activeMode = mode ?: sharedPreferences.getString("pref_language", "dub") ?: "dub"
        _audioMode.value = activeMode

        val parts = episodeId.split("|")
        val mediaId = parts.getOrNull(0) ?: ""

        // Check in-memory cache for immediate 0ms title and episode queue population
        if (mediaId.isNotBlank()) {
            mediaDetailCache[mediaId]?.let { cached ->
                _cachedMediaDetail.value = cached
                _cachedEpisodes.value = cached.episodes
                _cachedAnimeTitle.value = cached.title
            }
        }

        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val provider = providerSelector.getActiveProvider()

                // Concurrently fetch real media details to save proper titles and posters if not cached
                if (mediaId.isNotBlank() && _cachedMediaDetail.value == null) {
                    launch {
                        try {
                            provider.getDetails(mediaId).onSuccess { detail ->
                                mediaDetailCache[mediaId] = detail
                                _cachedMediaDetail.value = detail
                                _cachedEpisodes.value = detail.episodes
                                _cachedAnimeTitle.value = detail.title
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Use the currently active provider for stream resolution with universal mode (sub / dub)
                val result = provider.getStreamSources(episodeId, activeMode)
                result.onSuccess { sources ->
                    if (sources.isNotEmpty()) {
                        _uiState.value = PlayerUiState.Success(sources)
                    } else {
                        _uiState.value = PlayerUiState.Error("No streams found")
                    }
                }.onFailure { error ->
                    _uiState.value = PlayerUiState.Error(error.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleAudioMode() {
        val newMode = if (_audioMode.value == "sub") "dub" else "sub"
        setAudioMode(newMode)
    }

    fun setAudioMode(mode: String) {
        _audioMode.value = mode
        sharedPreferences.edit().putString("pref_language", mode).apply()
        viewModelScope.launch {
            try {
                webUserRepository.updatePreferredAudio(mode)
            } catch (_: Exception) {}
        }
        if (currentEpisodeId.isNotBlank()) {
            loadStreamSources(currentEpisodeId, mode)
        }
    }

    /**
     * Resolves next episode ID based on current episode and cached episode list
     */
    fun getNextEpisodeId(currentEpId: String): String? {
        val parts = currentEpId.split("|")
        val mediaId = parts.getOrNull(0) ?: ""
        val epNum = if (parts.size >= 3) {
            parts[2].toIntOrNull() ?: 1
        } else {
            parts.getOrNull(1)?.toIntOrNull() ?: 1
        }
        val episodes = _cachedEpisodes.value
        val nextEp = episodes.find { it.number == epNum + 1 }
        if (nextEp != null) {
            return nextEp.id
        }
        if (episodes.isNotEmpty() && epNum >= (episodes.maxOfOrNull { it.number } ?: 0)) {
            return null // End of series
        }
        return if (mediaId.isNotBlank()) "$mediaId|${epNum + 1}" else null
    }

    private var lastSavedPositionMs = 0L
    private var lastSavedTimestamp = 0L

    fun saveProgress(mediaId: String, epNum: Int, currentPosition: Long, duration: Long, force: Boolean = false) {
        if (duration <= 0L || currentPosition <= 0L) return
        val now = System.currentTimeMillis()
        if (!force && (now - lastSavedTimestamp < 8000L) && kotlin.math.abs(currentPosition - lastSavedPositionMs) < 8000L) {
            return
        }
        lastSavedTimestamp = now
        lastSavedPositionMs = currentPosition
        viewModelScope.launch {
            try {
                val title = _cachedAnimeTitle.value.takeIf { it.isNotBlank() } ?: "Anime Stream"
                val poster = _cachedMediaDetail.value?.posterUrl?.takeIf { it.isNotBlank() }
                    ?: _cachedMediaDetail.value?.backdropUrl?.takeIf { it.isNotBlank() }
                    ?: ""

                val epId = currentEpisodeId.ifBlank { "$mediaId|$epNum" }
                webUserRepository.saveHistory(
                    animeId = mediaId,
                    title = title,
                    poster = poster,
                    epNum = epNum.toFloat(),
                    episodeId = epId,
                    progressMs = currentPosition,
                    durationMs = duration
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@androidx.compose.runtime.Immutable
sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(val sources: List<StreamSource>) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
