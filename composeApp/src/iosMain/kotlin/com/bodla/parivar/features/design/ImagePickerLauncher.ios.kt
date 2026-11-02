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
import platform.Foundation.NSDataReadingMappedIfSafe
import platform.Foundation.NSURL
import platform.Foundation.NSObject
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerSourceTypePhotoLibrary
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.posix.memcpy

private class ImagePickerDelegate(
    private val onPicked: (ByteArray?) -> Unit,
    private val onCancelled: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo["UIImagePickerControllerOriginalImage"] as? UIImage
        val data = image?.let { UIImageJPEGRepresentation(it, 0.9) }
        val bytes = data?.let { source ->
            ByteArray(source.length.toInt()).also { target ->
                if (target.isNotEmpty()) target.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), source.bytes, source.length)
                }
            }
        }
        onPicked(bytes)
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onCancelled()
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

private class ResultDocumentPickerDelegate(
    private val onPicked: (ResultDocument?) -> Unit,
    private val onCancelled: () -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val bytes = url?.let { NSData(contentsOfURL = it, options = NSDataReadingMappedIfSafe, error = null) }
        val result = if (url != null && bytes != null) {
            val contentType = when (url.pathExtension?.lowercase()) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val documentBytes = ByteArray(bytes.length.toInt()).also { target ->
                if (target.isNotEmpty()) target.usePinned { pinned ->
                    memcpy(pinned.addressOf(0), bytes.bytes, bytes.length)
                }
            }
            ResultDocument(documentBytes, url.lastPathComponent ?: "result", contentType)
        } else null
        onPicked(result)
        controller.dismissViewControllerAnimated(true, completion = null)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
        controller.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ImagePickerButton(label: String, modifier: Modifier, onImagePicked: (ByteArray?) -> Unit) {
    val parent = LocalUIViewController.current
    TextButton(onClick = {
        val picker = UIImagePickerController()
        picker.delegate = ImagePickerDelegate(onPicked = onImagePicked, onCancelled = {})
        picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary
        parent.presentViewController(picker, animated = true, completion = null)
    }, modifier = modifier) { Text(label) }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ResultDocumentPicker(label: String, modifier: Modifier, onDocumentPicked: (ResultDocument?) -> Unit) {
    val parent = LocalUIViewController.current
    TextButton(onClick = {
        val picker = UIDocumentPickerViewController(documentTypes = listOf("public.image", "com.adobe.pdf"), inMode = UIDocumentPickerModeImport)
        picker.delegate = ResultDocumentPickerDelegate(onPicked = onDocumentPicked, onCancelled = {})
        parent.presentViewController(picker, animated = true, completion = null)
    }, modifier = modifier) { Text(label) }
}
