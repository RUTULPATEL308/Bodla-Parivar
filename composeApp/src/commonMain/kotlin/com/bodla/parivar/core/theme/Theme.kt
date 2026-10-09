package com.bodla.parivar.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = WarmWhiteSurface,
    primaryContainer = LightIconBg,
    onPrimaryContainer = SaffronPrimary,
    secondary = LeafGreenSecondary,
    onSecondary = WarmWhiteSurface,
    tertiary = GoldenAccent,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = WarmWhiteSurface,
    onSurface = TextPrimary,
    surfaceVariant = CreamBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderStroke,
    error = StatusError,
    onError = WarmWhiteSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkIconBg,
    onPrimaryContainer = GoldenAccent,
    secondary = LeafGreenSecondary,
    onSecondary = DarkBackground,
    tertiary = GoldenAccent,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = StatusError,
    onError = DarkBackground
)

val LocalThemeIsDark = staticCompositionLocalOf { false }

@Composable
fun BodlaParivarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
