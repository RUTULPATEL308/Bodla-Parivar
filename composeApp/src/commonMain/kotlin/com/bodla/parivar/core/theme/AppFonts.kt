package com.bodla.parivar.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

// ==============================================================================
// AppFonts — expect/actual for platform font loading
// ==============================================================================

// Expect declaration: each platform provides its own Anek Gujarati FontFamily.
// On Android: resolved via downloadable fonts (Google Fonts) or bundled TTF.
// On iOS: system fallback.
expect val AnekGujaratiFont: FontFamily

// Convenience alias used in Typography.kt
val BodlaFontFamily: FontFamily
    get() = AnekGujaratiFont
