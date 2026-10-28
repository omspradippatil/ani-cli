package com.anicli.tv.ui.player

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
    @javax.inject.Named("VideoClient") val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _audioMode = MutableStateFlow("sub")
    val audioMode: StateFlow<String> = _audioMode.asStateFlow()

    private var currentEpisodeId: String = ""

    private var cachedMediaDetail: MediaDetail? = null
    val cachedAnimeTitle: String get() = cachedMediaDetail?.title ?: ""
    val cachedEpisodes: List<com.anicli.tv.domain.model.Episode> get() = cachedMediaDetail?.episodes ?: emptyList()

    fun loadStreamSources(episodeId: String, mode: String = _audioMode.value) {
        currentEpisodeId = episodeId
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val parts = episodeId.split("|")
                val mediaId = parts.getOrNull(0) ?: ""
                val provider = providerSelector.getActiveProvider()

                // Concurrently fetch real media details to save proper titles and posters
                if (mediaId.isNotBlank() && cachedMediaDetail == null) {
                    launch {
                        try {
                            provider.getDetails(mediaId).onSuccess { detail ->
                                cachedMediaDetail = detail
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Use the currently active provider for stream resolution with mode (sub / dub)
                val result = provider.getStreamSources(episodeId, mode)
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
        _audioMode.value = newMode
        if (currentEpisodeId.isNotBlank()) {
            loadStreamSources(currentEpisodeId, newMode)
        }
    }

    fun setAudioMode(mode: String) {
        if (_audioMode.value == mode) return
        _audioMode.value = mode
        if (currentEpisodeId.isNotBlank()) {
            loadStreamSources(currentEpisodeId, mode)
        }
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
                val title = cachedMediaDetail?.title?.takeIf { it.isNotBlank() } ?: "Anime Stream"
                val poster = cachedMediaDetail?.posterUrl?.takeIf { it.isNotBlank() }
                    ?: cachedMediaDetail?.backdropUrl?.takeIf { it.isNotBlank() }
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

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(val sources: List<StreamSource>) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
