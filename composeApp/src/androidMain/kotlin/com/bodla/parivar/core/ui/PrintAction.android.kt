package com.bodla.parivar.core.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.unit.IntRect
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.core.view.WindowInsetsControllerCompat
import com.bodla.parivar.core.AndroidPlatform
import io.github.aakira.napier.Napier

actual fun PlatformPrint(content: String, title: String) {
    val activity = AndroidPlatform.currentActivityRef?.get()
    if (activity == null) {
        Napier.e("Cannot open print dialog without an active Activity", tag = "Print")
        return
    }
    val html = documentHtml(content, title)

    val runPrint = {
        try {
            val webView = WebView(activity).apply { settings.defaultTextEncodingName = "UTF-8" }
            var printed = false
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    if (printed) return
                    printed = true
                    view.post {
                        try {
                            val manager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                ?: error("Android print service is unavailable")
                            val mediaSize = if (isLandscapeDocument(content)) PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4
                            manager.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().setMediaSize(mediaSize).build())
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
    sharePdf(content, title)
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

private fun sharePdf(content: String, title: String) {
    val activity = AndroidPlatform.currentActivityRef?.get()
    val context = activity ?: AndroidPlatform.appContext ?: return
    val run = {
        try {
            renderPdf(context, content, title) { file ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, title)
                if (activity == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Napier.e("PDF share error", e, tag = "Share")
        }
    }
    if (Looper.myLooper() == Looper.getMainLooper()) run() else Handler(Looper.getMainLooper()).post(run)
}

private fun renderPdf(context: Context, content: String, title: String, onReady: (File) -> Unit) {
    val activity = AndroidPlatform.currentActivityRef?.get() ?: return
    val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
    val density = context.resources.displayMetrics.density
    val widthPx = (794 * density).toInt().coerceAtLeast(1)
    val webView = WebView(activity).apply {
        settings.defaultTextEncodingName = "UTF-8"
        setBackgroundColor(android.graphics.Color.WHITE)
        alpha = 0.01f
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    var completed = false
    fun cleanup() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            if (completed) return
            view.evaluateJavascript("document.documentElement.scrollHeight.toString()") { raw ->
                if (completed) return@evaluateJavascript
                val cssHeight = raw.trim('"').toFloatOrNull()?.toInt()?.coerceAtLeast(1) ?: 1123
                val heightPx = (cssHeight * density).toInt().coerceAtLeast(widthPx)
                view.measure(View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, widthPx, heightPx)
                val ready = Runnable {
                    if (completed) return@Runnable
                    completed = true
                    try {
                        val safeTitle = title.replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-').ifBlank { "Bodla-Parivar-Document" }
                        val file = File(context.cacheDir, "$safeTitle.pdf")
                        val landscape = isLandscapeDocument(content)
                        val pageWidth = if (landscape) 842 else 595
                        val pageHeight = if (landscape) 595 else 842
                        val scale = pageWidth.toFloat() / widthPx
                        val pageHeightPx = (pageHeight / scale).toInt().coerceAtLeast(1)
                        val pageCount = ((heightPx + pageHeightPx - 1) / pageHeightPx).coerceAtLeast(1)
                        val pdf = PdfDocument()
                        for (pageNumber in 0 until pageCount) {
                            val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber + 1).create())
                            page.canvas.save()
                            page.canvas.clipRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                            page.canvas.scale(scale, scale)
                            page.canvas.translate(0f, -(pageNumber * pageHeightPx).toFloat())
                            view.draw(page.canvas)
                            page.canvas.restore()
                            pdf.finishPage(page)
                        }
                        FileOutputStream(file).use { pdf.writeTo(it) }
                        pdf.close()
                        cleanup()
                        if (file.length() > 0L) onReady(file) else Napier.e("Generated PDF is empty", tag = "Share")
                    } catch (e: Exception) {
                        cleanup()
                        Napier.e("PDF generation failed", e, tag = "Share")
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    view.postVisualStateCallback(System.nanoTime(), object : WebView.VisualStateCallback() {
                        override fun onComplete(requestId: Long) { view.post(ready) }
                    })
                } else {
                    view.postDelayed(ready, 250)
                }
            }
        }
    }
    root.addView(webView, 0, ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT))
    webView.loadDataWithBaseURL(null, documentHtml(content, title), "text/html", "UTF-8", null)
}
private fun documentHtml(content: String, title: String): String {
    val escapedTitle = title.escapeHtml()
    val lines = content.lines()
    val tableLines = lines.filter { line -> line.contains(" | ") && !line.split("|").all { cell -> cell.trim().isNotEmpty() && cell.trim().all { it.code == 0x2500 || it == '-' } } }
    val detailLines = lines.filter { line -> !line.contains(" | ") && line.isNotBlank() && !line.trim().all { it.code == 0x2500 || it == '-' } }
    val bodyContent = if (tableLines.isNotEmpty()) {
        buildString {
            append("<table><thead><tr>")
            tableLines.first().split(" | ").forEach { append("<th>").append(it.escapeHtml()).append("</th>") }
            append("</tr></thead><tbody>")
            tableLines.drop(1).forEach { line ->
                append("<tr>")
                line.split(" | ").forEach { cell -> append("<td>").append(cell.trim().escapeHtml()).append("</td>") }
                append("</tr>")
            }
            append("</tbody></table>")
            if (detailLines.size > 1) {
                append("<section class=\"details\">")
                detailLines.drop(1).forEach { append("<p>").append(it.trim().escapeHtml()).append("</p>") }
                append("</section>")
            }
        }
    } else "<main class=\"content\">${content.escapeHtml()}</main>"
    val now = java.util.Date()
    val date = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH).format(now)
    val reference = java.text.SimpleDateFormat("yyyyMM", java.util.Locale.ENGLISH).format(now) + "-" + (1000..9999).random()
    return """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
        *{box-sizing:border-box}html,body{margin:0;background:#fff;color:#172033;font-family:Arial,'Noto Sans Gujarati',sans-serif}
        body{padding:30px;font-size:14px;line-height:1.5}.heading{border-bottom:2px solid #0F2744;padding-bottom:8px;margin-bottom:10px;display:flex;justify-content:space-between;align-items:flex-end}
        .brand{font-size:15px;font-weight:800;color:#0F2744;line-height:1.1}.subtitle{font-size:10px;color:#64748b;margin-top:2px}.title{font-size:13px;font-weight:700;color:#D97706;margin-top:4px;text-transform:uppercase;letter-spacing:.02em}
        .date{font-size:10px;color:#64748b;text-align:right;line-height:1.3}.reference{font-family:monospace;font-size:9px;color:#94a3b8}.content{white-space:pre-wrap;overflow-wrap:anywhere;font-size:13px;line-height:1.6}
        table{width:100%;border-collapse:collapse;font-size:10px;margin:12px 0}th{background:#0F2744;color:#fff;text-align:left}
        th,td{border:1px solid #cbd5e1;padding:6px;vertical-align:top}tbody tr:nth-child(even){background:#f8fafc}
        .details{font-size:10px;color:#475569}.details p{margin:4px 0}
        .footer{margin-top:10px;padding-top:7px;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:9px;display:flex;justify-content:space-between}
        </style></head><body><header class="heading"><div><div class="brand">બોદલા પરિવાર • BODLA PARIVAR</div><div class="subtitle">બોદલા ગ્રામિણ સમુદાય ટ્રસ્ટ • Mehsana, Gujarat</div><div class="title">$escapedTitle</div></div><div class="date"><div><strong>Date:</strong> $date</div><div class="reference">REF: $reference</div></div></header>
        $bodyContent<footer class="footer"><span>Official confidential community record • Bodla Parivar Portal</span><span>Authorized Digital Copy</span></footer></body></html>
    """.trimIndent()
}

private fun isLandscapeDocument(content: String): Boolean =
    content.lines().firstOrNull { it.contains(" | ") }?.split(" | ")?.size?.let { it > 6 } == true

actual fun PlatformSetSystemBarStyle(darkTheme: Boolean) {
    val activity = AndroidPlatform.currentActivityRef?.get() ?: return
    val window = activity.window
    window.statusBarColor = android.graphics.Color.rgb(255, 248, 232)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
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
