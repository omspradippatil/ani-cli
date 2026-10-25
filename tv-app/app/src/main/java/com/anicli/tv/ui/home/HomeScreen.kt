package com.anicli.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
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
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anicli.tv.core.presentation.MediaCard
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedDrawerItem by remember { mutableStateOf("Home") }
    var focusedItem by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    LaunchedEffect(state.rows) {
        if (focusedItem == null && state.rows.isNotEmpty()) {
            val trendingRow = state.rows.firstOrNull { it.title.contains("Trending", ignoreCase = true) }
            val firstAvailable = trendingRow?.items?.firstOrNull() 
                ?: state.rows.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()
            focusedItem = firstAvailable
        }
    }

    NavigationDrawer(
        drawerContent = { drawerValue ->
            Sidebar(
                isExpanded = drawerValue == DrawerValue.Open,
                selectedItem = selectedDrawerItem,
                onItemSelected = { selectedDrawerItem = it },
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KuroNamiColors.Obsidian)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Loading KuroNami Stream...",
                            style = MaterialTheme.typography.titleMedium,
                            color = KuroNamiColors.Cyan
                        )
                    }
                }
            } else {
                TvLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 72.dp),
                    contentPadding = PaddingValues(bottom = 48.dp, top = 16.dp)
                ) {
                    item {
                        HeroBanner(
                            item = focusedItem,
                            onClick = { focusedItem?.let { onMediaClick(it.id) } }
                        )
                    }

                    items(state.rows) { row ->
                        HomeRow(
                            row = row,
                            onMediaClick = onMediaClick,
                            onFocusItem = { focusedItem = it }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Sidebar(
    isExpanded: Boolean,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isExpanded) 240.dp else 80.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(KuroNamiColors.SurfaceLow, KuroNamiColors.Obsidian)
                )
            )
            .border(
                width = 1.dp,
                color = KuroNamiColors.Outline.copy(alpha = 0.2f)
            )
            .padding(16.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        // App Logo & Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KuroNamiColors.Crimson),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "KuroNami",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "PRECISION STREAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = KuroNamiColors.Cyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Items
        DrawerItem(
            icon = Icons.Filled.Search,
            text = "Search",
            isSelected = selectedItem == "Search",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Search"); onSearchClick() }
        )
        DrawerItem(
            icon = Icons.Filled.Home,
            text = "Home",
            isSelected = selectedItem == "Home",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Home") }
        )
        DrawerItem(
            icon = Icons.AutoMirrored.Filled.List,
            text = "History",
            isSelected = selectedItem == "History",
            isExpanded = isExpanded,
            onClick = { onItemSelected("History") }
        )

        Spacer(modifier = Modifier.weight(1f))

        DrawerItem(
            icon = Icons.Filled.Settings,
            text = "Settings",
            isSelected = selectedItem == "Settings",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Settings"); onSettingsClick() }
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) KuroNamiColors.Crimson else Color.Transparent,
            contentColor = if (isSelected) Color.White else KuroNamiColors.OnSurfaceVariant,
            focusedContainerColor = KuroNamiColors.CrimsonContainer,
            focusedContentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(item: MediaItem?, onClick: (String) -> Unit) {
    if (item == null) return
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 48.dp, vertical = 12.dp)
    ) {
        Card(
            onClick = { onClick(item.id) },
            colors = CardDefaults.colors(
                containerColor = KuroNamiColors.SurfaceLow,
                focusedContainerColor = KuroNamiColors.SurfaceHigh
            ),
            shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { isFocused = it.isFocused }
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) KuroNamiColors.Crimson else KuroNamiColors.Outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // High-End Multi-Stop Vignette Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    KuroNamiColors.Obsidian.copy(alpha = 0.95f),
                                    KuroNamiColors.Obsidian.copy(alpha = 0.85f),
                                    KuroNamiColors.Obsidian.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    KuroNamiColors.Obsidian.copy(alpha = 0.9f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(32.dp)
                        .fillMaxWidth(0.65f)
                ) {
                    // Quick Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.Crimson, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "FEATURED SPOTLIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (!item.rating.isNullOrBlank()) {
                            Text(
                                text = item.rating,
                                style = MaterialTheme.typography.labelMedium,
                                color = KuroNamiColors.Gold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!item.releaseYear.isNullOrBlank()) {
                            Text(
                                text = "•  ${item.releaseYear}",
                                style = MaterialTheme.typography.labelMedium,
                                color = KuroNamiColors.OnSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.description ?: "Stream anime in ultra-crisp HD with synced dual-audio playback on KuroNami.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuroNamiColors.OnSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isFocused) KuroNamiColors.Crimson else KuroNamiColors.SurfaceHighest,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Now",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeRow(row: MediaRow, onMediaClick: (String) -> Unit, onFocusItem: (MediaItem) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(KuroNamiColors.Crimson, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        TvLazyRow(
            modifier = Modifier.padding(horizontal = 40.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(row.items) { item ->
                MediaCard(
                    item = item,
                    onClick = { onMediaClick(item.id) },
                    onFocus = { onFocusItem(item) }
                )
            }
        }
    }
}
