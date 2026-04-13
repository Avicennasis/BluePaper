package com.avicennasis.bluepaper.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class MonochromeEncoderTest {

    private fun gray(value: Int): Int = (0xFF shl 24) or (value shl 16) or (value shl 8) or value
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun allBlackPixelsProduceAllOnes() {
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertEquals(1, rows.size)
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[0])
    }

    @Test
    fun allWhitePixelsProduceAllZeros() {
        val pixels = IntArray(8) { gray(255) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertEquals(1, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0])
    }

    @Test
    fun alternatingBlackWhite() {
        val pixels = IntArray(8) { i -> if (i % 2 == 0) gray(0) else gray(255) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertContentEquals(byteArrayOf(0xAA.toByte()), rows[0])
    }

    @Test
    fun widthNotMultipleOf8PadsWithZeros() {
        val pixels = IntArray(10) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 10, height = 1)
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(0xFF.toByte(), rows[0][0])
        assertEquals(0xC0.toByte(), rows[0][1])
    }

    @Test
    fun multipleRows() {
        val pixels = IntArray(16)
        for (i in 0 until 8) pixels[i] = gray(0)
        for (i in 8 until 16) pixels[i] = gray(255)
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 2)
        assertEquals(2, rows.size)
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[0])
        assertContentEquals(byteArrayOf(0x00), rows[1])
    }

    @Test
    fun grayscaleConversionFromColor() {
        val pixels = intArrayOf(rgb(255, 0, 0), rgb(0, 0, 255), rgb(0, 255, 0), gray(255))
        val rows = MonochromeEncoder.encode(pixels, width = 4, height = 1)
        assertEquals(1, rows.size)
        assertEquals(0xC0.toByte(), rows[0][0])
    }

    @Test
    fun thresholdBoundary() {
        // gray(128): luminance truncates to 127 via float rounding → inverted=128 → ON (black)
        // gray(129): luminance truncates to 128 → inverted=127 → OFF (white)
        val pixels = intArrayOf(gray(128), gray(129), gray(128), gray(129), gray(128), gray(129), gray(128), gray(129))
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertContentEquals(byteArrayOf(0xAA.toByte()), rows[0])
    }

    @Test
    fun bytesPerRowCalculation() {
        assertEquals(1, MonochromeEncoder.bytesPerRow(1))
        assertEquals(1, MonochromeEncoder.bytesPerRow(8))
        assertEquals(2, MonochromeEncoder.bytesPerRow(9))
        assertEquals(2, MonochromeEncoder.bytesPerRow(16))
        assertEquals(30, MonochromeEncoder.bytesPerRow(240))
        assertEquals(48, MonochromeEncoder.bytesPerRow(384))
    }

    @Test
    fun emptyImageReturnsEmptyList() {
        val rows = MonochromeEncoder.encode(IntArray(0), width = 0, height = 0)
        assertEquals(0, rows.size)
    }

    @Test
    fun singlePixelBlack() {
        val pixels = intArrayOf(gray(0))
        val rows = MonochromeEncoder.encode(pixels, width = 1, height = 1)
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size)
        assertEquals(0x80.toByte(), rows[0][0])
    }

    @Test
    fun sixteenPixelWidth() {
        val pixels = IntArray(16) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 16, height = 1)
        assertEquals(2, rows[0].size)
        assertEquals(0xFF.toByte(), rows[0][0])
        assertEquals(0xFF.toByte(), rows[0][1])
    }

    @Test
    fun positiveHorizontalOffset() {
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, horizontalOffset = 4)
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(0x0F.toByte(), rows[0][0])
        assertEquals(0xF0.toByte(), rows[0][1])
    }

    @Test
    fun negativeHorizontalOffset() {
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, horizontalOffset = -2)
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size)
        assertEquals(0xFC.toByte(), rows[0][0])
    }

    @Test
    fun positiveVerticalOffset() {
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, verticalOffset = 2)
        assertEquals(3, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0])
        assertContentEquals(byteArrayOf(0x00), rows[1])
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[2])
    }

    @Test
    fun negativeVerticalOffset() {
        val pixels = IntArray(24)
        for (i in 0 until 8) pixels[i] = gray(0)
        for (i in 8 until 16) pixels[i] = gray(255)
        for (i in 16 until 24) pixels[i] = gray(0)
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 3, verticalOffset = -1)
        assertEquals(2, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0])
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[1])
    }

    // ---- Rotation Tests ----

    @Test
    fun rotate0DegreesIsIdentity() {
        // 2x3 grid: pixels [A, B / C, D / E, F]
        val pixels = intArrayOf(1, 2, 3, 4, 5, 6)
        val (rotated, w, h) = MonochromeEncoder.rotatePixels(pixels, 2, 3, 0)
        assertEquals(2, w)
        assertEquals(3, h)
        assertContentEquals(intArrayOf(1, 2, 3, 4, 5, 6), rotated)
    }

    @Test
    fun rotate90CW() {
        // 2x3 grid:      90° CW → 3x2 grid:
        // [1, 2]          [5, 3, 1]
        // [3, 4]          [6, 4, 2]
        // [5, 6]
        val pixels = intArrayOf(1, 2, 3, 4, 5, 6)
        val (rotated, w, h) = MonochromeEncoder.rotatePixels(pixels, 2, 3, 90)
        assertEquals(3, w)
        assertEquals(2, h)
        assertContentEquals(intArrayOf(5, 3, 1, 6, 4, 2), rotated)
    }

    @Test
    fun rotate180() {
        // 2x3 grid:      180° → 2x3 grid:
        // [1, 2]          [6, 5]
        // [3, 4]          [4, 3]
        // [5, 6]          [2, 1]
        val pixels = intArrayOf(1, 2, 3, 4, 5, 6)
        val (rotated, w, h) = MonochromeEncoder.rotatePixels(pixels, 2, 3, 180)
        assertEquals(2, w)
        assertEquals(3, h)
        assertContentEquals(intArrayOf(6, 5, 4, 3, 2, 1), rotated)
    }

    @Test
    fun rotate270CW() {
        // 2x3 grid:      270° CW (= 90° CCW) → 3x2 grid:
        // [1, 2]          [2, 4, 6]
        // [3, 4]          [1, 3, 5]
        // [5, 6]
        val pixels = intArrayOf(1, 2, 3, 4, 5, 6)
        val (rotated, w, h) = MonochromeEncoder.rotatePixels(pixels, 2, 3, 270)
        assertEquals(3, w)
        assertEquals(2, h)
        assertContentEquals(intArrayOf(2, 4, 6, 1, 3, 5), rotated)
    }

    @Test
    fun rotateNeg90IsSameAs270() {
        // -90° should be identical to 270° CW
        val pixels = intArrayOf(1, 2, 3, 4, 5, 6)
        val (r270, w270, h270) = MonochromeEncoder.rotatePixels(pixels, 2, 3, 270)
        val (rNeg90, wNeg90, hNeg90) = MonochromeEncoder.rotatePixels(pixels, 2, 3, -90)
        assertEquals(w270, wNeg90)
        assertEquals(h270, hNeg90)
        assertContentEquals(r270, rNeg90)
    }

    @Test
    fun rotate90SwapsDimensions() {
        // 4 wide x 2 tall → 2 wide x 4 tall
        val pixels = IntArray(8) { it }
        val (_, w, h) = MonochromeEncoder.rotatePixels(pixels, 4, 2, 90)
        assertEquals(2, w)
        assertEquals(4, h)
    }

    @Test
    fun rotate1x1IsIdentity() {
        val pixels = intArrayOf(42)
        val (rotated, w, h) = MonochromeEncoder.rotatePixels(pixels, 1, 1, 90)
        assertEquals(1, w)
        assertEquals(1, h)
        assertContentEquals(intArrayOf(42), rotated)
    }
}
