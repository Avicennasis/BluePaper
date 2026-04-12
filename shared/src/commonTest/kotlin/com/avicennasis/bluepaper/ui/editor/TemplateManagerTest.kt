package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateManagerTest {

    @Test
    fun builtInTemplatesLoad() {
        val templates = TemplateManager.builtInTemplates()
        assertEquals(6, templates.size)
    }

    @Test
    fun simpleTextTemplateHasOneElement() {
        val templates = TemplateManager.builtInTemplates()
        val simple = templates.find { it.name == "Simple Text" }!!
        assertEquals(1, simple.elements.size)
        assertEquals("text", simple.elements[0].type)
    }

    @Test
    fun proportionalScaling() {
        val templateEl = TemplateElement(
            type = "text", xFraction = 0.1f, yFraction = 0.2f,
            widthFraction = 0.8f, heightFraction = 0.3f,
            text = "Test", fontSize = 24f,
        )
        val labelElement = TemplateManager.scaleToLabel(templateEl, labelWidthPx = 240, labelHeightPx = 120, idPrefix = "t")
        assertEquals(24f, labelElement.x)
        assertEquals(24f, labelElement.y)
        assertEquals(192f, labelElement.width)
        assertEquals(36f, labelElement.height)
    }

    @Test
    fun applyTemplateProducesCorrectElements() {
        val template = LabelTemplate(
            name = "Test",
            description = "test",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0f, yFraction = 0f, widthFraction = 1f, heightFraction = 0.5f, text = "Title", fontSize = 28f, fontFamily = "oswald"),
                TemplateElement(type = "text", xFraction = 0f, yFraction = 0.5f, widthFraction = 1f, heightFraction = 0.5f, text = "Subtitle", fontSize = 16f),
            ),
        )
        val elements = TemplateManager.applyTemplate(template, labelWidthPx = 200, labelHeightPx = 100)
        assertEquals(2, elements.size)

        val title = elements[0] as LabelElement.TextElement
        assertEquals("Title", title.text)
        assertEquals(28f, title.fontSize)
        assertEquals("oswald", title.fontFamily)
        assertEquals(0f, title.x)
        assertEquals(0f, title.y)
        assertEquals(200f, title.width)

        val subtitle = elements[1] as LabelElement.TextElement
        assertEquals("Subtitle", subtitle.text)
        assertEquals(50f, subtitle.y)
    }

    @Test
    fun imageTemplateElementProducesImageElement() {
        val template = LabelTemplate(
            name = "Test",
            description = "test",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 0.6f, heightFraction = 1f),
            ),
        )
        val elements = TemplateManager.applyTemplate(template, labelWidthPx = 200, labelHeightPx = 100)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is LabelElement.ImageElement)
        assertTrue(elements[0].width > 119.9f && elements[0].width < 120.1f)
    }
}
