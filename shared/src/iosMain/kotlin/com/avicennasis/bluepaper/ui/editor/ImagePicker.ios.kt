package com.avicennasis.bluepaper.ui.editor

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

private fun bytesToImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private class ImagePickerDelegate(
    private val callback: (ImageBitmap?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            callback(null)
            return
        }
        val provider = result.itemProvider
        if (provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
            provider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data, _ ->
                if (data != null) {
                    val bitmap = bytesToImageBitmap(data.toByteArray())
                    callback(bitmap)
                } else {
                    callback(null)
                }
            }
        } else {
            callback(null)
        }
    }
}

@Composable
actual fun ImagePickerButton(
    onImageLoaded: (ImageBitmap) -> Unit,
    modifier: Modifier,
) {
    val delegate = remember { ImagePickerDelegate { bitmap -> bitmap?.let(onImageLoaded) } }

    OutlinedButton(
        onClick = {
            val config = PHPickerConfiguration()
            config.setFilter(PHPickerFilter.imagesFilter)
            config.setSelectionLimit(1)
            val picker = PHPickerViewController(configuration = config)
            picker.setDelegate(delegate)
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        },
        modifier = modifier,
    ) {
        Text("+ Image")
    }
}
