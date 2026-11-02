package com.bodla.parivar.core.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.aakira.napier.Napier
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

actual fun PlatformShrinkPoster(png: ByteArray, maxEdgePx: Int): ByteArray {
    if (png.isEmpty()) return png
    return try {
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size) ?: return png
        val longestEdge = max(bitmap.width, bitmap.height)
        if (longestEdge <= maxEdgePx) {
            bitmap.recycle()
            return png
        }
        val ratio = maxEdgePx.toFloat() / longestEdge
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).roundToInt().coerceAtLeast(1),
            (bitmap.height * ratio).roundToInt().coerceAtLeast(1),
            true
        )
        if (scaled !== bitmap) bitmap.recycle()
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
            scaled.recycle()
            output.toByteArray()
        }
    } catch (error: Exception) {
        Napier.e("Could not shrink the Antim Yatra poster", error, tag = "Share")
        png
    }
}
