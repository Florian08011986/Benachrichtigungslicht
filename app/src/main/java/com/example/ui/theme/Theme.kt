package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EdgeLightingColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF380042),
    onSecondaryContainer = NeonMagenta,
    tertiary = ElectricPurple,
    onTertiary = Color.White,
    background = DarkOledBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

@Composable
fun EdgeLightingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EdgeLightingColorScheme,
        typography = Typography,
        content = content
    )
}
