package com.artcompanion.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFE8B86D),        // warm ochre accent, reads well next to artwork
    onPrimary = Color(0xFF1B1408),
    secondary = Color(0xFF8FB3C9),
    background = Color(0xFF0F0F11),
    surface = Color(0xFF17171A),
    surfaceVariant = Color(0xFF222227),
    onSurface = Color(0xFFE6E4E0),
    onSurfaceVariant = Color(0xFFA9A6A0),
    error = Color(0xFFE5776B),
)

@Composable
fun ArtCompanionTheme(content: @Composable () -> Unit) {
    // Phase 3 adds the light theme and a user setting; the PoC is dark only.
    MaterialTheme(colorScheme = Dark, content = content)
}
