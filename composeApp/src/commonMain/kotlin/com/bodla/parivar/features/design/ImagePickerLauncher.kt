package com.bodla.parivar.features.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ImagePickerButton(label: String, modifier: Modifier, onImagePicked: (ByteArray?) -> Unit)

data class ResultDocument(val bytes: ByteArray, val name: String, val contentType: String)

@Composable
expect fun ResultDocumentPicker(label: String, modifier: Modifier, onDocumentPicked: (ResultDocument?) -> Unit)
