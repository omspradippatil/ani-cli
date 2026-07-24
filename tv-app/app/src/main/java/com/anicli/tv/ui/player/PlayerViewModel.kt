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

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val provider: MediaProvider
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
}

sealed class PlayerUiState {
    object Loading : PlayerUiState()
    data class Success(val sources: List<StreamSource>) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
