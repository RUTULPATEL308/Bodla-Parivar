package com.bodla.parivar.features.design

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImage
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.Foundation.NSURL
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.Foundation.dataWithContentsOfURL
import platform.darwin.NSObject
import platform.posix.memcpy
import platform.UIKit.UIDocumentPickerMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ImagePickerButton(label: String, modifier: Modifier, onImagePicked: (ByteArray?) -> Unit) {
    val parent = LocalUIViewController.current
    TextButton(onClick = {
        val picker = UIImagePickerController()
        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo["UIImagePickerControllerOriginalImage"] as? UIImage
                val data: NSData? = image?.let { UIImageJPEGRepresentation(it, 0.9) }
                val bytes = data?.let { source -> ByteArray(source.length.toInt()).also { target ->
                    if (target.isNotEmpty()) target.usePinned { memcpy(it.addressOf(0), source.bytes, source.length) }
                } }
                onImagePicked(bytes)
                picker.dismissViewControllerAnimated(true, completion = null)
            }
            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, completion = null)
            }
        }
        picker.delegate = delegate
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        parent.presentViewController(picker, animated = true, completion = null)
    }, modifier = modifier) { Text(label) }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ResultDocumentPicker(label: String, modifier: Modifier, onDocumentPicked: (ResultDocument?) -> Unit) {
    val parent = LocalUIViewController.current
    TextButton(onClick = {
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.image", "com.adobe.pdf"),
            inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
        )
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val bytes = url?.let { NSData.dataWithContentsOfURL(it) }
                val result = if (url != null && bytes != null) {
                    val contentType = when (url.pathExtension?.lowercase()) {
                        "pdf" -> "application/pdf"
                        "png" -> "image/png"
                        "webp" -> "image/webp"
                        else -> "image/jpeg"
                    }
                    ResultDocument(ByteArray(bytes.length.toInt()).also { target ->
                        if (target.isNotEmpty()) target.usePinned { memcpy(it.addressOf(0), bytes.bytes, bytes.length) }
                    }, url.lastPathComponent ?: "result", contentType)
                } else null
                onDocumentPicked(result)
                controller.dismissViewControllerAnimated(true, completion = null)
            }
            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                controller.dismissViewControllerAnimated(true, completion = null)
            }
        }
        picker.delegate = delegate
        parent.presentViewController(picker, animated = true, completion = null)
    }, modifier = modifier) { Text(label) }
}
