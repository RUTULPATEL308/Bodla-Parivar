package com.bodla.parivar.core.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.bodla.parivar.core.AndroidPlatform

actual fun PlatformPrint(content: String, title: String) {
    val activity = AndroidPlatform.currentActivityRef?.get()
    val context = activity ?: AndroidPlatform.appContext ?: return
    val html = "<html><meta charset=\"utf-8\"><head><style>@font-face{font-family:AnekGujarati;src:url('file:///android_res/font/anek_gujarati.ttf')}body{font-family:AnekGujarati,sans-serif;padding:16px;white-space:pre-wrap;font-size:12pt;line-height:1.45}</style></head><body>${content.escapeHtml()}</body></html>"

    val runPrint = {
        try {
            val webView = WebView(context)
            var printed = false
            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    if (printed) return
                    printed = true
                    view.post {
                        try {
                            val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            manager?.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
                        } catch (e: Exception) {
                            io.github.aakira.napier.Napier.e("PlatformPrint error", e, tag = "Print")
                        }
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        } catch (e: Exception) {
            io.github.aakira.napier.Napier.e("Failed to initialize WebView for printing", e, tag = "Print")
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
            io.github.aakira.napier.Napier.e("PlatformShare error", e, tag = "Share")
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        runShare()
    } else {
        Handler(Looper.getMainLooper()).post(runShare)
    }
}
