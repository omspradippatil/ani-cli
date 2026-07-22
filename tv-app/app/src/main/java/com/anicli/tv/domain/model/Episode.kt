package com.anicli.tv.domain.model

data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val thumbnail: String? = null,
    val isFiller: Boolean = false
)
