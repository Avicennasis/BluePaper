package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailEncoderTest {

    @Test
    fun standardIsEmail() {
        assertEquals(DataStandard.EMAIL, EmailEncoder.standard)
    }

    @Test
    fun fieldsContainAddressSubjectBody() {
        val fields = EmailEncoder.fields()
        assertEquals(3, fields.size)
        assertEquals("address", fields[0].key)
        assertTrue(fields[0].required)
        assertEquals(FieldType.EMAIL, fields[0].fieldType)
        assertEquals("subject", fields[1].key)
        assertEquals("body", fields[2].key)
    }

    @Test
    fun encodeWithSubjectAndBody() {
        val result = EmailEncoder.encode(
            mapOf("address" to "user@example.com", "subject" to "Hi", "body" to "Hello there")
        )
        assertEquals("mailto:user@example.com?subject=Hi&body=Hello%20there", result)
    }

    @Test
    fun encodeAddressOnly() {
        val result = EmailEncoder.encode(mapOf("address" to "user@example.com"))
        assertEquals("mailto:user@example.com", result)
    }

    @Test
    fun encodeWithSubjectOnly() {
        val result = EmailEncoder.encode(
            mapOf("address" to "user@example.com", "subject" to "Hi", "body" to "")
        )
        assertEquals("mailto:user@example.com?subject=Hi", result)
    }

    @Test
    fun decodeWithSubjectAndBody() {
        val result = EmailEncoder.decode("mailto:user@example.com?subject=Hi&body=Hello there")
        assertEquals("user@example.com", result["address"])
        assertEquals("Hi", result["subject"])
        assertEquals("Hello there", result["body"])
    }

    @Test
    fun decodeAddressOnly() {
        val result = EmailEncoder.decode("mailto:user@example.com")
        assertEquals("user@example.com", result["address"])
        assertEquals("", result["subject"])
        assertEquals("", result["body"])
    }

    @Test
    fun roundTrip() {
        val original = mapOf("address" to "test@test.com", "subject" to "Test", "body" to "Body text")
        val encoded = EmailEncoder.encode(original)
        val decoded = EmailEncoder.decode(encoded)
        assertEquals(original["address"], decoded["address"])
        assertEquals(original["subject"], decoded["subject"])
        assertEquals(original["body"], decoded["body"])
    }

    @Test
    fun roundTripWithAmpersand() {
        val original = mapOf("address" to "test@example.com", "subject" to "Tom & Jerry", "body" to "A&B")
        val encoded = EmailEncoder.encode(original)
        val decoded = EmailEncoder.decode(encoded)
        assertEquals(original["subject"], decoded["subject"])
        assertEquals(original["body"], decoded["body"])
    }
}
