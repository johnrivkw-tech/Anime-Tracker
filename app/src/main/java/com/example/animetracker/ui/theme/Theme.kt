package com.example.animetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-mode-only, intentionally — no light theme, no dynamic wallpaper color.
// The brand identity (Void background + Blaze/Pulse accents) is the point,
// so we don't hand it over to Material You's per-device palette.
private val AppColorScheme = darkColorScheme(
    primary = Blaze,
    onPrimary = Bone,
    primaryContainer = BlazeDim,
    onPrimaryContainer = Bone,
    secondary = Pulse,
    onSecondary = Bone,
    secondaryContainer = Charcoal,
    onSecondaryContainer = Bone,
    tertiary = Pulse,
    onTertiary = Bone,
    background = Void,
    onBackground = Bone,
    surface = Charcoal,
    onSurface = Bone,
    surfaceVariant = CharcoalHigh,
    onSurfaceVariant = Smoke,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = Void
)

@Composable
fun AnimeTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
