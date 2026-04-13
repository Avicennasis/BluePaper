package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WidthClampingTest {

    @Test
    fun elementWithinBoundsUnchanged() {
        val el = LabelElement.TextElement(id = "t1", x = 10f, y = 10f, width = 50f, height = 30f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(10f, clamped.x)
        assertEquals(10f, clamped.y)
        assertEquals(50f, clamped.width)
        assertEquals(30f, clamped.height)
    }

    @Test
    fun elementOverflowingRightClamped() {
        val el = LabelElement.TextElement(id = "t1", x = 200f, y = 10f, width = 100f, height = 30f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(140f, clamped.x)  // 240 - 100 = 140
    }

    @Test
    fun elementOverflowingBottomClamped() {
        val el = LabelElement.TextElement(id = "t1", x = 10f, y = 100f, width = 50f, height = 50f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(70f, clamped.y)  // 120 - 50 = 70
    }

    @Test
    fun negativePositionClamped() {
        val el = LabelElement.TextElement(id = "t1", x = -50f, y = -30f, width = 50f, height = 30f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(0f, clamped.x)
        assertEquals(0f, clamped.y)
    }

    @Test
    fun widthTooLargeClamped() {
        val el = LabelElement.TextElement(id = "t1", x = 0f, y = 0f, width = 500f, height = 30f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(240f, clamped.width)
    }

    @Test
    fun widthTooSmallClamped() {
        val el = LabelElement.TextElement(id = "t1", x = 0f, y = 0f, width = 2f, height = 2f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertEquals(MIN_ELEMENT_SIZE, clamped.width)
        assertEquals(MIN_ELEMENT_SIZE, clamped.height)
    }

    @Test
    fun barcodeElementClamped() {
        val el = LabelElement.BarcodeElement(id = "b1", x = 300f, y = 200f, width = 100f, height = 100f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertTrue(clamped.x <= 240f - clamped.width)
        assertTrue(clamped.y <= 120f - clamped.height)
    }

    @Test
    fun clampAllClampsList() {
        val els = listOf(
            LabelElement.TextElement(id = "t1", x = -10f, y = 0f, width = 50f, height = 30f),
            LabelElement.TextElement(id = "t2", x = 300f, y = 0f, width = 50f, height = 30f),
        )
        val clamped = clampAllToLabel(els, labelWidth = 240, labelHeight = 120)
        assertEquals(0f, clamped[0].x)
        assertEquals(190f, clamped[1].x) // 240 - 50
    }

    @Test
    fun imageElementClamped() {
        val el = LabelElement.ImageElement(id = "i1", x = 250f, y = 0f, width = 100f, height = 50f)
        val clamped = clampToLabel(el, labelWidth = 240, labelHeight = 120)
        assertTrue(clamped is LabelElement.ImageElement)
        assertEquals(140f, clamped.x) // 240 - 100
    }
}
