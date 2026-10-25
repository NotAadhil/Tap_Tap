package com.tapflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Modern Artistic Frosted Glass & Pastel Beige Theme.
 */
private val LightColorScheme = lightColorScheme(
    primary = PastelSage,
    onPrimary = PastelOnSage,
    primaryContainer = PastelSageContainer,
    onPrimaryContainer = PastelSage,
    secondary = PastelTextSecondary,
    onSecondary = PastelGlassSurface,
    secondaryContainer = PastelSurfaceSubtle,
    onSecondaryContainer = PastelTextPrimary,
    tertiary = PastelTerracotta,
    onTertiary = PastelGlassSurface,
    tertiaryContainer = PastelTerracottaContainer,
    onTertiaryContainer = PastelTerracotta,
    background = PastelBeigeBg,
    onBackground = PastelTextPrimary,
    surface = PastelGlassSurface,
    onSurface = PastelTextPrimary,
    surfaceVariant = PastelSurfaceSubtle,
    onSurfaceVariant = PastelTextSecondary,
    outline = PastelBorder,
    outlineVariant = PastelBorderStrong,
    error = PastelError,
    onError = PastelGlassSurface,
    errorContainer = PastelErrorContainer,
    onErrorContainer = PastelError
)

private val DarkColorScheme = darkColorScheme(
    primary = PastelDarkSage,
    onPrimary = PastelDarkBg,
    primaryContainer = PastelDarkSageContainer,
    onPrimaryContainer = PastelDarkSage,
    secondary = PastelDarkTextSecondary,
    onSecondary = PastelDarkSurface,
    secondaryContainer = PastelDarkSurfaceSubtle,
    onSecondaryContainer = PastelDarkTextPrimary,
    tertiary = PastelTerracotta,
    onTertiary = PastelDarkSurface,
    tertiaryContainer = PastelDarkSageContainer,
    onTertiaryContainer = PastelAmber,
    background = PastelDarkBg,
    onBackground = PastelDarkTextPrimary,
    surface = PastelDarkSurface,
    onSurface = PastelDarkTextPrimary,
    surfaceVariant = PastelDarkSurfaceSubtle,
    onSurfaceVariant = PastelDarkTextSecondary,
    outline = PastelDarkBorder,
    outlineVariant = PastelDarkBorder,
    error = PastelError,
    onError = PastelDarkSurface,
    errorContainer = PastelErrorContainer,
    onErrorContainer = PastelError
)

@Composable
fun TapFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
