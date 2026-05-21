package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

private fun presentOnRoot(viewController: platform.UIKit.UIViewController) {
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(viewController, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        try {
            val tempDir = NSTemporaryDirectory()
            val tempUrl = NSURL.fileURLWithPath(tempDir + defaultName)
            val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            data?.writeToURL(tempUrl, atomically = true)

            val bplType = UTType.typeWithFilenameExtension("bpl") ?: UTTypeData
            val picker = UIDocumentPickerViewController(
                forExportingURLs = listOf(tempUrl),
                asCopy = true,
            )

            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>,
                ) {
                    onDone()
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDone()
                }
            }
            picker.delegate = delegate
            presentOnRoot(picker)
        } catch (_: Exception) {
            onDone()
        }
    }
}

@Composable
actual fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        try {
            val bplType = UTType.typeWithFilenameExtension("bpl") ?: UTTypeData
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(bplType),
                asCopy = true,
            )

            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>,
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (url != null) {
                        val json = NSString.stringWithContentsOfURL(
                            url,
                            encoding = NSUTF8StringEncoding,
                            error = null,
                        )
                        if (json != null) {
                            onLoaded(json)
                        }
                    }
                    onDone()
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDone()
                }
            }
            picker.delegate = delegate
            presentOnRoot(picker)
        } catch (_: Exception) {
            onDone()
        }
    }
}
