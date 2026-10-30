package com.anicli.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val thumbnail: String? = null,
    val isFiller: Boolean = false
)
