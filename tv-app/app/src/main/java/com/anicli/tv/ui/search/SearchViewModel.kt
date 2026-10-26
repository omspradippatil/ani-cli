package com.anicli.tv.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.provider.ProviderSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val providerSelector: ProviderSelector
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    
    private var searchJob: Job? = null

    init {
        loadRecommendations()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val provider = providerSelector.getActiveProvider()
                provider.getHomeFeed().collect { result ->
                    result.onSuccess { rows ->
                        val trending = rows.firstOrNull { it.title.contains("Trending", ignoreCase = true) }?.items
                            ?: rows.firstOrNull()?.items
                            ?: emptyList()
                        _state.update { it.copy(recommendations = trending) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }
        
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(350) // snappier 350ms debounce
            val result = providerSelector.getActiveProvider().search(trimmed)
            result.onSuccess { items ->
                _state.update { it.copy(results = items, isLoading = false) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }
}

data class SearchState(
    val isLoading: Boolean = false,
    val results: List<MediaItem> = emptyList(),
    val recommendations: List<MediaItem> = emptyList(),
    val error: String? = null
)
