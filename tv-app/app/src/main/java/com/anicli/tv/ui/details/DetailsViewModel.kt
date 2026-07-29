package com.anicli.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.data.local.dao.FavoriteDao
import com.anicli.tv.data.local.entity.FavoriteEntity
import com.anicli.tv.data.repository.FirebaseRepository
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val providerSelector: ProviderSelector,
    private val favoriteDao: FavoriteDao,
    private val firebaseRepository: FirebaseRepository
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
            
            val entity = FavoriteEntity(
                mediaId = mediaId,
                title = currentDetail.title,
                posterUrl = currentDetail.posterUrl,
                addedAt = System.currentTimeMillis()
            )
            
            if (isFav) {
                favoriteDao.deleteFavorite(mediaId)
                firebaseRepository.removeFavoriteFromCloud(entity)
            } else {
                favoriteDao.insertFavorite(entity)
                firebaseRepository.syncFavoritesToCloud(listOf(entity))
            }
        }
    }
}

data class DetailsState(
    val isLoading: Boolean = false,
    val detail: MediaDetail? = null,
    val isFavorite: Boolean = false,
    val error: String? = null
)
