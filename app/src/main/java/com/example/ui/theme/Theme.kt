package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberTelemetryColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF022C36),
    primaryContainer = CyberCyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = CyberEmerald,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = CyberEmeraldDim,
    onSecondaryContainer = TextPrimary,
    tertiary = CyberAmber,
    onTertiary = Color(0xFF2D1600),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = CyberBorderBright,
    error = CyberRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberTelemetryColorScheme,
        typography = Typography,
        content = content
    )
}
