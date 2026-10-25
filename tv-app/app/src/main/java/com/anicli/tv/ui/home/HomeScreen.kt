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
                            text = "Loading Om CLI Precision Stream...",
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
                            isWatchlisted = state.favoriteMediaIds.contains(focusedItem?.id ?: ""),
                            onClick = { focusedItem?.let { onMediaClick(it.id) } },
                            onToggleWatchlist = { focusedItem?.let { viewModel.toggleWatchlist(it) } }
                        )
                    }

                    // Quick Category Presets Chips Bar (Matching Web /genre-bar)
                    item {
                        GenreChipsBar(
                            selectedGenre = state.selectedGenre,
                            onGenreSelect = { viewModel.selectGenre(it) }
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
            .width(if (isExpanded) 250.dp else 80.dp)
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
        // Om CLI Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KuroNamiColors.SurfaceHigh)
                    .border(1.dp, KuroNamiColors.Crimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(KuroNamiColors.Crimson)
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OM ",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "CLI",
                            style = MaterialTheme.typography.titleMedium,
                            color = KuroNamiColors.CrimsonContainer,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "PRECISION STREAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = KuroNamiColors.Cyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 100% FREE Badge
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .background(KuroNamiColors.Emerald.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                    .border(1.dp, KuroNamiColors.Emerald.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "100% FREE",
                    style = MaterialTheme.typography.labelSmall,
                    color = KuroNamiColors.Emerald,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

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
            icon = Icons.Filled.Bookmark,
            text = "My Watchlist",
            isSelected = selectedItem == "My Watchlist",
            isExpanded = isExpanded,
            onClick = { onItemSelected("My Watchlist") }
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
fun GenreChipsBar(
    selectedGenre: String,
    onGenreSelect: (String) -> Unit
) {
    val genres = listOf(
        "All" to "All Genres",
        "Action" to "Action",
        "Fantasy" to "Dark Fantasy",
        "Shounen" to "Shounen",
        "Sci-Fi" to "Cyberpunk / Sci-Fi",
        "Supernatural" to "Supernatural",
        "Drama" to "Drama",
        "Comedy" to "Comedy",
        "Romance" to "Romance"
    )

    TvLazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 48.dp)
    ) {
        items(genres) { (key, label) ->
            val isSelected = selectedGenre == key
            var isFocused by remember { mutableStateOf(false) }

            Surface(
                onClick = { onGenreSelect(key) },
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (isSelected) KuroNamiColors.Crimson else KuroNamiColors.SurfaceLow,
                    contentColor = if (isSelected) Color.White else KuroNamiColors.OnSurfaceVariant,
                    focusedContainerColor = KuroNamiColors.CrimsonContainer,
                    focusedContentColor = Color.White
                ),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .border(
                        width = 1.dp,
                        color = if (isFocused) KuroNamiColors.Cyan else KuroNamiColors.Outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(
    item: MediaItem?,
    isWatchlisted: Boolean = false,
    onClick: (String) -> Unit,
    onToggleWatchlist: () -> Unit = {}
) {
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
                        .fillMaxWidth(0.68f)
                ) {
                    // Web-Matched Badges Row (#1 Spotlight, 4K HDR, Simulcast Exclusive, Dual Audio)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.CrimsonContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#1 SPOTLIGHT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.SurfaceHighest.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "4K HDR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.CyanContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SIMULCAST",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.Cyan,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(KuroNamiColors.SurfaceHighest.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DUAL AUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = KuroNamiColors.CyanSoft,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!item.rating.isNullOrBlank()) {
                            Text(
                                text = "★ ${item.rating}",
                                style = MaterialTheme.typography.labelMedium,
                                color = KuroNamiColors.Gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.description ?: "Stream anime in ultra-crisp HD with synced dual-audio playback on Om CLI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuroNamiColors.OnSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action pill buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    text = "Watch Episode 1",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            onClick = onToggleWatchlist,
                            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = KuroNamiColors.SurfaceLow,
                                contentColor = Color.White,
                                focusedContainerColor = KuroNamiColors.SurfaceHighest,
                                focusedContentColor = Color.White
                            ),
                            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    if (isWatchlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (isWatchlisted) KuroNamiColors.Crimson else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isWatchlisted) "In Watchlist" else "Add to Watchlist",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
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
