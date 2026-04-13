package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class Gs1EncoderTest {

    private val encoder = Gs1128Encoder()
    private val dmEncoder = Gs1DataMatrixEncoder()
    private val GS = Gs1128Encoder.GS

    // ---- GS1-128 Tests ----

    @Test
    fun standardIsGs1128() {
        assertEquals(DataStandard.GS1_128, encoder.standard)
    }

    @Test
    fun encodeProducesRawAiData() {
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
            "batchLot" to "ABC123",
        )
        val result = encoder.encode(input)
        // Fixed-length AIs (01, 17) need no separator; batchLot (10) is last so no trailing GS
        assertEquals("010123456789012817261231" + "10ABC123", result)
    }

    @Test
    fun encodeNoParenthesesInOutput() {
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
            "batchLot" to "ABC123",
            "serial" to "SN999",
            "count" to "100",
        )
        val result = encoder.encode(input)
        assertTrue('(' !in result, "Encoded output must not contain '('")
        assertTrue(')' !in result, "Encoded output must not contain ')'")
    }

    @Test
    fun encodeFieldOrder() {
        val input = mapOf(
            "gtin" to "09876543210987",
            "useBy" to "270630",
            "batchLot" to "LOT42",
            "serial" to "SN001",
            "count" to "50",
        )
        val result = encoder.encode(input)
        assertTrue(result.startsWith("01"), "Data should start with AI 01")
        val idx01 = result.indexOf("01098765")
        val idx17 = result.indexOf("17270630")
        val idx10 = result.indexOf("10LOT42")
        val idx21 = result.indexOf("21SN001")
        val idx30 = result.indexOf("3050")
        assertTrue(idx01 < idx17, "GTIN (01) should come before Use By (17)")
        assertTrue(idx17 < idx10, "Use By (17) should come before Batch (10)")
        assertTrue(idx10 < idx21, "Batch (10) should come before Serial (21)")
        assertTrue(idx21 < idx30, "Serial (21) should come before Count (30)")
    }

    @Test
    fun encodeOmitsEmptyFields() {
        val input = mapOf(
            "gtin" to "01234567890128",
        )
        val result = encoder.encode(input)
        assertEquals("0101234567890128", result)
    }

    @Test
    fun encodeVariableLengthAiInsertsSeparator() {
        // When a variable-length AI (10) is followed by another AI (21),
        // a GS separator must appear between them.
        val input = mapOf(
            "batchLot" to "LOT42",
            "serial" to "SN001",
        )
        val result = encoder.encode(input)
        assertEquals("10LOT42${GS}21SN001", result)
    }

    @Test
    fun encodeFixedLengthAiNoSeparator() {
        // Fixed-length AIs (01, 17) should NOT produce a GS separator
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
        )
        val result = encoder.encode(input)
        assertEquals("010123456789012817261231", result)
        assertTrue(GS !in result, "Fixed-length AIs should not have GS separators")
    }

    @Test
    fun decodeRoundTrip() {
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
            "batchLot" to "ABC123",
            "serial" to "SN999",
            "count" to "100",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)

        assertEquals("01234567890128", decoded["gtin"])
        assertEquals("261231", decoded["useBy"])
        assertEquals("ABC123", decoded["batchLot"])
        assertEquals("SN999", decoded["serial"])
        assertEquals("100", decoded["count"])
    }

    @Test
    fun decodeRoundTripAllFields() {
        // Round-trip with every field populated — encode then decode must recover all values
        val input = mapOf(
            "gtin" to "09876543210987",
            "useBy" to "270630",
            "batchLot" to "LOT42",
            "serial" to "SN001",
            "count" to "50",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun decodeHandlesPartialData() {
        val data = "011234567890123410BATCH1"
        val decoded = encoder.decode(data)
        assertEquals("12345678901234", decoded["gtin"])
        assertEquals("BATCH1", decoded["batchLot"])
        assertEquals(2, decoded.size)
    }

    @Test
    fun decodeStripsGsSeparators() {
        val data = "010123456789012817261231" + "${GS}10ABC123"
        val decoded = encoder.decode(data)
        assertEquals("01234567890128", decoded["gtin"])
        assertEquals("261231", decoded["useBy"])
        assertEquals("ABC123", decoded["batchLot"])
    }

    @Test
    fun fieldTypesAreCorrect() {
        val fields = encoder.fields().associateBy { it.key }
        assertEquals(FieldType.NUMBER, fields["gtin"]?.fieldType)
        assertEquals(FieldType.NUMBER, fields["count"]?.fieldType)
        assertEquals(FieldType.TEXT, fields["batchLot"]?.fieldType)
        assertEquals(FieldType.TEXT, fields["serial"]?.fieldType)
    }

    // ---- GS1 DataMatrix Tests ----

    @Test
    fun dataMatrixStandardIsCorrect() {
        assertEquals(DataStandard.GS1_DATAMATRIX, dmEncoder.standard)
    }

    @Test
    fun dataMatrixMatchesGs1128Encoding() {
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
            "batchLot" to "ABC123",
        )
        val gs1Result = encoder.encode(input)
        val dmResult = dmEncoder.encode(input)
        assertEquals(gs1Result, dmResult)
    }

    @Test
    fun dataMatrixMatchesGs1128Decoding() {
        val data = "010123456789012817261231" + "10LOT42"
        val gs1Decoded = encoder.decode(data)
        val dmDecoded = dmEncoder.decode(data)
        assertEquals(gs1Decoded, dmDecoded)
    }

    @Test
    fun dataMatrixFieldsMatchGs1128() {
        val gs1Fields = encoder.fields().map { it.key }
        val dmFields = dmEncoder.fields().map { it.key }
        assertEquals(gs1Fields, dmFields)
    }

    @Test
    fun dataMatrixRoundTrip() {
        val input = mapOf(
            "gtin" to "01234567890128",
            "useBy" to "261231",
            "batchLot" to "ABC123",
        )
        val encoded = dmEncoder.encode(input)
        val decoded = dmEncoder.decode(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun fieldCount() {
        assertEquals(5, encoder.fields().size)
    }
}
