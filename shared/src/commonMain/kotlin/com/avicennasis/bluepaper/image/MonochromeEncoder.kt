package com.avicennasis.bluepaper.image

import kotlin.math.ceil

object MonochromeEncoder {

    fun bytesPerRow(width: Int): Int = ceil(width.toDouble() / 8.0).toInt()

    /**
     * Rotate ARGB pixel data by the given degrees (0, 90, 180, 270, or negative equivalents).
     * Returns the rotated pixels and the new (width, height).
     */
    fun rotatePixels(pixels: IntArray, width: Int, height: Int, degrees: Int): Triple<IntArray, Int, Int> {
        val normalizedDegrees = ((degrees % 360) + 360) % 360
        if (normalizedDegrees == 0) return Triple(pixels, width, height)

        val (newWidth, newHeight) = when (normalizedDegrees) {
            90, 270 -> height to width
            180 -> width to height
            else -> return Triple(pixels, width, height)
        }

        val result = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (newX, newY) = when (normalizedDegrees) {
                    90 -> (height - 1 - y) to x
                    180 -> (width - 1 - x) to (height - 1 - y)
                    270 -> y to (width - 1 - x)
                    else -> x to y
                }
                result[newY * newWidth + newX] = pixels[y * width + x]
            }
        }
        return Triple(result, newWidth, newHeight)
    }

    fun encode(
        pixels: IntArray,
        width: Int,
        height: Int,
        horizontalOffset: Int = 0,
        verticalOffset: Int = 0,
        maxWidth: Int = Int.MAX_VALUE,
    ): List<ByteArray> {
        if (width <= 0 || height <= 0) return emptyList()

        val srcStartRow: Int
        val blankRowsTop: Int
        val effectiveHeight: Int

        if (verticalOffset >= 0) {
            srcStartRow = 0
            blankRowsTop = verticalOffset
            effectiveHeight = height + verticalOffset
        } else {
            srcStartRow = -verticalOffset
            blankRowsTop = 0
            effectiveHeight = height + verticalOffset
        }

        if (effectiveHeight <= 0) return emptyList()

        val srcStartCol: Int
        val blankColsLeft: Int
        val effectiveWidth: Int

        if (horizontalOffset >= 0) {
            srcStartCol = 0
            blankColsLeft = horizontalOffset
            effectiveWidth = (width + horizontalOffset).coerceAtMost(maxWidth)
        } else {
            srcStartCol = -horizontalOffset
            blankColsLeft = 0
            effectiveWidth = width + horizontalOffset
        }

        if (effectiveWidth <= 0) return emptyList()

        val bpr = bytesPerRow(effectiveWidth)
        val rows = mutableListOf<ByteArray>()

        for (outY in 0 until effectiveHeight) {
            val packed = ByteArray(bpr)

            if (outY < blankRowsTop) {
                rows.add(packed)
                continue
            }

            val srcY = srcStartRow + (outY - blankRowsTop)
            if (srcY >= height) {
                rows.add(packed)
                continue
            }

            for (outX in 0 until effectiveWidth) {
                val bit: Int
                if (outX < blankColsLeft) {
                    bit = 0
                } else {
                    val srcX = srcStartCol + (outX - blankColsLeft)
                    if (srcX >= width) {
                        bit = 0
                    } else {
                        val argb = pixels[srcY * width + srcX]
                        val r = (argb shr 16) and 0xFF
                        val g = (argb shr 8) and 0xFF
                        val b = argb and 0xFF
                        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        val inverted = 255 - gray
                        bit = if (inverted >= 128) 1 else 0
                    }
                }

                if (bit == 1) {
                    val byteIndex = outX / 8
                    val bitIndex = 7 - (outX % 8)
                    packed[byteIndex] = (packed[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }

            rows.add(packed)
        }

        return rows
    }
}
