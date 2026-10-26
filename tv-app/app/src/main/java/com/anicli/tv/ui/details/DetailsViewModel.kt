package com.anicli.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.repository.FirebaseRepository
import com.anicli.tv.data.repository.WebUserRepository
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.anicli.tv.data.local.dao.WatchProgressDao
import com.anicli.tv.data.local.entity.WatchProgressEntity

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val providerSelector: ProviderSelector,
    private val favoriteDao: FavoriteDao,
    private val watchProgressDao: WatchProgressDao,
    private val firebaseRepository: FirebaseRepository,
    private val webUserRepository: WebUserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadDetails(mediaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Also check if favorite
            launch {
                favoriteDao.isFavorite(mediaId).collect { isFav ->
                    _state.update { it.copy(isFavorite = isFav) }
                }
            }

            // Check existing watch progress
            try {
                val progress = watchProgressDao.getProgressForMedia(mediaId)
                _state.update { it.copy(watchProgress = progress) }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Use the currently active provider
            val result = providerSelector.getActiveProvider().getDetails(mediaId)
            result.onSuccess { detail ->
                _state.update { it.copy(detail = detail, isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }
    
    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            val currentDetail = _state.value.detail ?: return@launch
            val isFav = _state.value.isFavorite
            
            if (isFav) {
                webUserRepository.removeFromWatchlist(mediaId)
            } else {
                webUserRepository.addToWatchlist(
                    animeId = mediaId,
                    title = currentDetail.title,
                    poster = currentDetail.posterUrl
                )
            }
        }
    }
}

data class DetailsState(
    val isLoading: Boolean = false,
    val detail: MediaDetail? = null,
    val isFavorite: Boolean = false,
    val watchProgress: WatchProgressEntity? = null,
    val error: String? = null
)
