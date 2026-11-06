package com.artcompanion.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.artcompanion.data.ThemeMode

private val Dark = darkColorScheme(
    primary = Color(0xFFE8B86D),        // warm ochre accent, neutral next to artwork
    onPrimary = Color(0xFF1B1408),
    primaryContainer = Color(0xFF3A2E1A),
    onPrimaryContainer = Color(0xFFF5DDB5),
    secondary = Color(0xFF8FB3C9),
    onSecondary = Color(0xFF0B1A24),
    secondaryContainer = Color(0xFF2A2B31),
    onSecondaryContainer = Color(0xFFE6E4E0),
    background = Color(0xFF0F0F11),
    onBackground = Color(0xFFE6E4E0),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFE6E4E0),
    surfaceVariant = Color(0xFF222227),
    onSurfaceVariant = Color(0xFFA9A6A0),
    surfaceContainer = Color(0xFF17171A),
    outline = Color(0xFF4A4850),
    error = Color(0xFFE5776B),
)

private val Light = lightColorScheme(
    primary = Color(0xFF7A5512),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5DDB5),
    onPrimaryContainer = Color(0xFF2A1B00),
    secondary = Color(0xFF3D6378),
    secondaryContainer = Color(0xFFE6E2DA),
    onSecondaryContainer = Color(0xFF1C1B1F),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFECE8E1),
    onSurfaceVariant = Color(0xFF5E5A54),
    surfaceContainer = Color(0xFFF1EEE8),
    outline = Color(0xFFB9B3AA),
    error = Color(0xFFB3261E),
)

@Composable
fun ArtCompanionTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
