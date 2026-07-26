package com.anicli.tv.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.StreamSource
import com.anicli.tv.domain.provider.MediaProvider
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

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val provider: MediaProvider,
    private val watchProgressDao: WatchProgressDao,
    private val firebaseRepository: FirebaseRepository,
    @javax.inject.Named("VideoClient") val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadStreamSources(episodeId: String) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            try {
                val result = provider.getStreamSources(episodeId)
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

    fun saveProgress(mediaId: String, epNum: Int, currentPosition: Long, duration: Long) {
        if (duration <= 0L || currentPosition <= 0L) return
        viewModelScope.launch {
            try {
                val entity = WatchProgressEntity(
                    mediaId = mediaId,
                    episodeId = "$mediaId|$epNum",
                    mediaTitle = "Unknown Anime", // Placeholder
                    episodeTitle = "Episode $epNum",
                    posterUrl = "https://wp.youtube-anime.com/aln.youtube-anime.com/images/covers/${mediaId}.jpg",
                    progressMs = currentPosition,
                    totalDurationMs = duration,
                    lastUpdated = System.currentTimeMillis()
                )
                watchProgressDao.insertProgress(entity)
                firebaseRepository.syncWatchProgressToCloud(listOf(entity))
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
