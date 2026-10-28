package com.anicli.tv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.*
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.player.VideoPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    viewModel: PlayerViewModel = hiltViewModel(),
    onNextEpisode: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val audioMode by viewModel.audioMode.collectAsState()

    var showQualitySelector by remember { mutableStateOf(false) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }

    BackHandler {
        if (showQualitySelector) {
            showQualitySelector = false
        } else if (showEpisodeDrawer) {
            showEpisodeDrawer = false
        } else {
            onBack()
        }
    }

    LaunchedEffect(episodeId) {
        viewModel.loadStreamSources(episodeId)
    }

    val parts = episodeId.split("|")
    val mediaId = parts.getOrNull(0) ?: ""
    val epNum = if (parts.size >= 3) {
        parts[2].toIntOrNull() ?: 1
    } else {
        parts.getOrNull(1)?.toIntOrNull() ?: 1
    }
    val nextEp = viewModel.cachedEpisodes.find { it.number == epNum + 1 }
    val nextEpId = nextEp?.id ?: "$mediaId|${epNum + 1}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroNamiColors.Obsidian),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Connecting to Cloud CDN Stream...",
                        color = KuroNamiColors.Cyan,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Episode $epNum  •  Audio: ${audioMode.uppercase()}",
                        color = KuroNamiColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is PlayerUiState.Success -> {
                var selectedSourceIndex by remember { mutableIntStateOf(0) }
                var isControllerVisible by remember { mutableStateOf(false) }

                val source = state.sources.getOrNull(selectedSourceIndex)

                Box(modifier = Modifier.fillMaxSize()) {
                    if (source != null) {
                        VideoPlayer(
                            videoUrl = source.url,
                            title = viewModel.cachedAnimeTitle.ifBlank { "Anime Stream" },
                            episodeTitle = "Episode $epNum",
                            serverName = source.title.ifBlank { "Cloud CDN HD" },
                            headers = source.headers ?: emptyMap(),
                            subtitles = source.subtitles,
                            okHttpClient = viewModel.okHttpClient,
                            audioMode = audioMode,
                            isOverlayDialogOpen = showQualitySelector || showEpisodeDrawer,
                            onToggleAudioMode = {
                                viewModel.toggleAudioMode()
                            },
                            onShowQualitySelector = {
                                showQualitySelector = true
                            },
                            onShowEpisodeList = {
                                showEpisodeDrawer = true
                            },
                            onNextEpisode = {
                                onNextEpisode(nextEpId)
                            },
                            onControllerVisibilityChanged = { isVisible ->
                                isControllerVisible = isVisible
                            },
                            onSaveProgress = { pos, dur ->
                                viewModel.saveProgress(mediaId, epNum, pos, dur)
                            }
                        )
                    } else {
                        Text(
                            "No playable stream found",
                            color = KuroNamiColors.Crimson,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Sleek Obsidian Source & Audio Selection Dialog
                    if (showQualitySelector) {
                        BackHandler { showQualitySelector = false }

                        val sourcesDialogInitialRequester = remember { FocusRequester() }
                        LaunchedEffect(showQualitySelector) {
                            delay(80)
                            try { sourcesDialogInitialRequester.requestFocus() } catch (_: Exception) {}
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showQualitySelector = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(460.dp)
                                    .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(16.dp))
                                    .border(1.5.dp, KuroNamiColors.Crimson, RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {} // consume touch/click
                                    .padding(32.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Audio & Streaming Sources",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Direct Edge HLS streams with synchronized dual audio",
                                        color = KuroNamiColors.OnSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )

                                    // Audio Mode Quick Pill Selector
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.setAudioMode("sub") },
                                            colors = ButtonDefaults.colors(
                                                containerColor = if (audioMode == "sub") KuroNamiColors.CyanContainer else KuroNamiColors.SurfaceHigh,
                                                focusedContainerColor = KuroNamiColors.Crimson
                                            ),
                                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(sourcesDialogInitialRequester)
                                        ) {
                                            Text(
                                                "SUB (JA / EN)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (audioMode == "sub") KuroNamiColors.Cyan else Color.White
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.setAudioMode("dub") },
                                            colors = ButtonDefaults.colors(
                                                containerColor = if (audioMode == "dub") KuroNamiColors.CyanContainer else KuroNamiColors.SurfaceHigh,
                                                focusedContainerColor = KuroNamiColors.Crimson
                                            ),
                                            shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                "DUB (EN)",
                                                fontWeight = FontWeight.Bold,
                                                color = if (audioMode == "dub") KuroNamiColors.Cyan else Color.White
                                            )
                                        }
                                    }

                                    TvLazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        itemsIndexed(state.sources) { index, src ->
                                            val isSelected = selectedSourceIndex == index
                                            Button(
                                                onClick = {
                                                    selectedSourceIndex = index
                                                    showQualitySelector = false
                                                },
                                                colors = ButtonDefaults.colors(
                                                    containerColor = if (isSelected)
                                                        KuroNamiColors.Crimson else KuroNamiColors.SurfaceHigh,
                                                    focusedContainerColor = KuroNamiColors.CrimsonContainer
                                                ),
                                                shape = ButtonDefaults.shape(RoundedCornerShape(10.dp)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = src.title,
                                                        color = Color.White,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = if (isSelected) "ACTIVE" else src.quality,
                                                        color = if (isSelected) KuroNamiColors.CyanSoft else KuroNamiColors.OnSurfaceVariant,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = { showQualitySelector = false },
                                        colors = ButtonDefaults.colors(
                                            containerColor = KuroNamiColors.SurfaceHigh,
                                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                                        ),
                                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                                    ) {
                                        Text("Close", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // In-Player Episode Queue Drawer Dialog
                    if (showEpisodeDrawer) {
                        BackHandler { showEpisodeDrawer = false }

                        val episodeDrawerInitialRequester = remember { FocusRequester() }
                        LaunchedEffect(showEpisodeDrawer) {
                            delay(80)
                            try { episodeDrawerInitialRequester.requestFocus() } catch (_: Exception) {}
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showEpisodeDrawer = false },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(380.dp)
                                    .background(KuroNamiColors.SurfaceLow)
                                    .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {} // consume touch/click
                                    .padding(24.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Episode Queue",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${viewModel.cachedEpisodes.size} EP",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KuroNamiColors.Cyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    TvLazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(viewModel.cachedEpisodes) { ep ->
                                            val isCurrentEp = ep.number == epNum
                                            Button(
                                                onClick = {
                                                    showEpisodeDrawer = false
                                                    onNextEpisode(ep.id)
                                                },
                                                colors = ButtonDefaults.colors(
                                                    containerColor = if (isCurrentEp) KuroNamiColors.Crimson else KuroNamiColors.SurfaceHigh,
                                                    focusedContainerColor = KuroNamiColors.CrimsonContainer
                                                ),
                                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (isCurrentEp) Modifier.focusRequester(episodeDrawerInitialRequester) else Modifier
                                                    )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "EP ${ep.number}: ${ep.title}",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isCurrentEp) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (isCurrentEp) {
                                                        Text(
                                                            text = "PLAYING",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = KuroNamiColors.CyanSoft,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            modifier = Modifier.padding(start = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showEpisodeDrawer = false },
                                        colors = ButtonDefaults.colors(
                                            containerColor = KuroNamiColors.SurfaceHigh,
                                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                                        ),
                                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Close Queue", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is PlayerUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        color = KuroNamiColors.Crimson,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = KuroNamiColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
