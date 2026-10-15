package com.bodla.parivar.core.ui

import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInteractionController
import platform.UIKit.UIPrintFormatter
import platform.UIKit.UISimpleTextPrintFormatter

actual fun PlatformPrint(content: String, title: String) {
    val formatter: UIPrintFormatter = UISimpleTextPrintFormatter(text = content)
    val printInfo = UIPrintInfo.printInfo()
    printInfo.jobName = title
    val controller = UIPrintInteractionController.sharedPrintController()
    controller.printInfo = printInfo
    controller.printFormatter = formatter
    controller.presentAnimated(true, completionHandler = { _, _, _ -> })
}
