package com.anicli.tv.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
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
    isOverlayDialogOpen: Boolean = false,
    initialPositionMs: Long = 0L,
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

    // Remote D-Pad Navigation Requesters
    // Top Row
    val audioBtnRequester = remember { FocusRequester() }
    val ccBtnRequester = remember { FocusRequester() }
    val qualityBtnRequester = remember { FocusRequester() }
    val sourcesBtnRequester = remember { FocusRequester() }

    // Center Controls
    val rewindBtnRequester = remember { FocusRequester() }
    val playPauseRequester = remember { FocusRequester() }
    val forwardBtnRequester = remember { FocusRequester() }

    // Bottom Controls
    val skipIntroRequester = remember { FocusRequester() }
    val nextEpRequester = remember { FocusRequester() }
    val episodesDrawerRequester = remember { FocusRequester() }
    val volumeBtnRequester = remember { FocusRequester() }

    // State for user-selectable video quality and captions
    var selectedQuality by remember { mutableStateOf("Auto") }
    var showQualityDialog by remember { mutableStateOf(false) }
    var captionsEnabled by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Ensure device audio stream is unmuted and at healthy volume on startup
    LaunchedEffect(Unit) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                if (!am.isVolumeFixed) {
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (curVol <= (maxVol * 0.3f)) {
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVol * 0.85f).toInt().coerceAtLeast(1), 0)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Auto-hide controller after 10 seconds of active playback (paused when dialogs or drawers are open)
    LaunchedEffect(showController, isPlaying, showQualityDialog, isOverlayDialogOpen) {
        if (showController && isPlaying && !showQualityDialog && !isOverlayDialogOpen) {
            delay(10000)
            showController = false
        }
    }

    LaunchedEffect(showController) {
        onControllerVisibilityChanged(showController)
    }

    // Initial focus on Play/Pause when screen first appears
    LaunchedEffect(Unit) {
        delay(120)
        try { playPauseRequester.requestFocus() } catch (_: Exception) {}
    }

    // Dismiss controller on Back press when visible and no subdialog is open
    BackHandler(enabled = showController && !showQualityDialog) {
        showController = false
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setForceLowestBitrate(false)
                    .setAllowMultipleAdaptiveSelections(true)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setSelectUndeterminedTextLanguage(true)
            )
        }
    }

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)
            .setEnableAudioFloatOutput(false) // 16-bit PCM output avoids silence on Android TV DACs

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .build().apply {
                // Must be false on Android TV so Compose focus navigation never silences or ducks audio!
                setAudioAttributes(audioAttributes, false)
                volume = 1.0f
                playWhenReady = true
            }
    }

    val availableQualities = remember { listOf("Auto", "1080p", "720p", "480p", "360p") }

    fun applyQuality(quality: String) {
        selectedQuality = quality
        val paramsBuilder = trackSelector.buildUponParameters()
        if (quality == "Auto") {
            paramsBuilder.clearVideoSizeConstraints()
            paramsBuilder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            val targetHeight = when (quality) {
                "1080p" -> 1080
                "720p" -> 720
                "480p" -> 480
                "360p" -> 360
                else -> 1080
            }
            var appliedOverride = false
            for (group in exoPlayer.currentTracks.groups) {
                if (group.type == C.TRACK_TYPE_VIDEO) {
                    val mediaTrackGroup = group.mediaTrackGroup
                    for (i in 0 until mediaTrackGroup.length) {
                        val format = mediaTrackGroup.getFormat(i)
                        if (kotlin.math.abs(format.height - targetHeight) <= 30) {
                            paramsBuilder.setOverrideForType(
                                TrackSelectionOverride(mediaTrackGroup, listOf(i))
                            )
                            appliedOverride = true
                            break
                        }
                    }
                }
                if (appliedOverride) break
            }
            if (!appliedOverride) {
                paramsBuilder.setMaxVideoSize(Int.MAX_VALUE, targetHeight)
            }
        }
        trackSelector.setParameters(paramsBuilder)
    }

    fun toggleCaptions() {
        val newState = !captionsEnabled
        captionsEnabled = newState
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !newState)
        )
    }

    fun toggleMute() {
        val newMuted = !isMuted
        isMuted = newMuted
        exoPlayer.volume = if (newMuted) 0f else 1.0f
    }

    // Update progress ticker and save to SQLite/Cloud
    LaunchedEffect(showController) {
        if (showController) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
        }
    }

    LaunchedEffect(exoPlayer) {
        var tickCount = 0
        while (true) {
            if (exoPlayer.isPlaying) {
                if (showController) {
                    currentPosition = exoPlayer.currentPosition
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    bufferedPosition = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                }
                tickCount++
                if (tickCount >= 8) {
                    tickCount = 0
                    onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(0L))
                }
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
        if (initialPositionMs > 0L) {
            exoPlayer.seekTo(initialPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.volume = 1.0f
        isMuted = false
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    exoPlayer.volume = if (isMuted) 0f else 1.0f
                } else if (playbackState == Player.STATE_ENDED) {
                    onNextEpisode()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                // Ensure active audio track selection across dual-audio and single-track streams
                val hasSelectedAudio = tracks.groups.any { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
                if (!hasSelectedAudio) {
                    for (group in tracks.groups) {
                        if (group.type == C.TRACK_TYPE_AUDIO && group.mediaTrackGroup.length > 0) {
                            val params = trackSelector.buildUponParameters()
                                .setOverrideForType(
                                    TrackSelectionOverride(group.mediaTrackGroup, listOf(0))
                                )
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .build()
                            trackSelector.setParameters(params)
                            break
                        }
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("AniCliPlayer", "Playback error: ${error.errorCodeName} - ${error.message}", error)
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
                    subtitleView?.visibility = if (captionsEnabled) android.view.View.VISIBLE else android.view.View.GONE
                }
            },
            update = { playerView ->
                playerView.subtitleView?.visibility = if (captionsEnabled) android.view.View.VISIBLE else android.view.View.GONE
            },
            modifier = Modifier.fillMaxSize()
        )

        // Backdrop listener when controller is hidden: any directional or select key brings back HUD
        if (!showController) {
            val invisibleFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                try { invisibleFocusRequester.requestFocus() } catch (_: Exception) {}
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(invisibleFocusRequester)
                    .focusable()
                    .clickable {
                        showController = true
                        try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                    }
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            // Let Back key pass through to exit player cleanly
                            if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                return@onKeyEvent false
                            }
                            showController = true
                            try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                            return@onKeyEvent true
                        }
                        false
                    }
            )
        }

        // TV Remote Navigable Player HUD
        AnimatedVisibility(
            visible = showController,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(160)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(160)),
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
                        // Breadcrumb Trail
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

                    // Top Right Action Buttons (Inter-connected D-pad Navigation)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Audio Mode Toggle (SUB / DUB)
                        Button(
                            onClick = onToggleAudioMode,
                            colors = ButtonDefaults.colors(
                                containerColor = if (audioMode == "sub") KuroNamiColors.CyanContainer else KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .focusRequester(audioBtnRequester)
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                                onToggleAudioMode()
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                try { ccBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                        ) {
                            Text(
                                text = if (audioMode == "sub") "AUDIO: SUB (JA)" else "AUDIO: DUB (EN)",
                                fontWeight = FontWeight.Bold,
                                color = if (audioMode == "sub") KuroNamiColors.Cyan else Color.White
                            )
                        }

                        // 2. Closed Captions Toggle
                        Button(
                            onClick = { toggleCaptions() },
                            colors = ButtonDefaults.colors(
                                containerColor = if (captionsEnabled) KuroNamiColors.CyanContainer else KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .focusRequester(ccBtnRequester)
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                                toggleCaptions()
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                try { audioBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (captionsEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                                    contentDescription = "Toggle Captions",
                                    tint = if (captionsEnabled) KuroNamiColors.Cyan else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (captionsEnabled) "CC: ON" else "CC: OFF",
                                    fontWeight = FontWeight.Bold,
                                    color = if (captionsEnabled) KuroNamiColors.Cyan else Color.White
                                )
                            }
                        }

                        // 3. Video Quality Selector Dialog Trigger
                        Button(
                            onClick = { showQualityDialog = true },
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .focusRequester(qualityBtnRequester)
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                                showQualityDialog = true
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                try { ccBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                try { sourcesBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.HighQuality,
                                    contentDescription = "Video Quality",
                                    tint = KuroNamiColors.CyanSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedQuality == "Auto") "Auto HD" else selectedQuality,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KuroNamiColors.CyanSoft,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 4. Sources / More Button Trigger
                        Button(
                            onClick = onShowQualitySelector,
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .focusRequester(sourcesBtnRequester)
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                                onShowQualitySelector()
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                try { forwardBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                        ) {
                            Text("Sources", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Center Quick Playback Controls (D-Pad Graph) ────────────
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                        colors = IconButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh.copy(alpha = 0.7f),
                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                        ),
                        modifier = Modifier
                            .size(60.dp)
                            .focusRequester(rewindBtnRequester)
                            .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), CircleShape)
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER -> {
                                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { audioBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            try { skipIntroRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
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
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        colors = IconButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer
                        ),
                        modifier = Modifier
                            .size(80.dp)
                            .focusRequester(playPauseRequester)
                            .border(2.5.dp, KuroNamiColors.CyanSoft.copy(alpha = 0.7f), CircleShape)
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER -> {
                                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try { rewindBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            try { forwardBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            try { skipIntroRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
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
                            .focusRequester(forwardBtnRequester)
                            .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), CircleShape)
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER -> {
                                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration))
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { sourcesBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            try { nextEpRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                    }
                                }
                                false
                            }
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

                    // Action Shortcuts (Continuous 4-way D-Pad Flow)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // 1. Skip Intro (+85s)
                            Button(
                                onClick = {
                                    val target = (exoPlayer.currentPosition + 85000).coerceAtMost(duration)
                                    exoPlayer.seekTo(target)
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                modifier = Modifier
                                    .focusRequester(skipIntroRequester)
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                                android.view.KeyEvent.KEYCODE_ENTER -> {
                                                    val target = (exoPlayer.currentPosition + 85000).coerceAtMost(duration)
                                                    exoPlayer.seekTo(target)
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    try { nextEpRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                            }
                                        }
                                        false
                                    }
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

                            // 2. Next Episode
                            Button(
                                onClick = onNextEpisode,
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                modifier = Modifier
                                    .focusRequester(nextEpRequester)
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                                android.view.KeyEvent.KEYCODE_ENTER -> {
                                                    onNextEpisode()
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    try { skipIntroRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    try { episodesDrawerRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                            }
                                        }
                                        false
                                    }
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

                            // 3. Episode Drawer Toggle Button
                            Button(
                                onClick = onShowEpisodeList,
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                modifier = Modifier
                                    .focusRequester(episodesDrawerRequester)
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                                android.view.KeyEvent.KEYCODE_ENTER -> {
                                                    onShowEpisodeList()
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                    try { nextEpRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    try { volumeBtnRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                    try { playPauseRequester.requestFocus() } catch (_: Exception) {}
                                                    return@onKeyEvent true
                                                }
                                            }
                                        }
                                        false
                                    }
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

                        // 4. Volume / Unmute Quick Toggle
                        Button(
                            onClick = { toggleMute() },
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.Crimson
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .focusRequester(volumeBtnRequester)
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                                toggleMute()
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                try { episodesDrawerRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { forwardBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                    }
                                    false
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (!isMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Audio Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (!isMuted) "Audio: 100%" else "MUTED", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Glassmorphic Quality Selector Dialog (100% TV Remote Navigable)
        if (showQualityDialog) {
            BackHandler {
                showQualityDialog = false
                try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
            }

            val qualityDialogInitialRequester = remember { FocusRequester() }
            LaunchedEffect(showQualityDialog) {
                delay(80)
                try { qualityDialogInitialRequester.requestFocus() } catch (_: Exception) {}
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showQualityDialog = false
                        try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(460.dp)
                        .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(16.dp))
                        .border(1.5.dp, KuroNamiColors.CyanSoft.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {} // consume touch/click
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Stream Quality",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select preferred resolution for direct HLS stream",
                            style = MaterialTheme.typography.bodySmall,
                            color = KuroNamiColors.OnSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 4.dp, bottom = 18.dp)
                        )

                        availableQualities.forEachIndexed { qIdx, q ->
                            val isCurrent = selectedQuality == q
                            val isFirst = qIdx == 0
                            Button(
                                onClick = {
                                    applyQuality(q)
                                    showQualityDialog = false
                                    try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (isCurrent) KuroNamiColors.Crimson else KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.CrimsonContainer
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .then(
                                        if (isCurrent || (isFirst && !availableQualities.contains(selectedQuality))) {
                                            Modifier.focusRequester(qualityDialogInitialRequester)
                                        } else Modifier
                                    )
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                                event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                            ) {
                                                applyQuality(q)
                                                showQualityDialog = false
                                                try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                                return@onKeyEvent true
                                            }
                                        }
                                        false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (q == "Auto") "Auto (Adaptive Bitrate)" else q,
                                        color = Color.White,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCurrent) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KuroNamiColors.CyanSoft,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showQualityDialog = false
                                try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceHigh,
                                focusedContainerColor = KuroNamiColors.SurfaceHighest
                            ),
                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                        if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                            event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                        ) {
                                            showQualityDialog = false
                                            try { qualityBtnRequester.requestFocus() } catch (_: Exception) {}
                                            return@onKeyEvent true
                                        }
                                    }
                                    false
                                }
                        ) {
                            Text("Close Quality Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0L)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
