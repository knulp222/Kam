package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KaamelottDarkColorScheme = darkColorScheme(
    primary = SophisticatedGold,
    onPrimary = SophisticatedBg,
    primaryContainer = SophisticatedSurfaceHighlight,
    onPrimaryContainer = SophisticatedGold,
    secondary = SophisticatedGoldDim,
    onSecondary = SophisticatedBg,
    background = SophisticatedBg,
    onBackground = SophisticatedTextPrimary,
    surface = SophisticatedSurface,
    onSurface = SophisticatedTextPrimary,
    surfaceVariant = SophisticatedSurfaceHighlight,
    onSurfaceVariant = SophisticatedTextSecondary,
    outline = SophisticatedTextMuted,
    outlineVariant = SophisticatedBorder,
    error = AvoidRed,
    onError = SophisticatedBg,
    errorContainer = AvoidRedDim,
    onErrorContainer = AvoidRed
)

@Composable
fun KaamelottNuitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KaamelottDarkColorScheme,
        typography = Typography,
        content = content
    )
}
