package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelDesignMigrationTest {

    @Test
    fun v2RoundTrip() {
        val design = LabelDesign(
            version = 2,
            model = "b21",
            labelWidthMm = 50.0,
            labelHeightMm = 30.0,
            density = 5,
            quantity = 3,
            elements = listOf(
                SerializableLabelElement(type = "text", id = "t1", x = 10f, y = 20f, text = "Hello", fontSize = 32f, fontFamily = "oswald"),
                SerializableLabelElement(type = "image", id = "i1", x = 0f, y = 0f, width = 100f, height = 80f, scale = 1.5f),
            ),
        )
        val json = LabelDesign.toJson(design)
        val restored = LabelDesign.fromJson(json)
        assertEquals(2, restored.version)
        assertEquals(2, restored.elements.size)
        assertEquals("Hello", restored.elements[0].text)
        assertEquals(1.5f, restored.elements[1].scale)
    }

    @Test
    fun v1MigratesToV2() {
        val v1Json = """
        {
            "text": "Old Label",
            "fontSize": 18.0,
            "model": "d110",
            "labelWidthMm": 30.0,
            "labelHeightMm": 15.0,
            "density": 3,
            "quantity": 1,
            "imageTransform": {
                "offsetX": 5.0,
                "offsetY": 10.0,
                "scale": 2.0,
                "rotation": 90.0,
                "flipH": true,
                "flipV": false
            }
        }
        """.trimIndent()

        val design = LabelDesign.fromJson(v1Json)
        val migrated = design.migrateToV2()

        assertEquals(2, migrated.version)
        // V1 image transforms are skipped during migration (bitmap data was never serialized in v1)
        assertEquals(1, migrated.elements.size)

        val textEl = migrated.elements[0]
        assertEquals("text", textEl.type)
        assertEquals("Old Label", textEl.text)
        assertEquals(18f, textEl.fontSize)
    }

    @Test
    fun v1TextOnlyMigration() {
        val v1Json = """
        {
            "text": "Simple",
            "fontSize": 24.0,
            "model": "d110",
            "labelWidthMm": 30.0,
            "labelHeightMm": 15.0,
            "density": 3,
            "quantity": 1,
            "imageTransform": {}
        }
        """.trimIndent()

        val design = LabelDesign.fromJson(v1Json)
        val migrated = design.migrateToV2()

        assertEquals(2, migrated.version)
        assertEquals(1, migrated.elements.size)
        assertEquals("text", migrated.elements[0].type)
    }

    @Test
    fun v2DesignMigrateIsNoop() {
        val design = LabelDesign(
            version = 2,
            elements = listOf(
                SerializableLabelElement(type = "text", id = "t1", text = "Hi"),
            ),
        )
        val migrated = design.migrateToV2()
        assertEquals(design.elements, migrated.elements)
    }
}
