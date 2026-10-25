package com.anicli.tv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.*
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.player.VideoPlayer

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    viewModel: PlayerViewModel = hiltViewModel(),
    onNextEpisode: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    var showQualitySelector by remember { mutableStateOf(false) }

    BackHandler {
        if (showQualitySelector) {
            showQualitySelector = false
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
    val nextEpId = "$mediaId|${epNum + 1}"

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
                        "Episode $epNum",
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
                            onShowQualitySelector = {
                                showQualitySelector = true
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

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(420.dp)
                                    .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(16.dp))
                                    .border(1.5.dp, KuroNamiColors.Crimson, RoundedCornerShape(16.dp))
                                    .padding(32.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Select Audio & Streaming Source",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Direct CDN streams are faster with zero transcoding lag",
                                        color = KuroNamiColors.OnSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(bottom = 20.dp)
                                    )

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
