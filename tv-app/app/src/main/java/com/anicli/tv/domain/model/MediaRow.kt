package com.anicli.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaRow(
    val title: String,
    val items: List<MediaItem>
)
