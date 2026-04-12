package com.avicennasis.bluepaper.image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object LabelRenderer {

    fun render(
        width: Int,
        height: Int,
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

        val pixels = IntArray(width * height)
        bitmap.readPixels(
            buffer = pixels,
            startX = 0,
            startY = 0,
            width = width,
            height = height,
            bufferOffset = 0,
            stride = width,
        )

        return MonochromeEncoder.encode(pixels, width, height, horizontalOffset, verticalOffset)
    }
}
