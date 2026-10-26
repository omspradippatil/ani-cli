package com.anicli.tv.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*
import com.anicli.tv.core.presentation.MediaCard
import com.anicli.tv.core.theme.KuroNamiColors

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    onMediaClick: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    BackHandler { onBack() }
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val quickPicks = listOf(
        "Solo Leveling",
        "Chainsaw Man",
        "One Piece",
        "Bleach",
        "Demon Slayer",
        "Black Clover",
        "Jujutsu Kaisen",
        "Attack on Titan"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroNamiColors.Obsidian)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Search Input Box with Focus Border & 1-Click Clear
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSearchFocused) KuroNamiColors.SurfaceHigh else KuroNamiColors.SurfaceLow,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (isSearchFocused) 2.5.dp else 1.5.dp,
                    color = if (isSearchFocused) KuroNamiColors.Crimson else KuroNamiColors.Outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isSearchFocused) KuroNamiColors.Crimson else KuroNamiColors.OnSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.search(it)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(KuroNamiColors.Crimson),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isSearchFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            } else false
                        },
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search anime, movies, series...",
                                color = KuroNamiColors.OnSurfaceVariant,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        innerTextField()
                    }
                )
                if (query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            query = ""
                            viewModel.search("")
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = KuroNamiColors.Surface,
                            focusedContainerColor = KuroNamiColors.Crimson,
                            contentColor = KuroNamiColors.OnSurfaceVariant,
                            focusedContentColor = Color.White
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Picks Row for 1-Click TV Remote Navigation
        TvLazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPicks) { pick ->
                Surface(
                    onClick = {
                        query = pick
                        viewModel.search(pick)
                    },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.Crimson,
                        contentColor = KuroNamiColors.OnSurfaceVariant,
                        focusedContentColor = Color.White
                    ),
                    modifier = Modifier.border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = pick,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Searching across anime catalog...",
                    style = MaterialTheme.typography.titleMedium,
                    color = KuroNamiColors.Cyan
                )
            }
        } else if (state.results.isEmpty() && query.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No anime found for \"$query\"",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try checking the spelling or searching by Japanese/Romaji title.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KuroNamiColors.OnSurfaceVariant
                    )
                }
            }
        } else {
            val displayItems = if (query.isNotEmpty()) state.results else state.recommendations
            val sectionTitle = if (query.isNotEmpty()) "Search Results (${state.results.size})" else "Trending & Recommended Anime"
            val accentColor = if (query.isNotEmpty()) KuroNamiColors.Crimson else KuroNamiColors.Cyan

            if (displayItems.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = KuroNamiColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 64.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayItems) { item ->
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
