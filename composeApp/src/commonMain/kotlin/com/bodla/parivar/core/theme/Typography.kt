package com.bodla.parivar.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==============================================================================
// Typography: Anek Gujarati (loaded via BodlaFontFamily expect/actual)
// Conforms to Product Specification Section 10
// ==============================================================================

val BodlaDisplay = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 36.sp,
    letterSpacing = 0.sp
)

val BodlaHeaderTitle = BodlaDisplay

val BodlaPageTitle = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.sp
)

val BodlaSection = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.sp
)

val BodlaCardTitle = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.sp
)

// Gujarati body text is 16sp minimum for supreme legibility
val BodlaBody = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.25.sp
)

val BodlaButton = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

val BodlaCaption = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
)

val BodlaNavigation = TextStyle(
    fontFamily = BodlaFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.4.sp
)

val AppTypography = Typography(
    displayLarge = BodlaDisplay,
    titleLarge = BodlaPageTitle,
    titleMedium = BodlaSection,
    titleSmall = BodlaCardTitle,
    bodyLarge = BodlaBody,
    bodyMedium = BodlaBody.copy(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = BodlaButton,
    bodySmall = BodlaCaption,
    labelSmall = BodlaNavigation
)
