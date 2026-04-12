package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Renders packed 1-bit monochrome rows as a black/white bitmap preview.
 * Shows exactly what the printer will output.
 */
@Composable
fun MonochromePreview(
    rows: List<ByteArray>,
    width: Int,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty() || width <= 0) return

    val height = rows.size
    val ratio = width.toFloat() / height.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        // White background
        drawRect(Color.White)

        val scaleX = size.width / width
        val scaleY = size.height / height

        // Draw black pixels using horizontal run-length encoding for efficiency
        for (y in rows.indices) {
            val row = rows[y]
            var runStart = -1

            for (x in 0 until width) {
                val byteIndex = x / 8
                val bitIndex = 7 - (x % 8)
                val bit = if (byteIndex < row.size) {
                    (row[byteIndex].toInt() shr bitIndex) and 1
                } else {
                    0
                }

                if (bit == 1 && runStart == -1) {
                    runStart = x
                } else if (bit == 0 && runStart != -1) {
                    drawRect(
                        Color.Black,
                        topLeft = Offset(runStart * scaleX, y * scaleY),
                        size = Size((x - runStart) * scaleX, scaleY),
                    )
                    runStart = -1
                }
            }
            // Close any open run at end of row
            if (runStart != -1) {
                drawRect(
                    Color.Black,
                    topLeft = Offset(runStart * scaleX, y * scaleY),
                    size = Size((width - runStart) * scaleX, scaleY),
                )
            }
        }
    }
}
