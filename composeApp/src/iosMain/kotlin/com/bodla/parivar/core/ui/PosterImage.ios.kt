package com.bodla.parivar.core.ui

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.imageWithData
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun PlatformShrinkPoster(png: ByteArray, maxEdgePx: Int): ByteArray {
    if (png.isEmpty()) return png
    return try {
        val data = NSData.dataWithBytes(png.refTo(0), png.size.toULong())
        val image = UIImage.imageWithData(data) ?: return png
        val longestEdge = maxOf(image.size.width * image.scale, image.size.height * image.scale)
        if (longestEdge <= maxEdgePx) return png

        val ratio = maxEdgePx / longestEdge
        val targetWidth = image.size.width * ratio
        val targetHeight = image.size.height * ratio

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, image.scale)
        val context = UIGraphicsGetCurrentContext()
        if (context == null) {
            UIGraphicsEndImageContext()
            return png
        }
        CGContextScaleCTM(context, ratio, ratio)
        image.drawInRect(CGRectMake(0.0, 0.0, image.size.width, image.size.height))
        val resized = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        val resizedData = resized?.let { UIImagePNGRepresentation(it) } ?: return png
        ByteArray(resizedData.length.toInt()).also { bytes ->
            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), resizedData.bytes, resizedData.length) }
        }
    } catch (error: Exception) {
        Napier.e("Could not shrink the Antim Yatra poster", error, tag = "Share")
        png
    }
}
