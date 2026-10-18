package com.bodla.parivar.core.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import com.bodla.parivar.core.localization.AppLanguage
import com.bodla.parivar.core.localization.LocalAppLanguage

expect fun PlatformPrint(content: String, title: String)
expect fun PlatformShare(content: String, title: String)

/**
 * Material design vector icons for Print and Share that don't depend on extended icons library.
 */
private val IconPrint: ImageVector by lazy {
    ImageVector.Builder(
        name = "Print",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(19f, 8f)
        horizontalLineTo(5f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(-4f)
        horizontalLineToRelative(4f)
        verticalLineToRelative(-6f)
        curveToRelative(0f, -1.66f, -1.34f, -3f, -3f, -3f)
        close()
        moveTo(16f, 19f)
        horizontalLineTo(8f)
        verticalLineToRelative(-5f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(5f)
        close()
        moveTo(19f, 12f)
        curveToRelative(-0.55f, 0f, -1f, -0.45f, -1f, -1f)
        reflectiveCurveToRelative(0.45f, -1f, 1f, -1f)
        reflectiveCurveToRelative(1f, 0.45f, 1f, 1f)
        reflectiveCurveToRelative(-0.45f, 1f, -1f, 1f)
        close()
        moveTo(18f, 3f)
        horizontalLineTo(6f)
        verticalLineToRelative(4f)
        horizontalLineToRelative(12f)
        verticalLineTo(3f)
        close()
    }.build()
}

private val IconShare: ImageVector by lazy {
    ImageVector.Builder(
        name = "Share",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(fill = SolidColor(Color.Black)) {
        moveTo(18f, 16.08f)
        curveToRelative(-0.76f, 0f, -1.44f, 0.3f, -1.96f, 0.77f)
        lineTo(8.91f, 12.7f)
        curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
        reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
        lineToRelative(7.05f, -4.11f)
        curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
        curveToRelative(1.66f, 0f, 3f, -1.34f, 3f, -3f)
        reflectiveCurveToRelative(-1.34f, -3f, -3f, -3f)
        reflectiveCurveToRelative(-3f, 1.34f, -3f, 3f)
        curveToRelative(0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
        reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
        curveToRelative(0.79f, 0f, 1.5f, -0.31f, 2.04f, -0.81f)
        lineToRelative(7.12f, 4.16f)
        curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
        curveToRelative(0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
        reflectiveCurveToRelative(2.92f, -1.31f, 2.92f, -2.92f)
        curveToRelative(0f, -1.61f, -1.31f, -2.92f, -2.92f, -2.92f)
        close()
    }.build()
}

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
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp).semantics {
            contentDescription = if (lang == AppLanguage.GUJARATI) "\u0AAA\u0ACD\u0AB0\u0ABF\u0AA8\u0ACD\u0A9F \u0A95\u0AB0\u0ACB" else "Print"
        }) { Icon(IconPrint, contentDescription = null) }
        IconButton(onClick = { PlatformShare(content, title) }, modifier = Modifier.size(48.dp).semantics {
            contentDescription = if (lang == AppLanguage.GUJARATI) "\u0AB6\u0AC7\u0AB0 \u0A95\u0AB0\u0ACB" else "Share"
        }) { Icon(IconShare, contentDescription = null) }
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
