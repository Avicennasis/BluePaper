package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelDesignTest {

    @Test
    fun roundTripSerialization() {
        val design = LabelDesign(
            text = "Hello\nWorld",
            fontSize = 32f,
            model = "b21",
            labelWidthMm = 50.0,
            labelHeightMm = 30.0,
            density = 5,
            quantity = 3,
            imageTransform = SerializableImageTransform(
                offsetX = 10f, offsetY = 20f, scale = 1.5f, rotation = 90f, flipH = true, flipV = false,
            ),
        )

        val json = LabelDesign.toJson(design)
        val restored = LabelDesign.fromJson(json)

        assertEquals(design, restored)
    }

    @Test
    fun defaultDesignRoundTrips() {
        val design = LabelDesign()
        val json = LabelDesign.toJson(design)
        val restored = LabelDesign.fromJson(json)
        assertEquals(design, restored)
    }

    @Test
    fun ignoresUnknownFields() {
        val json = """{"text":"Test","fontSize":24.0,"model":"d110","unknownField":"value","labelWidthMm":30.0,"labelHeightMm":15.0,"density":3,"quantity":1,"imageTransform":{}}"""
        val design = LabelDesign.fromJson(json)
        assertEquals("Test", design.text)
    }
}
