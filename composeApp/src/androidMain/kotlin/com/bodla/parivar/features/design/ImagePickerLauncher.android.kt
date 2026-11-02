package com.bodla.parivar.features.design

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ImagePickerButton(label: String, modifier: Modifier, onImagePicked: (ByteArray?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImagePicked(uri?.let { selected -> context.contentResolver.openInputStream(selected)?.use { it.readBytes() } })
    }
    TextButton(onClick = { launcher.launch("image/*") }, modifier = modifier) { Text(label) }
}

@Composable
actual fun ResultDocumentPicker(label: String, modifier: Modifier, onDocumentPicked: (ResultDocument?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val selected = uri?.let {
            val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
            val type = context.contentResolver.getType(it)
            val name = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
            } ?: "result"
            if (bytes != null && type != null) ResultDocument(bytes, name, type) else null
        }
        onDocumentPicked(selected)
    }
    TextButton(onClick = { launcher.launch(arrayOf("image/*", "application/pdf")) }, modifier = modifier) { Text(label) }
}
