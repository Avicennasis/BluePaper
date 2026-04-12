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
    fun threshold128IsExact() {
        // gray(128): luminance rounds to 127 via float truncation → inverted=128 → ON (black)
        // gray(129): luminance rounds to 129 → inverted=126 → OFF (white)
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
}
