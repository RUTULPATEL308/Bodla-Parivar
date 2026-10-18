package com.bodla.parivar.core.ui

import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIPrintFormatter
import platform.UIKit.UISimpleTextPrintFormatter
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

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
