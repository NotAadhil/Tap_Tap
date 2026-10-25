package com.tapflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Editorial Research Color Schemes.
 * Prioritizes tactile parchment warmth, high legibility, and refined pill badges
 * matching Perplexity's light research aesthetic.
 */
private val LightColorScheme = lightColorScheme(
    primary = ResearchTeal,
    onPrimary = ResearchOnTeal,
    primaryContainer = ResearchTealContainer,
    onPrimaryContainer = ResearchTeal,
    secondary = ResearchTextSecondary,
    onSecondary = ResearchSurface,
    secondaryContainer = ResearchSurfaceSubtle,
    onSecondaryContainer = ResearchTextPrimary,
    tertiary = ResearchAmber,
    onTertiary = ResearchSurface,
    tertiaryContainer = ResearchAmberContainer,
    onTertiaryContainer = ResearchAmber,
    background = ResearchBeigeBg,
    onBackground = ResearchTextPrimary,
    surface = ResearchSurface,
    onSurface = ResearchTextPrimary,
    surfaceVariant = ResearchSurfaceSubtle,
    onSurfaceVariant = ResearchTextSecondary,
    outline = ResearchBorder,
    outlineVariant = ResearchBorderStrong,
    error = ResearchError,
    onError = ResearchSurface,
    errorContainer = ResearchErrorContainer,
    onErrorContainer = ResearchError
)

private val DarkColorScheme = darkColorScheme(
    primary = ResearchDarkTeal,
    onPrimary = ResearchDarkBg,
    primaryContainer = ResearchDarkTealContainer,
    onPrimaryContainer = ResearchDarkTeal,
    secondary = ResearchDarkTextSecondary,
    onSecondary = ResearchDarkSurface,
    secondaryContainer = ResearchDarkSurfaceSubtle,
    onSecondaryContainer = ResearchDarkTextPrimary,
    tertiary = ResearchAmber,
    onTertiary = ResearchDarkSurface,
    tertiaryContainer = ResearchDarkTealContainer,
    onTertiaryContainer = ResearchAmber,
    background = ResearchDarkBg,
    onBackground = ResearchDarkTextPrimary,
    surface = ResearchDarkSurface,
    onSurface = ResearchDarkTextPrimary,
    surfaceVariant = ResearchDarkSurfaceSubtle,
    onSurfaceVariant = ResearchDarkTextSecondary,
    outline = ResearchDarkBorder,
    outlineVariant = ResearchDarkBorder,
    error = ResearchError,
    onError = ResearchDarkSurface,
    errorContainer = ResearchErrorContainer,
    onErrorContainer = ResearchError
)

@Composable
fun TapFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Explicitly use the Perplexity Research Beige theme
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
