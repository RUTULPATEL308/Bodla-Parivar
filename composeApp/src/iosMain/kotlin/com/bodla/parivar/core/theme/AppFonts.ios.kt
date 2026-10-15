package com.bodla.parivar.core.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont

// ==============================================================================
// iOS actual: Anek Gujarati is not available as a system font on iOS.
// Falls back to the system default sans-serif family.
// To use a custom font on iOS, bundle the TTF in the iosApp Xcode project
// and load it via a proper iOS font registration mechanism.
// ==============================================================================

// The Xcode target registers AnekGujarati.ttf through UIAppFonts. SystemFont
// then resolves the bundled face for the shared Compose typography on iOS.
actual val AnekGujaratiFont: FontFamily = FontFamily(SystemFont("AnekGujarati-Medium"))
