package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class VCardEncoderTest {

    private val encoder = VCardEncoder()

    @Test
    fun standardIsVCard() {
        assertEquals(DataStandard.VCARD, encoder.standard)
    }

    @Test
    fun fieldsContainRequiredEntries() {
        val fields = encoder.fields()
        val required = fields.filter { it.required }.map { it.key }
        assertContains(required, "lastName")
        assertContains(required, "phone")
        assertContains(required, "email")
    }

    @Test
    fun fieldTypesAreCorrect() {
        val fields = encoder.fields().associateBy { it.key }
        assertEquals(FieldType.PHONE, fields["phone"]?.fieldType)
        assertEquals(FieldType.EMAIL, fields["email"]?.fieldType)
        assertEquals(FieldType.URL, fields["url"]?.fieldType)
        assertEquals(FieldType.TEXT, fields["firstName"]?.fieldType)
    }

    @Test
    fun encodeProducesValidVCard() {
        val input = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "phone" to "+1-555-0100",
            "email" to "john@example.com",
        )
        val result = encoder.encode(input)
        assertTrue(result.startsWith("BEGIN:VCARD"))
        assertTrue(result.endsWith("END:VCARD"))
        assertContains(result, "VERSION:3.0")
        assertContains(result, "N:Doe;John;;;")
        assertContains(result, "FN:John Doe")
        assertContains(result, "TEL:+1-555-0100")
        assertContains(result, "EMAIL:john@example.com")
    }

    @Test
    fun encodeIncludesOrganization() {
        val input = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith",
            "organization" to "Acme Corp",
            "title" to "Engineer",
        )
        val result = encoder.encode(input)
        assertContains(result, "ORG:Acme Corp")
        assertContains(result, "TITLE:Engineer")
    }

    @Test
    fun encodeIncludesAddress() {
        val input = mapOf(
            "lastName" to "Test",
            "street" to "123 Main St",
            "city" to "Springfield",
            "state" to "IL",
            "zip" to "62704",
            "country" to "US",
        )
        val result = encoder.encode(input)
        assertContains(result, "ADR:;;123 Main St;Springfield;IL;62704;US")
    }

    @Test
    fun encodeOmitsEmptyOptionalFields() {
        val input = mapOf(
            "firstName" to "Solo",
            "lastName" to "Person",
        )
        val result = encoder.encode(input)
        val lines = result.lines()
        assertTrue(lines.none { it.startsWith("TEL:") })
        assertTrue(lines.none { it.startsWith("ORG:") })
        assertTrue(lines.none { it.startsWith("ADR:") })
        assertTrue(lines.none { it.startsWith("URL:") })
        assertTrue(lines.none { it.startsWith("NOTE:") })
    }

    @Test
    fun decodeRoundTrip() {
        val input = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "organization" to "Widgets Inc",
            "phone" to "+1-555-0199",
            "email" to "john.doe@widgets.com",
            "street" to "456 Oak Ave",
            "city" to "Portland",
            "state" to "OR",
            "zip" to "97201",
            "country" to "US",
            "url" to "https://widgets.com",
            "note" to "VIP customer",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)

        assertEquals("John", decoded["firstName"])
        assertEquals("Doe", decoded["lastName"])
        assertEquals("Widgets Inc", decoded["organization"])
        assertEquals("+1-555-0199", decoded["phone"])
        assertEquals("john.doe@widgets.com", decoded["email"])
        assertEquals("456 Oak Ave", decoded["street"])
        assertEquals("Portland", decoded["city"])
        assertEquals("OR", decoded["state"])
        assertEquals("97201", decoded["zip"])
        assertEquals("US", decoded["country"])
        assertEquals("https://widgets.com", decoded["url"])
        assertEquals("VIP customer", decoded["note"])
    }

    @Test
    fun decodeParsesExternalVCard() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            N:Garcia;Maria;;;
            FN:Maria Garcia
            ORG:Global Tech
            TEL;TYPE=CELL:+34-600-123456
            EMAIL;TYPE=WORK:maria@globaltech.es
            ADR;TYPE=WORK:;;Calle Mayor 1;Madrid;;28001;Spain
            END:VCARD
        """.trimIndent()
        val decoded = encoder.decode(vcard)

        assertEquals("Maria", decoded["firstName"])
        assertEquals("Garcia", decoded["lastName"])
        assertEquals("Global Tech", decoded["organization"])
        assertEquals("+34-600-123456", decoded["phone"])
        assertEquals("maria@globaltech.es", decoded["email"])
        assertEquals("Calle Mayor 1", decoded["street"])
        assertEquals("Madrid", decoded["city"])
        assertEquals("28001", decoded["zip"])
        assertEquals("Spain", decoded["country"])
    }

    @Test
    fun encodeUrlAndNote() {
        val input = mapOf(
            "lastName" to "Tester",
            "url" to "https://example.org",
            "note" to "Test contact",
        )
        val result = encoder.encode(input)
        assertContains(result, "URL:https://example.org")
        assertContains(result, "NOTE:Test contact")
    }

    @Test
    fun roundTripNameWithSemicolon() {
        val input = mapOf(
            "firstName" to "Miles",
            "lastName" to "O'Brien; Jr.",
            "phone" to "+1-555-0100",
            "email" to "miles@ds9.fed",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)
        assertEquals("O'Brien; Jr.", decoded["lastName"])
        assertEquals("Miles", decoded["firstName"])
    }

    @Test
    fun roundTripAddressWithComma() {
        val input = mapOf(
            "lastName" to "Test",
            "street" to "100 Main St, Suite 4",
            "city" to "Springfield",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)
        assertEquals("100 Main St, Suite 4", decoded["street"])
        assertEquals("Springfield", decoded["city"])
    }

    @Test
    fun roundTripNoteWithNewline() {
        val input = mapOf(
            "lastName" to "Test",
            "note" to "Line one\nLine two",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)
        assertEquals("Line one\nLine two", decoded["note"])
    }

    @Test
    fun fieldCount() {
        assertEquals(13, encoder.fields().size)
    }
}
