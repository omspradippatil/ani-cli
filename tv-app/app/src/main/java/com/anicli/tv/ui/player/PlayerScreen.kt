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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    viewModel: PlayerViewModel = hiltViewModel()
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
                // Play the first stream for now (usually auto or 1080p)
                val source = state.sources.firstOrNull()
                if (source != null) {
                    VideoPlayer(
                        videoUrl = source.url,
                        headers = source.headers ?: emptyMap()
                    )
                } else {
                    Text("No playable stream found", color = Color.White)
                }
            }
            is PlayerUiState.Error -> {
                Text(text = "Error: ${state.message}", color = Color.White)
            }
        }
    }
}
