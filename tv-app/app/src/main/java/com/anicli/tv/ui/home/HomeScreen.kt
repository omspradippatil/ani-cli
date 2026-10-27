package com.anicli.tv.ui.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anicli.tv.core.presentation.ContinueWatchingCard
import com.anicli.tv.core.presentation.MediaCard
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow
import kotlinx.coroutines.delay

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
    val initialFocusRequester = remember { FocusRequester() }

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

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            delay(400)
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = selectedDrawerItem != "Home") {
        selectedDrawerItem = "Home"
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
                when (selectedDrawerItem) {
                    "My Watchlist" -> {
                        WatchlistView(
                            rows = state.rows,
                            onMediaClick = onMediaClick,
                            onExploreClick = { selectedDrawerItem = "Home" }
                        )
                    }
                    "History" -> {
                        HistoryView(
                            rows = state.rows,
                            onMediaClick = onMediaClick,
                            onExploreClick = { selectedDrawerItem = "Home" }
                        )
                    }
                    else -> {
                        TvLazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 64.dp, top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                HeroBanner(
                                    item = focusedItem,
                                    isWatchlisted = state.favoriteMediaIds.contains(focusedItem?.id ?: ""),
                                    primaryFocusRequester = initialFocusRequester,
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
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WatchlistView(
    rows: List<MediaRow>,
    onMediaClick: (String) -> Unit,
    onExploreClick: () -> Unit
) {
    val watchlistRow = rows.firstOrNull { it.title.equals("My Watchlist", ignoreCase = true) }
    val items = watchlistRow?.items ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 28.dp, top = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KuroNamiColors.Crimson)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "My Watchlist",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(12.dp))
                    .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${items.size} Titles",
                    style = MaterialTheme.typography.labelMedium,
                    color = KuroNamiColors.Crimson,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(16.dp))
                        .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 48.dp, vertical = 40.dp)
                ) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = KuroNamiColors.Crimson,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add series to your watchlist to track and watch them later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuroNamiColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer,
                            contentColor = Color.White
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Explore Anime",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 64.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    MediaCard(
                        item = item,
                        onClick = { onMediaClick(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryView(
    rows: List<MediaRow>,
    onMediaClick: (String) -> Unit,
    onExploreClick: () -> Unit
) {
    val historyRow = rows.firstOrNull { it.title.equals("Continue Watching", ignoreCase = true) }
    val items = historyRow?.items ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 28.dp, top = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KuroNamiColors.Cyan)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Watch History & Continue Watching",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(12.dp))
                    .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${items.size} Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = KuroNamiColors.Cyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(16.dp))
                        .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 48.dp, vertical = 40.dp)
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = KuroNamiColors.Cyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Watch History Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Episodes you stream will automatically appear here with your saved progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuroNamiColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.Cyan,
                            focusedContainerColor = KuroNamiColors.Cyan.copy(alpha = 0.8f),
                            contentColor = Color.Black
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "Start Watching",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 64.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    ContinueWatchingCard(
                        item = item,
                        onClick = { onMediaClick(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
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

        // Navigation Items: Home FIRST so default focus is Home!
        DrawerItem(
            icon = Icons.Filled.Home,
            text = "Home",
            isSelected = selectedItem == "Home",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Home") }
        )
        DrawerItem(
            icon = Icons.Filled.Search,
            text = "Search",
            isSelected = selectedItem == "Search",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Search"); onSearchClick() }
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
            .padding(start = 24.dp, end = 28.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 28.dp)
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
    primaryFocusRequester: FocusRequester = remember { FocusRequester() },
    onClick: (String) -> Unit,
    onToggleWatchlist: () -> Unit = {}
) {
    if (item == null) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(start = 24.dp, end = 28.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(KuroNamiColors.SurfaceLow)
            .border(
                width = 1.dp,
                color = KuroNamiColors.Outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Vignette Gradients
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
                .fillMaxWidth(0.70f)
        ) {
            // Badges Row
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
                    val cleanRating = item.rating.replace("★", "").trim()
                    Text(
                        text = "★ $cleanRating",
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons with standard focusable buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { onClick(item.id) },
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.Crimson,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                    modifier = Modifier.focusRequester(primaryFocusRequester)
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
                            text = "Watch Episode 1",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = onToggleWatchlist,
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.SurfaceHigh,
                        focusedContainerColor = KuroNamiColors.SurfaceHighest
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            if (isWatchlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isWatchlisted) KuroNamiColors.Crimson else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isWatchlisted) "In Watchlist" else "Add to Watchlist",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = { onClick(item.id) },
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.SurfaceHigh
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = KuroNamiColors.OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeRow(row: MediaRow, onMediaClick: (String) -> Unit, onFocusItem: (MediaItem) -> Unit) {
    val isContinueWatching = row.title.equals("Continue Watching", ignoreCase = true)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 24.dp, end = 28.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(
                        if (isContinueWatching) KuroNamiColors.Cyan else KuroNamiColors.Crimson,
                        RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (isContinueWatching) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(KuroNamiColors.CyanContainer, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "JUMP BACK IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = KuroNamiColors.Cyan,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        TvLazyRow(
            contentPadding = PaddingValues(start = 24.dp, end = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(row.items) { item ->
                if (isContinueWatching) {
                    ContinueWatchingCard(
                        item = item,
                        onClick = { onMediaClick(item.id) },
                        onFocus = { onFocusItem(item) }
                    )
                } else {
                    MediaCard(
                        item = item,
                        onClick = { onMediaClick(item.id) },
                        onFocus = { onFocusItem(item) }
                    )
                }
            }
        }
    }
}
