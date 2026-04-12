package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

actual object BarcodeRenderer {
    actual fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? = null

    actual fun isFormatAvailable(format: BarcodeFormat): Boolean = false
}
