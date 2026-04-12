package com.avicennasis.bluepaper.config

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelSizeTest {

    @Test
    fun mmToPixelsAt203Dpi() {
        val size = LabelSize(widthMm = 30.0, heightMm = 15.0, dpi = 203)
        assertEquals(239, size.widthPx)
        assertEquals(119, size.heightPx)
    }

    @Test
    fun mmToPixelsAt300Dpi() {
        val size = LabelSize(widthMm = 30.0, heightMm = 14.0, dpi = 300)
        assertEquals(354, size.widthPx)
        assertEquals(165, size.heightPx)
    }

    @Test
    fun displayName() {
        val size = LabelSize(widthMm = 50.0, heightMm = 30.0, dpi = 203)
        assertEquals("50x30mm", size.displayName)
    }

    @Test
    fun fractionalMm() {
        val size = LabelSize(widthMm = 109.0, heightMm = 12.5, dpi = 203)
        assertEquals(871, size.widthPx)
        assertEquals(99, size.heightPx)
    }
}
