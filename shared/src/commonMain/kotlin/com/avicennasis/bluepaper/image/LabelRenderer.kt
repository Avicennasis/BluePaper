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

        // Debug: check pixel values
        var transparentCount = 0  // alpha = 0
        var whiteCount = 0        // 0xFFFFFFFF
        var blackCount = 0        // 0xFF000000
        var otherCount = 0
        for (p in pixels) {
            val alpha = (p.toUInt() shr 24).toInt()
            if (alpha == 0) transparentCount++
            else if (p == -1) whiteCount++  // 0xFFFFFFFF
            else if (p == 0xFF000000.toInt()) blackCount++
            else otherCount++
        }
        println("[LabelRenderer] Canvas ${width}x${height}, pixels: total=${pixels.size}")
        println("[LabelRenderer]   transparent=$transparentCount, white=$whiteCount, black=$blackCount, other=$otherCount")
        // Sample pixels from different regions
        if (pixels.size > 100) {
            val corner = pixels.take(5).joinToString { "0x${it.toUInt().toString(16)}" }
            val mid = pixels.slice(pixels.size/2 until pixels.size/2 + 5).joinToString { "0x${it.toUInt().toString(16)}" }
            println("[LabelRenderer] Corner pixels: $corner")
            println("[LabelRenderer] Middle pixels: $mid")
        }

        val (rotatedPixels, rotatedWidth, rotatedHeight) =
            MonochromeEncoder.rotatePixels(pixels, width, height, rotationDegrees)

        println("[LabelRenderer] After rotation: ${rotatedWidth}x${rotatedHeight}")

        return MonochromeEncoder.encode(rotatedPixels, rotatedWidth, rotatedHeight, horizontalOffset, verticalOffset)
    }
}
