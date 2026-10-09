package com.bodla.parivar.core.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bodla.parivar.R

// ==============================================================================
// Android actual: Load Anek Gujarati from bundled local TTF file.
// This is 100% reliable offline and doesn't depend on Google Play Services.
// ==============================================================================

actual val AnekGujaratiFont: FontFamily = FontFamily(
    Font(R.font.anek_gujarati, FontWeight.Normal),
    Font(R.font.anek_gujarati, FontWeight.Medium),
    Font(R.font.anek_gujarati, FontWeight.SemiBold),
    Font(R.font.anek_gujarati, FontWeight.Bold)
)
