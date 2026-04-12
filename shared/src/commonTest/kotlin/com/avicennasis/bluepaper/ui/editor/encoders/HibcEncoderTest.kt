package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class HibcEncoderTest {

    private val encoder = HibcEncoder()

    @Test
    fun standardIsHibc() {
        assertEquals(DataStandard.HIBC, encoder.standard)
    }

    @Test
    fun fieldsContainRequiredEntries() {
        val fields = encoder.fields()
        val required = fields.filter { it.required }.map { it.key }
        assertContains(required, "lic")
        assertContains(required, "pcn")
    }

    @Test
    fun encodeStartsWithPlus() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
        )
        val result = encoder.encode(input)
        assertTrue(result.startsWith("+"))
    }

    @Test
    fun encodeBasicFormat() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
        )
        val result = encoder.encode(input)
        assertEquals("+H123PROD4561", result)
    }

    @Test
    fun encodeDefaultUom() {
        val input = mapOf(
            "lic" to "A999",
            "pcn" to "ITEM",
        )
        val result = encoder.encode(input)
        assertEquals("+A999ITEM0", result)
    }

    @Test
    fun encodeWithQuantity() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
            "quantity" to "25",
        )
        val result = encoder.encode(input)
        assertContains(result, "/25")
        assertEquals("+H123PROD4561/25", result)
    }

    @Test
    fun encodeWithoutQuantityHasNoSlash() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        assertTrue(!result.contains("/"))
    }

    @Test
    fun decodeRoundTrip() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
            "quantity" to "25",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)

        assertEquals("H123", decoded["lic"])
        assertEquals("PROD456", decoded["pcn"])
        assertEquals("1", decoded["uom"])
        assertEquals("25", decoded["quantity"])
    }

    @Test
    fun decodeWithoutQuantity() {
        val data = "+A999ITEM0"
        val decoded = encoder.decode(data)
        assertEquals("A999", decoded["lic"])
        assertEquals("ITEM", decoded["pcn"])
        assertEquals("0", decoded["uom"])
        assertTrue("quantity" !in decoded)
    }

    @Test
    fun decodeWithoutPlusPrefix() {
        val data = "H123PROD4561/10"
        val decoded = encoder.decode(data)
        assertEquals("H123", decoded["lic"])
        assertEquals("PROD456", decoded["pcn"])
        assertEquals("1", decoded["uom"])
        assertEquals("10", decoded["quantity"])
    }

    @Test
    fun quantityFieldIsNumber() {
        val fields = encoder.fields().associateBy { it.key }
        assertEquals(FieldType.NUMBER, fields["quantity"]?.fieldType)
    }

    @Test
    fun fieldCount() {
        assertEquals(4, encoder.fields().size)
    }
}
