package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

actual object ImageEncoder {
    actual fun encode(bitmap: ImageBitmap, maxDimension: Int): String = ""
    actual fun decode(base64: String): ImageBitmap? = null
}
