package com.bodla.parivar.features.notices

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
actual fun NoticeAttachmentPreview(url: String, modifier: Modifier) {
    if (url.substringBefore('?').endsWith(".pdf", ignoreCase = true)) {
        PdfAttachmentPreview(url, modifier)
    } else {
        WebAttachmentPreview(url, modifier)
    }
}

@Composable
private fun PdfAttachmentPreview(url: String, modifier: Modifier) {
    val context = LocalContext.current
    var currentPage by remember(url) { mutableIntStateOf(0) }
    var pageCount by remember(url) { mutableIntStateOf(0) }
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(url) { mutableStateOf(true) }
    var error by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url, currentPage) {
        loading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) {
                val pdfFile = File(context.cacheDir, "attachment-${url.hashCode()}.pdf")
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 20_000
                    connection.readTimeout = 30_000
                    connection.instanceFollowRedirects = true
                    try {
                        connection.inputStream.use { input ->
                            pdfFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }

                val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(descriptor).use { renderer ->
                    require(renderer.pageCount > 0) { "This PDF has no pages." }
                    val index = currentPage.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(index).use { page ->
                        val scale = minOf(1f, 1600f / maxOf(page.width, page.height))
                        val pageBitmap = Bitmap.createBitmap(
                            (page.width * scale).toInt().coerceAtLeast(1),
                            (page.height * scale).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        Triple(pageBitmap, renderer.pageCount, index)
                    }
                }.also { descriptor.close() }
            }
            bitmap = result.first
            pageCount = result.second
            currentPage = result.third
        } catch (exception: Exception) {
            error = exception.message ?: "Could not display this PDF."
        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 300.dp, max = 560.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(32.dp))
            error != null -> Text(error.orEmpty(), modifier = Modifier.padding(16.dp))
            bitmap != null -> ZoomableMedia(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                zoomInLabel = "Zoom in",
                zoomOutLabel = "Zoom out",
                resetLabel = "Reset"
            ) { imageModifier ->
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Aheval PDF page ${currentPage + 1}",
                    modifier = imageModifier,
                    contentScale = ContentScale.Fit
                )
            }
        }
        if (pageCount > 1 && error == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = currentPage > 0 && !loading, onClick = { currentPage-- }) {
                    Text("‹")
                }
                Text("${currentPage + 1} / $pageCount")
                IconButton(enabled = currentPage < pageCount - 1 && !loading, onClick = { currentPage++ }) {
                    Text("›")
                }
            }
        }
    }
}

@Composable
private fun WebAttachmentPreview(url: String, modifier: Modifier) {
    val path = url.substringBefore('?').lowercase()
    val isOfficeDocument = path.endsWith(".doc") || path.endsWith(".docx")
    val previewUrl = if (isOfficeDocument) {
        "https://docs.google.com/gview?embedded=1&url=${android.net.Uri.encode(url)}"
    } else {
        url
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = isOfficeDocument
                settings.mediaPlaybackRequiresUserGesture = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(previewUrl)
            }
        },
        update = { view ->
            if (view.url != previewUrl) view.loadUrl(previewUrl)
        }
    )
}
