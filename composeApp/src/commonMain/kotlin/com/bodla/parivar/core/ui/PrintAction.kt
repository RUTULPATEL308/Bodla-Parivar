package com.bodla.parivar.core.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage

expect fun PlatformPrint(content: String, title: String)

@Composable
fun PrintAction(
    content: String,
    title: String,
    englishContent: String = content,
    bothContent: String = content,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }, modifier = modifier) {
        Text(if (lang == AppLanguage.GUJARATI) "પ્રિન્ટ કરો" else "Print")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text("ગુજરાતી") }, onClick = {
            expanded = false
            PlatformPrint(content, "$title — ગુજરાતી")
        })
        DropdownMenuItem(text = { Text("English") }, onClick = {
            expanded = false
            PlatformPrint(englishContent, "$title — English")
        })
        DropdownMenuItem(text = { Text(if (lang == AppLanguage.GUJARATI) "બંને ભાષા" else "Both languages") }, onClick = {
            expanded = false
            PlatformPrint(bothContent, "$title — Gujarati and English")
        })
    }
}
