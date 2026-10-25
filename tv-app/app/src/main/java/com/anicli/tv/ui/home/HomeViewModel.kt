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
import com.anicli.tv.data.repository.FirebaseRepository
import com.anicli.tv.domain.model.MediaItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val providerSelector: ProviderSelector,
    private val watchProgressDao: WatchProgressDao,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun loadFeed() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Sync cloud progress if logged in
            try {
                val cloudProgress = firebaseRepository.fetchCloudWatchProgress()
                for (prog in cloudProgress) {
                    watchProgressDao.insertProgress(prog)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Always get the active provider at the time loadFeed() is called
            val provider = providerSelector.getActiveProvider()

            combine(
                provider.getHomeFeed(),
                watchProgressDao.getAllProgress()
            ) { feedResult, progressList ->
                feedResult.onSuccess { baseRows ->
                    val finalRows = mutableListOf<MediaRow>()
                    
                    // Filter out corrupt legacy "Unknown Anime" placeholders
                    val validProgress = progressList.filter { 
                        it.mediaTitle != "Unknown Anime" && !it.posterUrl.contains("wp.youtube-anime.com") 
                    }

                    val corruptProgress = progressList.filter { 
                        it.mediaTitle == "Unknown Anime" || it.posterUrl.contains("wp.youtube-anime.com") 
                    }
                    if (corruptProgress.isNotEmpty()) {
                        viewModelScope.launch {
                            for (corrupt in corruptProgress) {
                                watchProgressDao.deleteProgress(corrupt.mediaId)
                            }
                        }
                    }
                    
                    if (validProgress.isNotEmpty()) {
                        val continueItems = validProgress.map { 
                            MediaItem(
                                id = it.mediaId, 
                                title = it.mediaTitle, 
                                posterUrl = it.posterUrl
                            )
                        }
                        finalRows.add(MediaRow("Continue Watching", continueItems))
                    }
                    
                    finalRows.addAll(baseRows)
                    _state.update { it.copy(rows = finalRows, isLoading = false, activeProviderName = provider.name) }
                }.onFailure { error ->
                    _state.update { it.copy(error = error.message, isLoading = false) }
                }
            }.catch { e ->
                _state.update { it.copy(error = e.message, isLoading = false) }
            }.collect { }
        }
    }
}

data class HomeState(
    val isLoading: Boolean = false,
    val rows: List<MediaRow> = emptyList(),
    val error: String? = null,
    val activeProviderName: String = ""
)
