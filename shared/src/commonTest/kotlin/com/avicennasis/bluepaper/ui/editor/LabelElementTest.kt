package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LabelElementTest {

    @Test
    fun textElementDefaults() {
        val el = LabelElement.TextElement(id = "t1")
        assertEquals(8f, el.x)
        assertEquals(8f, el.y)
        assertEquals("Hello", el.text)
        assertEquals(24f, el.fontSize)
        assertEquals("default", el.fontFamily)
    }

    @Test
    fun imageElementDefaults() {
        val el = LabelElement.ImageElement(id = "i1")
        assertEquals(0f, el.x)
        assertEquals(0f, el.y)
        assertEquals(1f, el.scale)
        assertEquals(false, el.flipH)
        assertEquals(false, el.flipV)
    }

    @Test
    fun copyProducesNewInstance() {
        val original = LabelElement.TextElement(id = "t1", x = 10f, y = 20f, text = "Hi")
        val moved = original.copy(x = 50f)
        assertEquals(10f, original.x)
        assertEquals(50f, moved.x)
        assertEquals("Hi", moved.text)
    }

    @Test
    fun boundsCheck() {
        val el = LabelElement.TextElement(id = "t1", x = 10f, y = 20f, width = 100f, height = 50f)
        val inside = el.x <= 50f && 50f <= el.x + el.width && el.y <= 30f && 30f <= el.y + el.height
        assertEquals(true, inside)
        val outside = el.x <= 200f && 200f <= el.x + el.width
        assertEquals(false, outside)
    }

    @Test
    fun serializableRoundTrip() {
        val text = LabelElement.TextElement(
            id = "t1", x = 10f, y = 20f, width = 100f, height = 50f,
            text = "Hello", fontSize = 32f, fontFamily = "oswald",
        )
        val serializable = text.toSerializable()
        assertEquals("text", serializable.type)
        assertEquals("t1", serializable.id)
        assertEquals(10f, serializable.x)
        assertEquals("Hello", serializable.text)
        assertEquals("oswald", serializable.fontFamily)

        val restored = serializable.toLabelElement()
        assertIs<LabelElement.TextElement>(restored)
        assertEquals(text.id, restored.id)
        assertEquals(text.x, restored.x)
        assertEquals(text.text, restored.text)
        assertEquals(text.fontFamily, restored.fontFamily)
    }

    @Test
    fun imageSerializableRoundTrip() {
        val img = LabelElement.ImageElement(
            id = "i1", x = 5f, y = 10f, width = 200f, height = 150f,
            scale = 1.5f, flipH = true, flipV = false, rotation = 90f,
        )
        val serializable = img.toSerializable()
        assertEquals("image", serializable.type)
        assertEquals(1.5f, serializable.scale)
        assertEquals(true, serializable.flipH)

        val restored = serializable.toLabelElement()
        assertIs<LabelElement.ImageElement>(restored)
        assertEquals(img.scale, (restored as LabelElement.ImageElement).scale)
        assertEquals(img.flipH, restored.flipH)
    }
}
