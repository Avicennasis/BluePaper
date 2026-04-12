package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeCardEncoderTest {

    private val encoder = MeCardEncoder()

    @Test
    fun standardIsMeCard() {
        assertEquals(DataStandard.MECARD, encoder.standard)
    }

    @Test
    fun fieldsContainSixEntries() {
        val fields = encoder.fields()
        assertEquals(6, fields.size)
        assertEquals("name", fields[0].key)
        assertTrue(fields[0].required)
        assertEquals("phone", fields[1].key)
        assertTrue(fields[1].required)
        assertEquals(FieldType.PHONE, fields[1].fieldType)
        assertEquals("email", fields[2].key)
        assertEquals(FieldType.EMAIL, fields[2].fieldType)
        assertEquals("url", fields[3].key)
        assertEquals(FieldType.URL, fields[3].fieldType)
    }

    @Test
    fun encodeContainsMeCardPrefixAndFields() {
        val result = encoder.encode(
            mapOf(
                "name" to "Jane Doe",
                "phone" to "5551234567",
                "email" to "jane@example.com",
            )
        )
        assertTrue(result.startsWith("MECARD:"))
        assertTrue(result.contains("N:Jane Doe;"))
        assertTrue(result.contains("TEL:5551234567;"))
        assertTrue(result.contains("EMAIL:jane@example.com;"))
    }

    @Test
    fun encodeOmitsEmptyFields() {
        val result = encoder.encode(
            mapOf(
                "name" to "John",
                "phone" to "5559876543",
                "email" to "",
                "url" to "",
                "address" to "",
                "note" to "",
            )
        )
        assertTrue(result.contains("N:John;"))
        assertTrue(result.contains("TEL:5559876543;"))
        assertTrue(!result.contains("EMAIL:"))
        assertTrue(!result.contains("URL:"))
        assertTrue(!result.contains("ADR:"))
        assertTrue(!result.contains("NOTE:"))
    }

    @Test
    fun encodeAllFields() {
        val result = encoder.encode(
            mapOf(
                "name" to "Alice",
                "phone" to "5550001111",
                "email" to "alice@test.com",
                "url" to "https://alice.dev",
                "address" to "123 Main St",
                "note" to "Friend",
            )
        )
        assertEquals(
            "MECARD:N:Alice;TEL:5550001111;EMAIL:alice@test.com;URL:https://alice.dev;ADR:123 Main St;NOTE:Friend;;",
            result,
        )
    }

    @Test
    fun decodeExtractsNameAndPhone() {
        val result = encoder.decode("MECARD:N:Jane Doe;TEL:5551234567;EMAIL:jane@example.com;;")
        assertEquals("Jane Doe", result["name"])
        assertEquals("5551234567", result["phone"])
        assertEquals("jane@example.com", result["email"])
    }

    @Test
    fun decodeReturnsEmptyForInvalidPrefix() {
        val result = encoder.decode("VCARD:N:Someone;;")
        assertTrue(result.isEmpty())
    }

    @Test
    fun decodeAllFields() {
        val data = "MECARD:N:Alice;TEL:5550001111;EMAIL:alice@test.com;URL:https://alice.dev;ADR:123 Main St;NOTE:Friend;;"
        val result = encoder.decode(data)
        assertEquals("Alice", result["name"])
        assertEquals("5550001111", result["phone"])
        assertEquals("alice@test.com", result["email"])
        assertEquals("https://alice.dev", result["url"])
        assertEquals("123 Main St", result["address"])
        assertEquals("Friend", result["note"])
    }
}
