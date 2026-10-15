package com.bodla.parivar.features.notices

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ZoomableImage(
    model: Any,
    contentDescription: String,
    modifier: Modifier = Modifier,
    zoomInLabel: String,
    zoomOutLabel: String,
    resetLabel: String
) {
    ZoomableMedia(
        modifier = modifier,
        zoomInLabel = zoomInLabel,
        zoomOutLabel = zoomOutLabel,
        resetLabel = resetLabel
    ) { imageModifier ->
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
    }
}

@Composable
fun ZoomableMedia(
    modifier: Modifier = Modifier,
    zoomInLabel: String,
    zoomOutLabel: String,
    resetLabel: String,
    content: @Composable (Modifier) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 6f)
        val ratio = nextScale / scale
        scale = nextScale
        if (scale == 1f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX = offsetX * ratio + panChange.x
            offsetY = offsetY * ratio + panChange.y
        }
    }

    Box(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                content(Modifier.fillMaxSize())
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
            shape = CircleShape,
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    modifier = Modifier.semantics { contentDescription = zoomOutLabel },
                    onClick = {
                        scale = (scale / 1.25f).coerceAtLeast(1f)
                        if (scale == 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                ) {
                    Text("−", color = androidx.compose.ui.graphics.Color.White)
                }
                TextButton(onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                }) {
                    Text(resetLabel, color = androidx.compose.ui.graphics.Color.White)
                }
                TextButton(
                    modifier = Modifier.semantics { contentDescription = zoomInLabel },
                    onClick = { scale = (scale * 1.25f).coerceAtMost(6f) }
                ) {
                    Text("+", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
