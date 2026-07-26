package com.anicli.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.anicli.tv.player.VideoPlayer

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Button

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    viewModel: PlayerViewModel = hiltViewModel(),
    onNextEpisode: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(episodeId) {
        viewModel.loadStreamSources(episodeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Text("Loading stream...", color = Color.White)
            }
            is PlayerUiState.Success -> {
                var selectedSourceIndex by remember { mutableIntStateOf(0) }
                var showQualitySelector by remember { mutableStateOf(false) }
                var isControllerVisible by remember { mutableStateOf(false) }
                
                val source = state.sources.getOrNull(selectedSourceIndex)
                
                val parts = episodeId.split("|")
                val mediaId = parts.getOrNull(0) ?: ""
                val epNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                val nextEpId = "$mediaId|${epNum + 1}"
                
                Box(modifier = Modifier.fillMaxSize()) {
                    if (source != null) {
                        VideoPlayer(
                            videoUrl = source.url,
                            headers = source.headers ?: emptyMap(),
                            okHttpClient = viewModel.okHttpClient,
                            onShowQualitySelector = {
                                showQualitySelector = true
                            },
                            onNextEpisode = {
                                onNextEpisode(nextEpId)
                            },
                            onSettings = {
                                showQualitySelector = true
                            },
                            onControllerVisibilityChanged = { isVisible ->
                                isControllerVisible = isVisible
                            },
                            onSaveProgress = { pos, dur ->
                                viewModel.saveProgress(mediaId, epNum, pos, dur)
                            }
                        )
                    } else {
                        Text("No playable stream found", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    
                    // Physical button overlay when controller is visible
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isControllerVisible && !showQualitySelector,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 32.dp, end = 32.dp)
                    ) {
                        Button(
                            onClick = { showQualitySelector = true }
                        ) {
                            Text("Select Language / Source")
                        }
                    }
                    
                    if (showQualitySelector) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            TvLazyColumn(
                                modifier = Modifier.width(300.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Select Audio/Source",
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }
                                itemsIndexed(state.sources) { index, src ->
                                    Button(
                                        onClick = {
                                            selectedSourceIndex = index
                                            showQualitySelector = false
                                        },
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${src.title}",
                                            color = if (selectedSourceIndex == index) Color.Cyan else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is PlayerUiState.Error -> {
                Text(text = "Error: ${state.message}", color = Color.White)
            }
        }
    }
}
