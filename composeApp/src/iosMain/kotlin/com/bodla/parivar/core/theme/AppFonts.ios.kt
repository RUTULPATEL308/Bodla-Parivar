@file:OptIn(ExperimentalTextApi::class)

package com.bodla.parivar.core.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont

// iOS actual: the Xcode target registers AnekGujarati.ttf through UIAppFonts.
// SystemFont then resolves the bundled face for shared Compose typography.
actual val AnekGujaratiFont: FontFamily = FontFamily(SystemFont("AnekGujarati-Medium"))
