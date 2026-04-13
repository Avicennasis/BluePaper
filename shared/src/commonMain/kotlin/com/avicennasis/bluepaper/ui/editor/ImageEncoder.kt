package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

expect object ImageEncoder {
    /**
     * Encode an ImageBitmap as base64 PNG, downscaling so the largest dimension
     * doesn't exceed maxDimension.
     */
    fun encode(bitmap: ImageBitmap, maxDimension: Int): String

    /**
     * Decode a base64 PNG string back to an ImageBitmap.
     * Returns null if the data is invalid.
     */
    fun decode(base64: String): ImageBitmap?
}
