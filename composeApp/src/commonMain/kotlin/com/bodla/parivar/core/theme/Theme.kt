package com.bodla.parivar.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
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
fun bodlaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = WarmWhiteSurface,
    unfocusedContainerColor = WarmWhiteSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = SaffronPrimary,
    focusedBorderColor = SaffronPrimary,
    unfocusedBorderColor = BorderStroke,
    focusedLabelColor = SaffronPrimary,
    unfocusedLabelColor = TextSecondary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun bodlaTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = WarmWhiteSurface,
    titleContentColor = TextPrimary,
    navigationIconContentColor = TextPrimary,
    actionIconContentColor = TextPrimary
)

@Composable
fun BodlaParivarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customTextSelectionColors = TextSelectionColors(
        handleColor = SaffronPrimary,
        backgroundColor = SaffronPrimary.copy(alpha = 0.35f)
    )

    CompositionLocalProvider(
        LocalThemeIsDark provides darkTheme,
        LocalTextSelectionColors provides customTextSelectionColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
