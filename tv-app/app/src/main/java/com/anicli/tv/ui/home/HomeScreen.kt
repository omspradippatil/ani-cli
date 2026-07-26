
package com.anicli.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.anicli.tv.core.presentation.MediaCard
import com.anicli.tv.domain.model.MediaItem
import com.anicli.tv.domain.model.MediaRow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var focusedItem by remember { mutableStateOf<MediaItem?>(null) }
    var selectedDrawerItem by remember { mutableStateOf("Home") }

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }
    
    LaunchedEffect(state.rows) {
        if (focusedItem == null && state.rows.isNotEmpty()) {
            focusedItem = state.rows.first().items.firstOrNull()
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
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Solid background optimized for TV performance
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141414),
                            Color(0xFF000000)
                        )
                    )
                )
            )

            if (state.isLoading) {
                Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
            } else {
                TvLazyColumn(
                    modifier = Modifier.fillMaxSize().padding(start = 64.dp), // Padding for drawer
                    contentPadding = PaddingValues(bottom = 32.dp, top = 24.dp)
                ) {
                    item {
                        HeroBanner(item = focusedItem, onClick = { focusedItem?.let { onMediaClick(it.id) } })
                    }

                    items(state.rows) { row ->
                        HomeRow(row = row, onMediaClick = onMediaClick, onFocusItem = { focusedItem = it })
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
            .background(Color(0xFF141414).copy(alpha = 0.9f))
            .padding(16.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
    ) {
        // Profile Section
        Surface(
            onClick = { onItemSelected("Settings"); onSettingsClick() },
            modifier = Modifier.padding(bottom = 32.dp, top = 16.dp).fillMaxWidth(),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                }
                if (isExpanded) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Welcome", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Login / Register", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF5252))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            icon = Icons.Filled.PlayArrow,
            text = "Library",
            isSelected = selectedItem == "Library",
            isExpanded = isExpanded,
            onClick = { onItemSelected("Library") }
        )
        DrawerItem(
            icon = Icons.Filled.List,
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
            .padding(vertical = 8.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0xFFE50914) else Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.LightGray,
            focusedContainerColor = Color(0xFFE50914).copy(alpha = 0.8f),
            focusedContentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = text)
            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(item: MediaItem?, onClick: (String) -> Unit) {
    if (item == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        Card(
            onClick = { onClick(item.id) },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.backdropUrl ?: item.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(32.dp)
                ) {
                    Text(text = item.title, style = MaterialTheme.typography.displayMedium)
                    Text(
                        text = item.description ?: "A great adventure awaits in this highly rated anime. Watch it now on AniCli TV.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeRow(row: MediaRow, onMediaClick: (String) -> Unit, onFocusItem: (MediaItem) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )
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
