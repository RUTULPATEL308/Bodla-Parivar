package com.bodla.parivar.core.ui

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
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
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun PlatformShrinkPoster(png: ByteArray, maxEdgePx: Int): ByteArray {
    if (png.isEmpty()) return png
    return try {
        val data = png.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), png.size.toULong())
        }
        val image = UIImage.imageWithData(data) ?: return png
        val imageWidth = image.size.useContents { width }
        val imageHeight = image.size.useContents { height }
        val longestEdge = maxOf(imageWidth * image.scale, imageHeight * image.scale)
        if (longestEdge <= maxEdgePx) return png

        val ratio = maxEdgePx / longestEdge
        val targetWidth = imageWidth * ratio
        val targetHeight = imageHeight * ratio

        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, image.scale)
        val context = UIGraphicsGetCurrentContext()
        if (context == null) {
            UIGraphicsEndImageContext()
            return png
        }
        CGContextScaleCTM(context, ratio, ratio)
        image.drawInRect(CGRectMake(0.0, 0.0, imageWidth, imageHeight))
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
