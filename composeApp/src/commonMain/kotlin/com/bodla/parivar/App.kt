package com.bodla.parivar

import androidx.compose.runtime.Composable
import com.bodla.parivar.core.theme.BodlaParivarTheme
import com.bodla.parivar.features.main.MainScreen

@Composable
fun App() {
    BodlaParivarTheme {
        MainScreen()
    }
}
