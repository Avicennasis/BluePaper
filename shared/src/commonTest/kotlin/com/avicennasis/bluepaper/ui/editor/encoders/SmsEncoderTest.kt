package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsEncoderTest {

    @Test
    fun standardIsSms() {
        assertEquals(DataStandard.SMS, SmsEncoder.standard)
    }

    @Test
    fun fieldsContainPhoneAndMessage() {
        val fields = SmsEncoder.fields()
        assertEquals(2, fields.size)
        assertEquals("phone", fields[0].key)
        assertTrue(fields[0].required)
        assertEquals(FieldType.PHONE, fields[0].fieldType)
        assertEquals("message", fields[1].key)
    }

    @Test
    fun encodeWithPhoneAndMessage() {
        val result = SmsEncoder.encode(mapOf("phone" to "+123", "message" to "Hello!"))
        assertEquals("smsto:+123:Hello!", result)
    }

    @Test
    fun encodeWithPhoneOnly() {
        val result = SmsEncoder.encode(mapOf("phone" to "+123"))
        assertEquals("smsto:+123:", result)
    }

    @Test
    fun decodeWithPhoneAndMessage() {
        val result = SmsEncoder.decode("smsto:+123:Hello!")
        assertEquals("+123", result["phone"])
        assertEquals("Hello!", result["message"])
    }

    @Test
    fun decodePhoneOnly() {
        val result = SmsEncoder.decode("smsto:+123")
        assertEquals("+123", result["phone"])
        assertEquals("", result["message"])
    }

    @Test
    fun roundTrip() {
        val original = mapOf("phone" to "+15551234567", "message" to "Meet at 5pm")
        val encoded = SmsEncoder.encode(original)
        val decoded = SmsEncoder.decode(encoded)
        assertEquals(original["phone"], decoded["phone"])
        assertEquals(original["message"], decoded["message"])
    }

    @Test
    fun decodeMessageWithColons() {
        val result = SmsEncoder.decode("smsto:+123:Time: 5:00pm")
        assertEquals("+123", result["phone"])
        assertEquals("Time: 5:00pm", result["message"])
    }

    @Test
    fun decodeHandlesUppercasePrefix() {
        val result = SmsEncoder.decode("SMSTO:+456:Hi there")
        assertEquals("+456", result["phone"])
        assertEquals("Hi there", result["message"])
    }

    @Test
    fun decodeHandlesMixedCasePrefix() {
        val result = SmsEncoder.decode("SmStO:+789:Hey")
        assertEquals("+789", result["phone"])
        assertEquals("Hey", result["message"])
    }
}
