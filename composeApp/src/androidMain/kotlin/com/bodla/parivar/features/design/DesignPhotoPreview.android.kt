package com.bodla.parivar.features.design

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter

@Composable
actual fun DesignPhotoPreview(bytes: ByteArray, modifier: Modifier) {
    Image(
        painter = rememberAsyncImagePainter(model = bytes),
        contentDescription = "Portrait",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter
    )
}
