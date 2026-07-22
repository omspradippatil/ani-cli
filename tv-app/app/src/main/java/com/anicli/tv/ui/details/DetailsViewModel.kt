package com.anicli.tv.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.MediaDetail
import com.anicli.tv.domain.provider.MediaProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val provider: MediaProvider
) : ViewModel() {

    private val _state = MutableStateFlow(DetailsState())
    val state: StateFlow<DetailsState> = _state.asStateFlow()

    fun loadDetails(mediaId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = provider.getDetails(mediaId)
            result.onSuccess { detail ->
                _state.update { it.copy(detail = detail, isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }
}

data class DetailsState(
    val isLoading: Boolean = false,
    val detail: MediaDetail? = null,
    val error: String? = null
)
