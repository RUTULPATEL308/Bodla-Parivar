package com.bodla.parivar.core.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.unit.IntRect
import java.io.ByteArrayOutputStream
import java.io.File
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bodla.parivar.core.AndroidPlatform
import io.github.aakira.napier.Napier

actual fun PlatformPrint(content: String, title: String) {
    val activity = AndroidPlatform.currentActivityRef?.get()
    val context = activity ?: AndroidPlatform.appContext ?: return
    val html = "<html><meta charset=\"utf-8\"><head><style>@font-face{font-family:AnekGujarati;src:url('file:///android_res/font/anek_gujarati.ttf')}body{font-family:AnekGujarati,sans-serif;padding:16px;white-space:pre-wrap;font-size:12pt;line-height:1.45}</style></head><body>${content.escapeHtml()}</body></html>"

    val runPrint = {
        try {
            val webView = WebView(context)
            var printed = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    if (printed) return
                    printed = true
                    view.post {
                        try {
                            val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            manager?.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
                        } catch (e: Exception) {
                            Napier.e("PlatformPrint error", e, tag = "Print")
                        }
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        } catch (e: Exception) {
            Napier.e("Failed to initialize WebView for printing", e, tag = "Print")
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        runPrint()
    } else {
        Handler(Looper.getMainLooper()).post(runPrint)
    }
}

private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

actual fun PlatformShare(content: String, title: String) {
    val activity = AndroidPlatform.currentActivityRef?.get()
    val context = activity ?: AndroidPlatform.appContext ?: return

    val runShare = {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            val chooserIntent = Intent.createChooser(sendIntent, title).apply {
                if (activity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Napier.e("PlatformShare error", e, tag = "Share")
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        runShare()
    } else {
        Handler(Looper.getMainLooper()).post(runShare)
    }
}

actual fun PlatformShareFile(content: String, filename: String, mimeType: String) {
    val activity = AndroidPlatform.currentActivityRef?.get() ?: return
    try {
        val file = File(activity.cacheDir, filename)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, filename))
    } catch (e: Exception) {
        Napier.e("PlatformShareFile error", e, tag = "Share")
    }
}

actual fun PlatformCapturePoster(boundsInWindow: IntRect): ByteArray? {
    val activity = AndroidPlatform.currentActivityRef?.get() ?: return null
    return try {
        val window = activity.window ?: return null
        val decorView = window.decorView
        val viewWidth = decorView.width.takeIf { it > 0 } ?: boundsInWindow.width
        val viewHeight = decorView.height.takeIf { it > 0 } ?: boundsInWindow.height
        if (viewWidth <= 0 || viewHeight <= 0) return null

        var fullBitmap: Bitmap? = null

        // 1. Try modern PixelCopy API (API 26+, supported on all hardware-accelerated windows)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val pixelBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                val countDownLatch = java.util.concurrent.CountDownLatch(1)
                var copyResult = android.view.PixelCopy.ERROR_UNKNOWN

                val handlerThread = android.os.HandlerThread("PixelCopyThread").apply { start() }
                val handler = Handler(handlerThread.looper)

                android.view.PixelCopy.request(window, pixelBitmap, { result ->
                    copyResult = result
                    countDownLatch.countDown()
                }, handler)

                countDownLatch.await(1500, java.util.concurrent.TimeUnit.MILLISECONDS)
                handlerThread.quitSafely()

                if (copyResult == android.view.PixelCopy.SUCCESS) {
                    fullBitmap = pixelBitmap
                } else {
                    pixelBitmap.recycle()
                }
            } catch (e: Exception) {
                Napier.w("PixelCopy failed, attempting Canvas fallback", e, tag = "Share")
            }
        }

        // 2. Fallback to software Canvas draw if PixelCopy was unavailable or failed
        if (fullBitmap == null) {
            try {
                val canvasBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(canvasBitmap)
                decorView.draw(canvas)
                fullBitmap = canvasBitmap
            } catch (e: Exception) {
                Napier.w("Canvas draw failed, attempting drawingCache", e, tag = "Share")
            }
        }

        // 3. Fallback to drawingCache as last resort
        if (fullBitmap == null) {
            @Suppress("DEPRECATION")
            decorView.isDrawingCacheEnabled = true
            @Suppress("DEPRECATION")
            decorView.buildDrawingCache()
            @Suppress("DEPRECATION")
            val cache = decorView.drawingCache
            if (cache != null && cache.width > 0 && cache.height > 0) {
                fullBitmap = Bitmap.createBitmap(cache)
            }
            @Suppress("DEPRECATION")
            decorView.isDrawingCacheEnabled = false
        }

        val source = fullBitmap ?: return null

        val left = boundsInWindow.left.coerceIn(0, source.width - 1)
        val top = boundsInWindow.top.coerceIn(0, source.height - 1)
        val right = boundsInWindow.right.coerceIn(left + 1, source.width)
        val bottom = boundsInWindow.bottom.coerceIn(top + 1, source.height)
        val cropWidth = right - left
        val cropHeight = bottom - top

        if (cropWidth <= 0 || cropHeight <= 0) {
            source.recycle()
            return null
        }

        val poster = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
        if (poster !== source) {
            source.recycle()
        }

        ByteArrayOutputStream().use { output ->
            poster.compress(Bitmap.CompressFormat.PNG, 100, output)
            poster.recycle()
            output.toByteArray()
        }
    } catch (error: Exception) {
        Napier.e("Could not capture Antim Yatra poster", error, tag = "Share")
        null
    }
}

actual fun PlatformOpenUrl(url: String) {
    val activity = AndroidPlatform.currentActivityRef?.get() ?: return
    try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Napier.e("PlatformOpenUrl error", e, tag = "Share")
    }
}
