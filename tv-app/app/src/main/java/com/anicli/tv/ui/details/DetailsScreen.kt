package com.anicli.tv.ui.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: DetailsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsState()
    var selectedChunkIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroNamiColors.Obsidian)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Loading anime details...",
                    style = MaterialTheme.typography.titleMedium,
                    color = KuroNamiColors.Cyan
                )
            }
        } else if (state.detail != null) {
            val detail = state.detail!!

            // Cinematic Backdrop Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(detail.backdropUrl.takeIf { it.isNotBlank() } ?: detail.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp),
                contentScale = ContentScale.Crop
            )

            // Dynamic Gradients
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KuroNamiColors.Obsidian.copy(alpha = 0.5f),
                                KuroNamiColors.Obsidian
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                KuroNamiColors.Obsidian.copy(alpha = 0.95f),
                                KuroNamiColors.Obsidian.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )

            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 160.dp, bottom = 48.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .fillMaxWidth(0.70f)
                    ) {
                        // Title
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Genre Tag Row & Dual Audio Badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(KuroNamiColors.CyanContainer, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "DUAL AUDIO [SUB & DUB]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KuroNamiColors.Cyan,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            detail.genres.take(4).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .background(KuroNamiColors.SurfaceHigh, RoundedCornerShape(6.dp))
                                        .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KuroNamiColors.CyanSoft,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Synopsis
                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KuroNamiColors.OnSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: Resume / Play Ep 1 / Watchlist
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val firstEp = detail.episodes.firstOrNull()
                            val watchProg = state.watchProgress

                            if (watchProg != null && watchProg.episodeId.isNotBlank()) {
                                Button(
                                    onClick = { onPlayClick(watchProg.episodeId) },
                                    colors = ButtonDefaults.colors(
                                        containerColor = KuroNamiColors.Crimson,
                                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                                    ),
                                    shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Resume (${watchProg.episodeTitle.ifBlank { "Next" }})",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (firstEp != null && firstEp.id != watchProg.episodeId) {
                                    Button(
                                        onClick = { onPlayClick(firstEp.id) },
                                        colors = ButtonDefaults.colors(
                                            containerColor = KuroNamiColors.SurfaceHigh,
                                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                                        ),
                                        shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                                    ) {
                                        Text(
                                            text = "Play Episode 1",
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onPlayClick(firstEp?.id ?: "") },
                                    colors = ButtonDefaults.colors(
                                        containerColor = KuroNamiColors.Crimson,
                                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                                    ),
                                    shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (firstEp != null) "Play Episode 1" else "Play",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.toggleFavorite(mediaId) },
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.SurfaceHighest
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(10.dp))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        if (state.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (state.isFavorite) KuroNamiColors.Crimson else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (state.isFavorite) "Watchlisted" else "Add to Watchlist",
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Episode List Header & Range Tabs
                item {
                    val episodeChunks = detail.episodes.chunked(25)
                    Column(modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = 36.dp, bottom = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .background(KuroNamiColors.Crimson, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Episode Queue (${detail.episodes.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Audio Switcher Pill
                            Row(
                                modifier = Modifier
                                    .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(8.dp))
                                    .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(KuroNamiColors.CyanContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "SUB (JA / EN)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KuroNamiColors.Cyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(KuroNamiColors.SurfaceHigh, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "DUB (EN)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KuroNamiColors.OnSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Range Pagination Tabs if episodes > 25
                        if (episodeChunks.size > 1) {
                            TvLazyRow(
                                modifier = Modifier.padding(top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(episodeChunks) { index, chunk ->
                                    val startEp = index * 25 + 1
                                    val endEp = (index * 25) + chunk.size
                                    val isSelected = selectedChunkIndex == index
                                    var isFocused by remember { mutableStateOf(false) }

                                    Surface(
                                        onClick = { selectedChunkIndex = index },
                                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                        colors = ClickableSurfaceDefaults.colors(
                                            containerColor = if (isSelected) KuroNamiColors.Crimson else KuroNamiColors.SurfaceHigh,
                                            contentColor = Color.White,
                                            focusedContainerColor = KuroNamiColors.CrimsonContainer,
                                            focusedContentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .border(
                                                width = 1.dp,
                                                color = if (isFocused) KuroNamiColors.Cyan else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "$startEp - $endEp",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val episodeChunks = detail.episodes.chunked(25)
                    val activeEpisodes = episodeChunks.getOrElse(selectedChunkIndex) { detail.episodes }

                    TvLazyRow(
                        modifier = Modifier.padding(horizontal = 40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(activeEpisodes) { episode ->
                            val isWatched = state.watchProgress?.episodeId == episode.id
                            EpisodeCard(
                                episode = episode,
                                fallbackThumbnail = detail.backdropUrl.takeIf { it.isNotBlank() } ?: detail.posterUrl,
                                isWatched = isWatched,
                                onClick = { onPlayClick(episode.id) }
                            )
                        }
                    }
                }
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Error: ${state.error}",
                    color = KuroNamiColors.Crimson,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: Episode,
    fallbackThumbnail: String,
    isWatched: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = KuroNamiColors.SurfaceLow,
            focusedContainerColor = KuroNamiColors.SurfaceHigh
        ),
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        modifier = Modifier
            .width(220.dp)
            .padding(6.dp)
            .aspectRatio(16f / 10f)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) KuroNamiColors.Crimson else KuroNamiColors.Outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.thumbnail?.takeIf { it.isNotBlank() } ?: fallbackThumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Multi-stop darkening gradient for high text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Play indicator icon in center on focus
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KuroNamiColors.Crimson)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Episode Label Bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "EPISODE ${episode.number}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KuroNamiColors.Cyan,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Watched indicator bar at bottom
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .background(KuroNamiColors.Crimson)
                )
            }
        }
    }
}
