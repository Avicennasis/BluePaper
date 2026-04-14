package com.avicennasis.bluepaper.image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object LabelRenderer {

    /**
     * @param rotationDegrees Device rotation applied before encoding (e.g. -90 for D-series).
     *   Rotation swaps width/height — the returned rows have the rotated dimensions.
     */
    fun render(
        width: Int,
        height: Int,
        rotationDegrees: Int = 0,
        horizontalOffset: Int = 0,
        verticalOffset: Int = 0,
        draw: (DrawScope) -> Unit,
    ): List<ByteArray> {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val scope = CanvasDrawScope()
        scope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
        ) {
            draw(this)
        }

        var pixels = IntArray(width * height)
        bitmap.readPixels(
            buffer = pixels,
            startX = 0,
            startY = 0,
            width = width,
            height = height,
            bufferOffset = 0,
            stride = width,
        )

        // Debug: check how many non-white pixels we have
        var nonWhiteCount = 0
        var nonZeroCount = 0
        for (p in pixels) {
            if (p != 0) nonZeroCount++
            if (p != -1 && p != 0) nonWhiteCount++  // -1 is 0xFFFFFFFF (white in ARGB)
        }
        println("[LabelRenderer] Canvas ${width}x${height}, pixels: total=${pixels.size}, nonZero=$nonZeroCount, nonWhite=$nonWhiteCount")
        if (pixels.isNotEmpty()) {
            val sample = pixels.take(10).joinToString { "0x${it.toUInt().toString(16)}" }
            println("[LabelRenderer] First 10 pixels: $sample")
        }

        val (rotatedPixels, rotatedWidth, rotatedHeight) =
            MonochromeEncoder.rotatePixels(pixels, width, height, rotationDegrees)

        println("[LabelRenderer] After rotation: ${rotatedWidth}x${rotatedHeight}")

        return MonochromeEncoder.encode(rotatedPixels, rotatedWidth, rotatedHeight, horizontalOffset, verticalOffset)
    }
}
