package com.bodla.parivar.core.ui

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun PlatformShrinkPoster(png: ByteArray, maxEdgePx: Int): ByteArray {
    if (png.isEmpty()) return png
    return try {
        val data = png.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), png.size.toULong())
        }
        val image = UIImage(data = data) ?: return png
        val size = image.size.useContents { this }
        val longestEdge = maxOf(size.width * image.scale, size.height * image.scale)
        if (longestEdge <= maxEdgePx.toDouble()) return png

        val ratio = maxEdgePx.toDouble() / longestEdge
        val targetWidth = size.width * image.scale * ratio
        val targetHeight = size.height * image.scale * ratio

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, image.scale)
        val context = UIGraphicsGetCurrentContext()
        if (context == null) {
            UIGraphicsEndImageContext()
            return png
        }
        CGContextScaleCTM(context, ratio, ratio)
        image.drawInRect(CGRectMake(0.0, 0.0, size.width * image.scale, size.height * image.scale))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        val resizedData = resized?.let { UIImagePNGRepresentation(it) } ?: return png
        val resizedBytes = ByteArray(resizedData.length.toInt())
        resizedBytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), resizedData.bytes, resizedData.length)
        }
        resizedBytes
    } catch (error: Exception) {
        Napier.e("Could not shrink the Antim Yatra poster", error, tag = "Share")
        png
    }
}
