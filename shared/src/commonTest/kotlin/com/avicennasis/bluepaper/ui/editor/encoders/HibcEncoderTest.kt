package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class HibcEncoderTest {

    private val encoder = HibcEncoder()

    // Mirror the HIBC charset so tests can independently compute expected check characters
    private val hibcCharset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. \$/+%"

    private fun expectedCheck(data: String): Char {
        val sum = data.sumOf { ch -> hibcCharset.indexOf(ch).let { idx -> if (idx < 0) 0 else idx } }
        return hibcCharset[sum % 43]
    }

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
    fun encodeEndsWithCheckAndPlus() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
        )
        val result = encoder.encode(input)
        // Must end with a check character followed by '+'
        assertTrue(result.endsWith("+"))
        assertTrue(result.length > 2) // at least "+...X+"
        // The second-to-last char is the check character
        val checkChar = result[result.length - 2]
        assertTrue(hibcCharset.contains(checkChar))
    }

    @Test
    fun encodeBasicFormat() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
        )
        val result = encoder.encode(input)
        // Data = "H123PROD4561", check = expectedCheck("H123PROD4561")
        val check = expectedCheck("H123PROD4561")
        assertEquals("+H123PROD4561${check}+", result)
    }

    @Test
    fun encodeDefaultUom() {
        val input = mapOf(
            "lic" to "A999",
            "pcn" to "ITEM",
        )
        val result = encoder.encode(input)
        val check = expectedCheck("A999ITEM0")
        assertEquals("+A999ITEM0${check}+", result)
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
        val check = expectedCheck("H123PROD4561/25")
        assertContains(result, "/25")
        assertEquals("+H123PROD4561/25${check}+", result)
    }

    @Test
    fun encodeWithoutQuantityHasNoSlash() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        // The only slashes should not appear before the check character
        val body = result.removePrefix("+").dropLast(2) // strip leading + and trailing check+
        assertTrue(!body.contains("/"))
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
        // Encode then decode to ensure proper format with check character
        val input = mapOf(
            "lic" to "A999",
            "pcn" to "ITEM",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)
        assertEquals("A999", decoded["lic"])
        assertEquals("ITEM", decoded["pcn"])
        assertEquals("0", decoded["uom"])
        assertTrue("quantity" !in decoded)
    }

    @Test
    fun decodeWithoutPlusPrefix() {
        // Legacy data without leading '+' or check character still decodes
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

    // --- New tests for Fix 1: Short LIC padding with '0' ---

    @Test
    fun shortLicIsPaddedWithZeros() {
        val input = mapOf(
            "lic" to "AB",
            "pcn" to "X",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        // "AB" should become "AB00" (padded with zeros, not spaces)
        val body = result.removePrefix("+")
        assertTrue(body.startsWith("AB00"), "Short LIC 'AB' should be padded to 'AB00', got body: $body")
    }

    @Test
    fun singleCharLicIsPaddedWithZeros() {
        val input = mapOf(
            "lic" to "Z",
            "pcn" to "TEST",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        val body = result.removePrefix("+")
        assertTrue(body.startsWith("Z000"), "Single char LIC 'Z' should be padded to 'Z000', got body: $body")
    }

    @Test
    fun emptyLicIsPaddedWithZeros() {
        val input = mapOf(
            "lic" to "",
            "pcn" to "ITEM",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        val body = result.removePrefix("+")
        assertTrue(body.startsWith("0000"), "Empty LIC should become '0000', got body: $body")
    }

    // --- New tests for Fix 1: Uppercase conversion ---

    @Test
    fun lowercaseLicIsConvertedToUppercase() {
        val input = mapOf(
            "lic" to "ab12",
            "pcn" to "ITEM",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        val body = result.removePrefix("+")
        assertTrue(body.startsWith("AB12"), "Lowercase LIC 'ab12' should become 'AB12', got body: $body")
    }

    @Test
    fun mixedCaseLicIsConvertedToUppercase() {
        val input = mapOf(
            "lic" to "xY2z",
            "pcn" to "P",
            "uom" to "0",
        )
        val result = encoder.encode(input)
        val body = result.removePrefix("+")
        assertTrue(body.startsWith("XY2Z"), "Mixed case LIC 'xY2z' should become 'XY2Z', got body: $body")
    }

    // --- New tests for Fix 2: Check character ---

    @Test
    fun checkCharacterIsCorrect() {
        val input = mapOf(
            "lic" to "H123",
            "pcn" to "PROD456",
            "uom" to "1",
        )
        val result = encoder.encode(input)
        // result format: +<data><check>+
        val check = result[result.length - 2]
        val dataStr = result.removePrefix("+").dropLast(2)
        assertEquals(expectedCheck(dataStr), check, "Check character should be mod-43 of data positions")
    }

    @Test
    fun checkCharacterWithQuantity() {
        val input = mapOf(
            "lic" to "A999",
            "pcn" to "ITEM",
            "uom" to "0",
            "quantity" to "100",
        )
        val result = encoder.encode(input)
        val check = result[result.length - 2]
        val dataStr = result.removePrefix("+").dropLast(2)
        assertEquals(expectedCheck(dataStr), check, "Check character with quantity should include quantity in sum")
    }

    @Test
    fun roundTripPreservesDataWithCheckCharacter() {
        val input = mapOf(
            "lic" to "B456",
            "pcn" to "WIDGET",
            "uom" to "2",
        )
        val encoded = encoder.encode(input)
        // Verify format: starts with +, ends with check+
        assertTrue(encoded.startsWith("+"))
        assertTrue(encoded.endsWith("+"))
        assertTrue(encoded.length > 2)

        val decoded = encoder.decode(encoded)
        assertEquals("B456", decoded["lic"])
        assertEquals("WIDGET", decoded["pcn"])
        assertEquals("2", decoded["uom"])
    }
}
