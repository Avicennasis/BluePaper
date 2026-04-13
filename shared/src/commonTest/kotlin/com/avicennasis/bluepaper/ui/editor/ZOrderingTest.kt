package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class ZOrderingTest {

    private fun els() = listOf(
        LabelElement.TextElement(id = "a", text = "A"),
        LabelElement.TextElement(id = "b", text = "B"),
        LabelElement.TextElement(id = "c", text = "C"),
    )

    @Test
    fun bringToFront() {
        val list = els()
        val result = bringElementToFront(list, "a")
        assertEquals("b", result[0].id)
        assertEquals("c", result[1].id)
        assertEquals("a", result[2].id)
    }

    @Test
    fun bringToFrontAlreadyOnTop() {
        val list = els()
        val result = bringElementToFront(list, "c")
        assertEquals("a", result[0].id)
        assertEquals("b", result[1].id)
        assertEquals("c", result[2].id)
    }

    @Test
    fun sendToBack() {
        val list = els()
        val result = sendElementToBack(list, "c")
        assertEquals("c", result[0].id)
        assertEquals("a", result[1].id)
        assertEquals("b", result[2].id)
    }

    @Test
    fun sendToBackAlreadyAtBottom() {
        val list = els()
        val result = sendElementToBack(list, "a")
        assertEquals("a", result[0].id)
        assertEquals("b", result[1].id)
        assertEquals("c", result[2].id)
    }

    @Test
    fun moveUp() {
        val list = els()
        val result = moveElementUp(list, "a")
        assertEquals("b", result[0].id)
        assertEquals("a", result[1].id)
        assertEquals("c", result[2].id)
    }

    @Test
    fun moveUpAlreadyOnTop() {
        val list = els()
        val result = moveElementUp(list, "c")
        assertEquals(list.map { it.id }, result.map { it.id })
    }

    @Test
    fun moveDown() {
        val list = els()
        val result = moveElementDown(list, "c")
        assertEquals("a", result[0].id)
        assertEquals("c", result[1].id)
        assertEquals("b", result[2].id)
    }

    @Test
    fun moveDownAlreadyAtBottom() {
        val list = els()
        val result = moveElementDown(list, "a")
        assertEquals(list.map { it.id }, result.map { it.id })
    }

    @Test
    fun invalidIdReturnsUnchanged() {
        val list = els()
        assertEquals(list.map { it.id }, bringElementToFront(list, "x").map { it.id })
        assertEquals(list.map { it.id }, sendElementToBack(list, "x").map { it.id })
        assertEquals(list.map { it.id }, moveElementUp(list, "x").map { it.id })
        assertEquals(list.map { it.id }, moveElementDown(list, "x").map { it.id })
    }
}
