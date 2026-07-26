
package com.anicli.tv.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import kotlinx.coroutines.delay
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient


@Composable
fun VideoPlayer(
    videoUrl: String,
    headers: Map<String, String> = emptyMap(),
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier,
    onStateChanged: (PlayerState) -> Unit = {},
    onShowQualitySelector: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onSettings: () -> Unit = {},
    onControllerVisibilityChanged: (Boolean) -> Unit = {},
    onSaveProgress: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showController by remember { mutableStateOf(true) }
    
    // Auto-hide controller
    LaunchedEffect(showController, isPlaying) {
        if (showController && isPlaying) {
            delay(4000)
            showController = false
        }
    }

    LaunchedEffect(showController) {
        onControllerVisibilityChanged(showController)
    }
    
    val exoPlayer = remember {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(50000, 120000, 5000, 10000).build()
            
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context)
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
            setAudioAttributes(audioAttributes, true)
            volume = 1f 
            playWhenReady = true
        }
    }

    // Update progress periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    LaunchedEffect(videoUrl, headers) {
        val userAgent = headers["User-Agent"] ?: "Mozilla/5.0"
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(headers)
        val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key.nativeKeyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            showController = true
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .focusable()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Disable native controller
                    isFocusable = false
                    isFocusableInTouchMode = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showController,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f),
                                Color.Black
                            )
                        )
                    )
            ) {
                // Top Left Title
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(48.dp)
                ) {
                    Text(
                        text = "AniCli TV Player",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                // Right side vertical icons
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White.copy(alpha = 0.2f)), onClick = onShowQualitySelector) {
                        Icon(Icons.Filled.List, contentDescription = "Subtitles/Audio", tint = Color.White)
                    }
                    IconButton(colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White.copy(alpha = 0.2f)), onClick = { /* Volume */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Volume", tint = Color.White) // Reusing icon for volume visually
                    }
                    IconButton(colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White.copy(alpha = 0.2f)), onClick = { /* Settings */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 64.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        
                        // Custom Scrubber
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha=0.3f))
                        ) {
                            val progress = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceAtLeast(0.001f))
                                    .background(Color(0xFFE50914), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                        }

                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Playback Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val focusRequester = remember { FocusRequester() }
                        
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }

                        IconButton(
colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White, focusedContentColor = Color.Black, contentColor = Color.White),
onClick = { exoPlayer.seekTo(exoPlayer.currentPosition - 10000) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rewind 10s", tint = Color.White)
                        }
                        
                        IconButton(
colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White, focusedContentColor = Color.Black, contentColor = Color.White),
onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                isPlaying = !isPlaying
                            },
                            modifier = Modifier
                                .size(72.dp)
                                
                                .focusRequester(focusRequester)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow, 
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
colors = androidx.tv.material3.IconButtonDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White, focusedContentColor = Color.Black, contentColor = Color.White),
onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10000) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Forward 10s", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

enum class PlayerState {
    IDLE, BUFFERING, READY, ENDED
}
