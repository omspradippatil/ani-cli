package com.anicli.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.MediaRow
import com.anicli.tv.domain.provider.MediaProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val provider: MediaProvider
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun loadFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            provider.getHomeFeed().collect { result ->
                result.onSuccess { rows ->
                    _state.update { it.copy(rows = rows, isLoading = false) }
                }.onFailure { error ->
                    _state.update { it.copy(error = error.message, isLoading = false) }
                }
            }
        }
    }
}

data class HomeState(
    val isLoading: Boolean = false,
    val rows: List<MediaRow> = emptyList(),
    val error: String? = null
)
