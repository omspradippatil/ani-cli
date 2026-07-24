package com.anicli.tv.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.isLoading) {
            Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
        } else {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    TopNavigationBar(onSearchClick = onSearchClick)
                }
                
                if (state.rows.isNotEmpty()) {
                    item {
                        HeroBanner(item = state.rows.first().items.firstOrNull(), onClick = { onMediaClick(it) })
                    }
                }

                items(state.rows) { row ->
                    HomeRow(row = row, onMediaClick = onMediaClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavigationBar(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AniCli", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Button(onClick = onSearchClick) { Text("Search") }
        Button(onClick = { }) { Text("Home") }
        Button(onClick = { }) { Text("Library") }
        Button(onClick = { }) { Text("Settings") }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroBanner(item: MediaItem?, onClick: (String) -> Unit) {
    if (item == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
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
fun HomeRow(row: MediaRow, onMediaClick: (String) -> Unit) {
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
                    onClick = { onMediaClick(item.id) }
                )
            }
        }
    }
}
