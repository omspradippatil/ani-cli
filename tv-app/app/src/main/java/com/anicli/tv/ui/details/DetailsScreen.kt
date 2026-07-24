package com.anicli.tv.ui.details

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
import com.anicli.tv.domain.model.Episode
import com.anicli.tv.domain.model.MediaDetail

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    onPlayClick: (String) -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(mediaId) {
        viewModel.loadDetails(mediaId)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.isLoading) {
            Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
        } else if (state.detail != null) {
            val detail = state.detail!!
            
            // Backdrop Image
            AsyncImage(
                model = detail.backdropUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(500.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )

            TvLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 250.dp, bottom = 32.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 48.dp)) {
                        Text(text = detail.title, style = MaterialTheme.typography.displayLarge)
                        
                        Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            detail.genres.forEach { genre ->
                                Text(text = genre, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            }
                        }

                        Text(
                            text = detail.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp).fillMaxWidth(0.6f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { onPlayClick(detail.episodes.firstOrNull()?.id ?: "") }) {
                                Text("Play")
                            }
                            Button(onClick = { }) {
                                Text("Favorite")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Episodes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 48.dp, top = 48.dp, bottom = 16.dp)
                    )
                }

                item {
                    TvLazyRow(
                        modifier = Modifier.padding(horizontal = 40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(detail.episodes) { episode ->
                            EpisodeCard(episode = episode, onClick = { onPlayClick(episode.id) })
                        }
                    }
                }
            }
        } else if (state.error != null) {
            Text(
                text = "Error: ${state.error}",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .padding(8.dp)
            .aspectRatio(16f / 9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = episode.thumbnail ?: "https://picsum.photos/seed/${episode.id}/400/225",
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Text(
                text = "${episode.number}. ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center).padding(8.dp)
            )
        }
    }
}
