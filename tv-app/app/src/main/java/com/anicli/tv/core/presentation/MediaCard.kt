package com.anicli.tv.core.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.model.MediaItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier.width(190.dp)
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = KuroNamiColors.SurfaceLow,
            focusedContainerColor = KuroNamiColors.SurfaceHigh
        ),
        shape = CardDefaults.shape(
            shape = RoundedCornerShape(12.dp)
        ),
        scale = CardDefaults.scale(
            focusedScale = 1.05f
        ),
        border = CardDefaults.border(
            border = Border(BorderStroke(1.dp, KuroNamiColors.Outline.copy(alpha = 0.25f))),
            focusedBorder = Border(BorderStroke(2.5.dp, KuroNamiColors.Crimson))
        ),
        modifier = modifier
            .padding(6.dp)
            .zIndex(if (isFocused) 5f else 1f)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KuroNamiColors.SurfaceLow)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(KuroNamiColors.SurfaceHigh, KuroNamiColors.Surface)
                        )
                    )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.posterUrl.takeIf { it.isNotBlank() } ?: item.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Bottom vignette gradient over image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, KuroNamiColors.SurfaceLow)
                            )
                        )
                )

                // Rating pill badge with gold star
                if (!item.rating.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 0.8.dp,
                                color = KuroNamiColors.Gold.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.Gold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 3.dp)
                            )
                            Text(
                                text = item.rating.replace("★", "").trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Progress Bar if watched
                if (item.progressPercent != null && item.progressPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = item.progressPercent.coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .background(KuroNamiColors.Crimson)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFocused) Color.White else KuroNamiColors.OnSurface,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    lineHeight = 18.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 38.dp)
                )

                val meta = listOfNotNull(item.releaseYear, item.tags.firstOrNull()).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = KuroNamiColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Widescreen 16:9 Card for Continue Watching
 * Displays episode thumbnail/backdrop, episode badge, play indicator on focus,
 * and a prominent crimson progress bar.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContinueWatchingCard(
    item: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(260.dp)
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        onLongClick = onRemove,
        colors = CardDefaults.colors(
            containerColor = KuroNamiColors.SurfaceLow,
            focusedContainerColor = KuroNamiColors.SurfaceHigh
        ),
        shape = CardDefaults.shape(
            shape = RoundedCornerShape(12.dp)
        ),
        scale = CardDefaults.scale(
            focusedScale = 1.05f
        ),
        border = CardDefaults.border(
            border = Border(BorderStroke(1.dp, KuroNamiColors.Outline.copy(alpha = 0.25f))),
            focusedBorder = Border(BorderStroke(2.5.dp, KuroNamiColors.Cyan))
        ),
        modifier = modifier
            .padding(6.dp)
            .zIndex(if (isFocused) 5f else 1f)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus()
                }
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && onRemove != null) {
                    if (event.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_MENU ||
                        event.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_DEL ||
                        event.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_FORWARD_DEL) {
                        onRemove()
                        return@onKeyEvent true
                    }
                }
                false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(KuroNamiColors.SurfaceLow)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9.5f)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(KuroNamiColors.SurfaceHigh)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.backdropUrl.takeIf { !it.isNullOrBlank() } ?: item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Darkening overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Episode Badge Top Left
                val episodeLabel = item.episodeLabel ?: "EP 1"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = KuroNamiColors.CyanContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = 0.8.dp,
                            color = KuroNamiColors.Cyan.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episodeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = KuroNamiColors.Cyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Top Right: Remove Badge Hint on Focus
                if (isFocused && onRemove != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = KuroNamiColors.Crimson.copy(alpha = 0.90f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 0.8.dp,
                                color = KuroNamiColors.Crimson,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "HOLD OK TO REMOVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Center Play Icon on Focus
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(KuroNamiColors.Crimson)
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Progress Bar
                val progress = item.progressPercent ?: 0.5f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.coerceIn(0.04f, 1f))
                            .fillMaxHeight()
                            .background(KuroNamiColors.Crimson)
                    )
                }
            }

            // Title & Subtitle Below Image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFocused) Color.White else KuroNamiColors.OnSurface,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val percentText = ((item.progressPercent ?: 0f) * 100).toInt()
                Text(
                    text = if (isFocused) {
                        if (percentText > 0) "$percentText% • Press OK to play • Hold OK to remove" else "Press OK to play • Hold OK to remove"
                    } else {
                        if (percentText > 0) "$percentText% watched • Press OK to resume" else "Press OK to resume"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) KuroNamiColors.CyanSoft else KuroNamiColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
