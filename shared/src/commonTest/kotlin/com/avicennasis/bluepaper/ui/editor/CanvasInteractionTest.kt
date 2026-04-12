package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CanvasInteractionTest {

    private val elements = listOf(
        LabelElement.TextElement(id = "t1", x = 10f, y = 10f, width = 100f, height = 50f),
        LabelElement.TextElement(id = "t2", x = 80f, y = 40f, width = 100f, height = 50f),
    )

    @Test
    fun hitTestFindsTopElement() {
        val hit = hitTest(elements, 90f, 50f)
        assertEquals("t2", hit?.id)
    }

    @Test
    fun hitTestFindsOnlyMatchingElement() {
        val hit = hitTest(elements, 20f, 20f)
        assertEquals("t1", hit?.id)
    }

    @Test
    fun hitTestMissReturnsNull() {
        val hit = hitTest(elements, 300f, 300f)
        assertNull(hit)
    }

    @Test
    fun hitTestOnBoundary() {
        val hit = hitTest(elements, 10f, 10f)
        assertEquals("t1", hit?.id)
    }

    @Test
    fun snapToGridRoundsCorrectly() {
        assertEquals(0f, snapToGrid(3f, 8f))
        assertEquals(8f, snapToGrid(5f, 8f))
        assertEquals(8f, snapToGrid(8f, 8f))
        assertEquals(8f, snapToGrid(11f, 8f))
        assertEquals(16f, snapToGrid(13f, 8f))
    }

    @Test
    fun snapToGridDisabled() {
        assertEquals(3.7f, snapToGrid(3.7f, 0f))
    }

    @Test
    fun screenToLabelCoordinates() {
        val (lx, ly) = screenToLabel(100f, 50f, canvasWidth = 400f, canvasHeight = 200f, labelWidth = 200, labelHeight = 100)
        assertEquals(50f, lx)
        assertEquals(25f, ly)
    }

    @Test
    fun screenToLabelWithNonUniformAspect() {
        val (lx, ly) = screenToLabel(200f, 100f, canvasWidth = 600f, canvasHeight = 200f, labelWidth = 200, labelHeight = 100)
        assertEquals(100f, lx)
        assertEquals(50f, ly)
    }
}
