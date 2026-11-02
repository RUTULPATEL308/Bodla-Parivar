package com.bodla.parivar.core.ui

import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIPrintFormatter
import platform.UIKit.UISimpleTextPrintFormatter
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.writeToFile
import platform.Foundation.NSUTF8StringEncoding
import androidx.compose.ui.unit.IntRect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIScreen

actual fun PlatformPrint(content: String, title: String) {
    val formatter: UIPrintFormatter = UISimpleTextPrintFormatter(text = content)
    val printInfo = UIPrintInfo.printInfo()
    printInfo.jobName = title
    val controller = UIPrintInteractionController.sharedPrintController()
    controller.printInfo = printInfo
    controller.printFormatter = formatter
    controller.presentAnimated(true, completionHandler = { _, _, _ -> })
}

actual fun PlatformShare(content: String, title: String) {
    val controller = UIActivityViewController(listOf(content), null)
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        controller, animated = true, completion = null
    )
}

actual fun PlatformShareFile(content: String, filename: String, mimeType: String) {
    val path = NSTemporaryDirectory() + filename
    (content as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val fileUrl = NSURL.fileURLWithPath(path)
    val controller = UIActivityViewController(listOf(fileUrl), null)
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        controller, animated = true, completion = null
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun PlatformCapturePoster(boundsInWindow: IntRect): ByteArray? {
    val window = UIApplication.sharedApplication.keyWindow ?: return null
    val scale = UIScreen.mainScreen.scale
    val left = boundsInWindow.left.toDouble() / scale
    val top = boundsInWindow.top.toDouble() / scale
    val width = boundsInWindow.width.toDouble() / scale
    val height = boundsInWindow.height.toDouble() / scale
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), false, scale)
    return try {
        val context = UIGraphicsGetCurrentContext() ?: return null
        CGContextTranslateCTM(context, -left, -top)
        window.drawViewHierarchyInRect(window.bounds, afterScreenUpdates = true)
        val image = UIGraphicsGetImageFromCurrentImageContext() ?: return null
        val png = UIImagePNGRepresentation(image) ?: return null
        ByteArray(png.length.toInt()).also { bytes ->
            bytes.usePinned { pinned -> png.getBytes(pinned.addressOf(0), png.length) }
        }
    } catch (error: Exception) {
        io.github.aakira.napier.Napier.e("Could not capture Antim Yatra poster", error, tag = "Share")
        null
    } finally {
        UIGraphicsEndImageContext()
    }
}

actual fun PlatformOpenUrl(url: String) {
    NSURL(string = url)?.let { UIApplication.sharedApplication.openURL(it) }
}
