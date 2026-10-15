package com.bodla.parivar.core.ui

import android.content.Context
import android.os.Handler
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.os.Looper
import com.bodla.parivar.core.AndroidPlatform

actual fun PlatformPrint(content: String, title: String) {
    val context = AndroidPlatform.appContext ?: return
    val html = "<html><meta charset=\"utf-8\"><body style=\"font-family:sans-serif;padding:24px;white-space:pre-wrap\">${content.escapeHtml()}</body></html>"
    val webView = WebView(context)
    var printed = false
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            if (printed) return
            printed = true
            view.post {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(title, view.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
            }
        }
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    } else {
        Handler(Looper.getMainLooper()).post {
            webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
        }
    }
}

private fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
