package com.anicli.tv.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.model.Subtitle
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    videoUrl: String,
    title: String = "Anime Stream",
    episodeTitle: String = "Now Playing",
    serverName: String = "CDN HD",
    headers: Map<String, String> = emptyMap(),
    subtitles: List<Subtitle> = emptyList(),
    okHttpClient: OkHttpClient,
    modifier: Modifier = Modifier,
    audioMode: String = "sub",
    onToggleAudioMode: () -> Unit = {},
    onShowQualitySelector: () -> Unit = {},
    onShowEpisodeList: () -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onControllerVisibilityChanged: (Boolean) -> Unit = {},
    onSaveProgress: (Long, Long) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var showController by remember { mutableStateOf(true) }
    val rootFocusRequester = remember { FocusRequester() }

    // Ensure device audio stream is unmuted and audible on startup
    LaunchedEffect(Unit) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (curVol < maxVol * 0.75) {
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt().coerceAtLeast(1), 0)
                }
            }
        } catch (_: Exception) {}
    }

    // Auto-hide controller after 6.5 seconds of playback
    LaunchedEffect(showController, isPlaying) {
        if (showController && isPlaying) {
            delay(6500)
            showController = false
        }
    }

    LaunchedEffect(showController) {
        onControllerVisibilityChanged(showController)
        if (!showController) {
            delay(100)
            try {
                rootFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableAudioTrackPlaybackParams(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(40000, 100000, 2500, 5000)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceLowestBitrate(false)
                    .setAllowMultipleAdaptiveSelections(true)
            )
        }

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                // handleAudioFocus=false avoids silent muting if emulator doesn't grant focus
                setAudioAttributes(audioAttributes, false)
                volume = 1.0f
                playWhenReady = true
            }
    }

    // Update progress ticker
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
                bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                onSaveProgress(currentPosition, duration)
            }
            delay(1000)
        }
    }

    // Load Stream Source & Subtitles
    LaunchedEffect(videoUrl, headers, subtitles) {
        val userAgent = headers["User-Agent"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(headers)

        val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(videoUrl))
        if (!videoUrl.contains(".mp4") && !videoUrl.contains("mp4upload")) {
            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }

        // Attach VTT subtitles if available
        if (subtitles.isNotEmpty()) {
            val subConfigs = subtitles.filter { it.url.isNotBlank() }.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                    .setMimeType(androidx.media3.common.MimeTypes.TEXT_VTT)
                    .setLanguage(sub.language)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            if (subConfigs.isNotEmpty()) {
                mediaItemBuilder.setSubtitleConfigurations(subConfigs)
            }
        }

        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(mediaItemBuilder.build())

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.volume = 1.0f
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    exoPlayer.volume = 1.0f
                } else if (playbackState == Player.STATE_ENDED) {
                    onNextEpisode()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            if (currentPosition > 0 && duration > 0) {
                onSaveProgress(currentPosition, duration)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (!showController && (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp)) {
                    when (event.key.nativeKeyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY,
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            showController = true
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    setOnClickListener {
                        showController = !showController
                    }
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!showController) {
            val invisibleFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                invisibleFocusRequester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(invisibleFocusRequester)
                    .focusable()
                    .clickable { showController = true }
                    .onKeyEvent {
                        showController = true
                        true
                    }
            )
        }

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
                                KuroNamiColors.Obsidian.copy(alpha = 0.92f),
                                Color.Transparent,
                                Color.Transparent,
                                KuroNamiColors.Obsidian.copy(alpha = 0.85f),
                                KuroNamiColors.Obsidian.copy(alpha = 0.98f)
                            )
                        )
                    )
            ) {
                // ── Top Header ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 48.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Web Breadcrumb Trail (Home / Anime / Title / Episode)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Home",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.OnSurfaceVariant
                            )
                            Text(text = " / ", style = MaterialTheme.typography.labelSmall, color = KuroNamiColors.Outline)
                            Text(
                                text = "Anime",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.OnSurfaceVariant
                            )
                            Text(text = " / ", style = MaterialTheme.typography.labelSmall, color = KuroNamiColors.Outline)
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(text = " / ", style = MaterialTheme.typography.labelSmall, color = KuroNamiColors.Outline)
                            Text(
                                text = episodeTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.Cyan,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(KuroNamiColors.Crimson, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRECISION STREAM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = serverName,
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = KuroNamiColors.OnSurfaceVariant
                        )
                    }

                    // Top Right Badges & Audio Switcher
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick SUB / DUB Toggle Pill
                        Button(
                            onClick = onToggleAudioMode,
                            colors = ButtonDefaults.colors(
                                containerColor = if (audioMode == "sub") KuroNamiColors.CyanContainer else KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (audioMode == "sub") "AUDIO: SUB (JA)" else "AUDIO: DUB (EN)",
                                fontWeight = FontWeight.Bold,
                                color = if (audioMode == "sub") KuroNamiColors.Cyan else Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.SurfaceHigh, RoundedCornerShape(8.dp))
                                .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "1080p HD",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.CyanSoft,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onShowQualitySelector,
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                        ) {
                            Text("Sources", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Center Quick Playback Controls ──────────────────────────
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playPauseRequester = remember { FocusRequester() }

                    LaunchedEffect(showController) {
                        if (showController) {
                            delay(100)
                            playPauseRequester.requestFocus()
                        }
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        colors = IconButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh.copy(alpha = 0.7f),
                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                        ),
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause Main Glow Pill
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isPlaying = !isPlaying
                        },
                        colors = IconButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer
                        ),
                        modifier = Modifier
                            .size(80.dp)
                            .focusRequester(playPauseRequester)
                            .border(2.5.dp, KuroNamiColors.CyanSoft.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration)) },
                        colors = IconButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh.copy(alpha = 0.7f),
                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                        ),
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // ── Bottom Scrubber & Action Bar ────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 32.dp)
                ) {
                    // Progress Bar & Time Stamps
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        // Multi-layer Scrubber: Background, Buffer, Played
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(KuroNamiColors.SurfaceHighest)
                        ) {
                            val bufferRatio = if (duration > 0) (bufferedPosition.toFloat() / duration.toFloat()) else 0f
                            val progressRatio = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f

                            // Buffered bar
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(bufferRatio.coerceIn(0f, 1f))
                                    .background(KuroNamiColors.Outline.copy(alpha = 0.5f))
                            )

                            // Played bar
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressRatio.coerceIn(0.001f, 1f))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(KuroNamiColors.Crimson, KuroNamiColors.CrimsonContainer)
                                        )
                                    )
                            )
                        }

                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.titleMedium,
                            color = KuroNamiColors.OnSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Action Shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Skip Intro
                            Button(
                                onClick = {
                                    val target = (exoPlayer.currentPosition + 85000).coerceAtMost(duration)
                                    exoPlayer.seekTo(target)
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.FastForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Skip Intro (+85s)", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Next Episode
                            Button(
                                onClick = onNextEpisode,
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.SkipNext,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Next Episode", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Episode Drawer Toggle Button
                            Button(
                                onClick = onShowEpisodeList,
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Episodes", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Volume / Unmute Quick Toggle
                        Button(
                            onClick = {
                                val newVol = if (exoPlayer.volume > 0f) 0f else 1.0f
                                exoPlayer.volume = newVol
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (exoPlayer.volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Audio Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (exoPlayer.volume > 0f) "Audio: ON (100%)" else "Audio: MUTED", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
