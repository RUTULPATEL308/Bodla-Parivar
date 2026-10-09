package com.bodla.parivar.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    ENGLISH("en", "English", "English")
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.GUJARATI }

@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppLanguage provides language) {
        content()
    }
}
