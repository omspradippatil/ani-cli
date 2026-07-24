package com.anicli.tv.core.presentation

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.anicli.tv.domain.model.MediaItem

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(150.dp)
            .padding(8.dp)
            .aspectRatio(2f / 3.5f) // Slightly taller to fit text
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color.DarkGray)
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f/3f),
                contentScale = ContentScale.Crop
            )
            androidx.tv.material3.Text(
                text = item.title,
                style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
