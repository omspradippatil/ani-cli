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
import kotlinx.coroutines.flow.firstOrNull
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

        fun getDetailFromCache(mediaId: String): MediaDetail? = mediaDetailCache[mediaId]
        fun putDetailInCache(mediaId: String, detail: MediaDetail) {
            mediaDetailCache[mediaId] = detail
        }
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

    private val _savedPlaybackPositionMs = MutableStateFlow<Long>(0L)
    val savedPlaybackPositionMs: StateFlow<Long> = _savedPlaybackPositionMs.asStateFlow()

    fun loadStreamSources(episodeId: String, mode: String? = null) {
        currentEpisodeId = episodeId
        _uiState.value = PlayerUiState.Loading
        _savedPlaybackPositionMs.value = 0L
        // Always respect universal language setting unless explicitly requested
        val activeMode = mode ?: sharedPreferences.getString("pref_language", "dub") ?: "dub"
        _audioMode.value = activeMode

        val parts = episodeId.split("|")
        val mediaId = parts.getOrNull(0) ?: ""
        val epNum = if (parts.size >= 3) {
            val p = parts.lastOrNull()?.toIntOrNull() ?: 1
            if (p < 2000) p else 1
        } else if (parts.size == 2) {
            val p = parts.getOrNull(1)?.toIntOrNull() ?: 1
            if (p < 2000) p else 1
        } else {
            1
        }

        // Query saved progress to resume playback at exact millisecond
        if (mediaId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val progress = watchProgressDao.getProgressForMedia(mediaId)
                        ?: watchProgressDao.getAllProgress().firstOrNull()?.find {
                            it.mediaId == mediaId || it.mediaId.contains(mediaId) || mediaId.contains(it.mediaId)
                        }
                    if (progress != null && progress.progressMs > 3000L) {
                        val savedEpNum = progress.episodeId.split("|").lastOrNull()?.toIntOrNull()
                        val isSameEpisode = progress.episodeId == episodeId ||
                            progress.episodeId.contains(episodeId) ||
                            episodeId.contains(progress.episodeId) ||
                            (savedEpNum != null && savedEpNum == epNum)
                        if (isSameEpisode) {
                            val isNearEnd = progress.totalDurationMs > 0L && progress.progressMs >= (progress.totalDurationMs - 15000L)
                            _savedPlaybackPositionMs.value = if (!isNearEnd) progress.progressMs else 0L
                        }
                    }
                } catch (_: Exception) {}
            }
        }

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
     * Resolves next episode ID based on current episode and cached episode list.
     * GUARANTEE: Never plays a random anime. Only moves within the same anime series.
     * Returns null when series is finished or next episode does not exist.
     */
    fun getNextEpisodeId(currentEpId: String): String? {
        val parts = currentEpId.split("|")
        val mediaId = parts.getOrNull(0) ?: ""
        val epNum = if (parts.size >= 3) {
            val p = parts.lastOrNull()?.toIntOrNull() ?: 1
            if (p < 2000) p else 1
        } else if (parts.size == 2) {
            val p = parts.getOrNull(1)?.toIntOrNull() ?: 1
            if (p < 2000) p else 1
        } else {
            1
        }

        val episodes = _cachedEpisodes.value.takeIf { it.isNotEmpty() }
            ?: mediaDetailCache[mediaId]?.episodes?.takeIf { it.isNotEmpty() }
            ?: _cachedMediaDetail.value?.episodes?.takeIf { it.isNotEmpty() }
            ?: emptyList()

        if (episodes.isNotEmpty()) {
            val nextEp = episodes.find { it.number == epNum + 1 }
            if (nextEp != null) {
                return nextEp.id
            }
            val maxEp = episodes.maxOfOrNull { it.number } ?: 0
            if (epNum >= maxEp) {
                return null // End of series! Stop playback cleanly.
            }
        }

        // Check total episodes from cached media detail if episode list is sparse
        val totalEps = _cachedMediaDetail.value?.episodes?.size
            ?: mediaDetailCache[mediaId]?.episodes?.size
            ?: 0
        if (totalEps > 0 && epNum >= totalEps) {
            return null // End of series!
        }

        // Never guess or fallback to arbitrary numbers that could map to random anime
        return null
    }

    val preferredQuality: String
        get() = sharedPreferences.getString("pref_quality", "1080p") ?: "1080p"

    private val saveScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private var lastSavedPositionMs = 0L
    private var lastSavedTimestamp = 0L

    fun saveProgress(mediaId: String, epNum: Int, currentPosition: Long, duration: Long, force: Boolean = false) {
        if (duration <= 0L || currentPosition <= 0L || mediaId.isBlank()) return
        val now = System.currentTimeMillis()
        if (!force && (now - lastSavedTimestamp < 8000L) && kotlin.math.abs(currentPosition - lastSavedPositionMs) < 8000L) {
            return
        }
        lastSavedTimestamp = now
        lastSavedPositionMs = currentPosition
        saveScope.launch {
            try {
                // Resolve true mediaId if mediaId was accidentally passed as an episode ID
                val resolvedMediaId = if (mediaDetailCache.containsKey(mediaId)) {
                    mediaId
                } else {
                    mediaDetailCache.entries.find { entry ->
                        entry.value.episodes.any { it.id == mediaId || it.id == currentEpisodeId }
                    }?.key ?: _cachedMediaDetail.value?.id ?: mediaId
                }

                var title = _cachedAnimeTitle.value.takeIf { it.isNotBlank() && it != "Anime Stream" && it != "Anime" && it != "Anime Details" && it != "Continue Watching" }
                    ?: mediaDetailCache[resolvedMediaId]?.title?.takeIf { it.isNotBlank() && it != "Anime Stream" && it != "Anime" && it != "Anime Details" && it != "Continue Watching" }
                var poster = _cachedMediaDetail.value?.backdropUrl?.takeIf { it.isNotBlank() }
                    ?: _cachedMediaDetail.value?.posterUrl?.takeIf { it.isNotBlank() }
                    ?: mediaDetailCache[resolvedMediaId]?.backdropUrl?.takeIf { it.isNotBlank() }
                    ?: mediaDetailCache[resolvedMediaId]?.posterUrl?.takeIf { it.isNotBlank() }

                // Fallback to existing saved progress poster/title if available
                val existing = watchProgressDao.getProgressForMedia(resolvedMediaId)
                if (title.isNullOrBlank()) title = existing?.mediaTitle?.takeIf { it.isNotBlank() && it != "Anime Stream" && it != "Anime" && it != "Anime Details" }
                if (poster.isNullOrBlank()) poster = existing?.posterUrl?.takeIf { it.isNotBlank() }

                val finalTitle = title?.takeIf { it.isNotBlank() } ?: "Anime #$resolvedMediaId"
                val finalPoster = poster?.takeIf { it.isNotBlank() } ?: ""
                val epId = currentEpisodeId.ifBlank { "$resolvedMediaId|$epNum" }

                // 1. Immediately write to local Room database (un-cancellable, fast)
                val entity = WatchProgressEntity(
                    mediaId = resolvedMediaId,
                    episodeId = epId,
                    mediaTitle = finalTitle,
                    episodeTitle = "Episode $epNum",
                    posterUrl = finalPoster,
                    progressMs = currentPosition,
                    totalDurationMs = duration,
                    lastUpdated = System.currentTimeMillis()
                )
                watchProgressDao.insertProgress(entity)

                // 2. Sync to cloud backend in background
                webUserRepository.saveHistory(
                    animeId = resolvedMediaId,
                    title = finalTitle,
                    poster = finalPoster,
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
