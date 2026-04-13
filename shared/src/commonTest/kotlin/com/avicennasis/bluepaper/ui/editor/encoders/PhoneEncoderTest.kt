package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneEncoderTest {

    @Test
    fun encodePhone() {
        val result = PhoneEncoder.encode(mapOf("number" to "+15551234567"))
        assertEquals("tel:+15551234567", result)
    }

    @Test
    fun decodePhone() {
        val fields = PhoneEncoder.decode("tel:+15551234567")
        assertEquals("+15551234567", fields["number"])
    }

    @Test
    fun decodePhoneWithoutPrefix() {
        val fields = PhoneEncoder.decode("+15551234567")
        assertEquals("+15551234567", fields["number"])
    }

    @Test
    fun roundTrip() {
        val original = mapOf("number" to "+15551234567")
        val encoded = PhoneEncoder.encode(original)
        val decoded = PhoneEncoder.decode(encoded)
        assertEquals(original["number"], decoded["number"])
    }
}
