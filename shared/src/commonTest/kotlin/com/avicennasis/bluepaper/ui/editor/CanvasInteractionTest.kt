package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    private val textEl = LabelElement.TextElement(id = "r1", x = 50f, y = 50f, width = 100f, height = 60f)

    @Test
    fun hitTestHandleDetectsBottomRight() {
        val handle = hitTestHandle(textEl, 150f, 110f, 12f)
        assertEquals(ResizeHandle.BOTTOM_RIGHT, handle)
    }

    @Test
    fun hitTestHandleDetectsTopLeft() {
        val handle = hitTestHandle(textEl, 50f, 50f, 12f)
        assertEquals(ResizeHandle.TOP_LEFT, handle)
    }

    @Test
    fun hitTestHandleMissReturnsNull() {
        val handle = hitTestHandle(textEl, 100f, 80f, 12f)
        assertNull(handle)
    }

    @Test
    fun hitTestHandleDetectsMiddleHandles() {
        assertEquals(ResizeHandle.TOP, hitTestHandle(textEl, 100f, 50f, 12f))
        assertEquals(ResizeHandle.BOTTOM, hitTestHandle(textEl, 100f, 110f, 12f))
        assertEquals(ResizeHandle.LEFT, hitTestHandle(textEl, 50f, 80f, 12f))
        assertEquals(ResizeHandle.RIGHT, hitTestHandle(textEl, 150f, 80f, 12f))
    }

    @Test
    fun applyResizeBottomRight() {
        val b = applyResize(textEl, ResizeHandle.BOTTOM_RIGHT, 20f, 10f, 0f)
        assertEquals(50f, b.x)
        assertEquals(50f, b.y)
        assertEquals(120f, b.width)
        assertEquals(70f, b.height)
    }

    @Test
    fun applyResizeTopLeft() {
        val b = applyResize(textEl, ResizeHandle.TOP_LEFT, 10f, 5f, 0f)
        assertEquals(60f, b.x)
        assertEquals(55f, b.y)
        assertEquals(90f, b.width)
        assertEquals(55f, b.height)
    }

    @Test
    fun applyResizeEnforcesMinimumSize() {
        val b = applyResize(textEl, ResizeHandle.BOTTOM_RIGHT, -200f, -200f, 0f)
        assertEquals(50f, b.x)
        assertEquals(50f, b.y)
        assertEquals(MIN_ELEMENT_SIZE, b.width)
        assertEquals(MIN_ELEMENT_SIZE, b.height)
    }

    @Test
    fun applyResizeTopLeftClampsPosition() {
        val b = applyResize(textEl, ResizeHandle.TOP_LEFT, 200f, 200f, 0f)
        assertEquals(50f + 100f - MIN_ELEMENT_SIZE, b.x)
        assertEquals(50f + 60f - MIN_ELEMENT_SIZE, b.y)
        assertEquals(MIN_ELEMENT_SIZE, b.width)
        assertEquals(MIN_ELEMENT_SIZE, b.height)
    }

    @Test
    fun applyResizeWithGridSnap() {
        val b = applyResize(textEl, ResizeHandle.BOTTOM_RIGHT, 5f, 3f, 8f)
        assertEquals(48f, b.x)
        assertEquals(48f, b.y)
        assertEquals(104f, b.width)
        assertEquals(64f, b.height)
    }
}
