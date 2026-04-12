package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

expect object BarcodeRenderer {
    fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection = ErrorCorrection.M,
    ): ImageBitmap?

    fun isFormatAvailable(format: BarcodeFormat): Boolean
}
