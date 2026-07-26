package com.anicli.tv.ui.mobile

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.anicli.tv.ui.player.PlayerUiState
import com.anicli.tv.ui.player.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun MobilePlayerScreen(
    episodeId: String,
    onNextEpisode: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(episodeId) {
        viewModel.loadStreamSources(episodeId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is PlayerUiState.Success -> {
                val source = state.sources.firstOrNull()
                if (source != null) {
                    val context = LocalContext.current
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            val mediaItem = MediaItem.fromUri(source.url)
                            setMediaItem(mediaItem)
                            prepare()
                            playWhenReady = true
                        }
                    }

                    val parts = episodeId.split("|")
                    val mediaId = parts.getOrNull(0) ?: ""
                    val epNum = parts.getOrNull(1)?.toIntOrNull() ?: 1

                    DisposableEffect(Unit) {
                        onDispose {
                            viewModel.saveProgress(mediaId, epNum, exoPlayer.currentPosition, exoPlayer.duration)
                            exoPlayer.release()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                useController = true // Enable default touch controls
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("No stream found", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
            is PlayerUiState.Error -> {
                Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
        
        // Back Button Overlay
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}
