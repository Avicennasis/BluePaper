package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelDesignTest {

    @Test
    fun roundTripSerialization() {
        val design = LabelDesign(
            model = "b21",
            labelWidthMm = 50.0,
            labelHeightMm = 30.0,
            density = 5,
            quantity = 3,
            elements = listOf(
                SerializableLabelElement(type = "text", id = "t1", x = 10f, y = 20f, text = "Hello\nWorld", fontSize = 32f, fontFamily = "default"),
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
        val json = """{"version":2,"model":"d110","unknownField":"value","labelWidthMm":30.0,"labelHeightMm":15.0,"density":3,"quantity":1,"elements":[]}"""
        val design = LabelDesign.fromJson(json)
        assertEquals("d110", design.model)
    }
}
