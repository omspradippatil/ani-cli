package com.anicli.tv.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object KuroNamiColors {
    val Crimson = Color(0xFFFF2A5F)
    val CrimsonContainer = Color(0xFFFF4F73)
    val Cyan = Color(0xFF00E3FD)
    val CyanSoft = Color(0xFF9CF0FF)
    val CyanContainer = Color(0xFF00363D)
    val Obsidian = Color(0xFF0C0E14)
    val Surface = Color(0xFF13151D)
    val SurfaceLow = Color(0xFF191B24)
    val SurfaceHigh = Color(0xFF222531)
    val SurfaceHighest = Color(0xFF2E3242)
    val OnSurface = Color(0xFFE2E2EA)
    val OnSurfaceVariant = Color(0xFF9EA3B0)
    val Outline = Color(0xFF383D4D)
    val Gold = Color(0xFFFFD54F)
}

@OptIn(ExperimentalTvMaterial3Api::class)
private val DarkColorScheme = darkColorScheme(
    primary = KuroNamiColors.Crimson,
    onPrimary = Color.White,
    primaryContainer = KuroNamiColors.CrimsonContainer,
    onPrimaryContainer = Color.White,
    secondary = KuroNamiColors.Cyan,
    onSecondary = Color.Black,
    secondaryContainer = KuroNamiColors.CyanContainer,
    onSecondaryContainer = KuroNamiColors.Cyan,
    background = KuroNamiColors.Obsidian,
    onBackground = KuroNamiColors.OnSurface,
    surface = KuroNamiColors.Surface,
    onSurface = KuroNamiColors.OnSurface,
    surfaceVariant = KuroNamiColors.SurfaceLow,
    onSurfaceVariant = KuroNamiColors.OnSurfaceVariant
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AniCliTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
