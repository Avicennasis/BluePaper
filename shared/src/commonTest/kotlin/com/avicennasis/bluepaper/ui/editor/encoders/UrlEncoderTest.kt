package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlEncoderTest {

    private val encoder = UrlEncoder()

    @Test
    fun encodeUrl() {
        val result = encoder.encode(mapOf("url" to "https://example.com"))
        assertEquals("https://example.com", result)
    }

    @Test
    fun decodeUrl() {
        val fields = encoder.decode("https://example.com")
        assertEquals("https://example.com", fields["url"])
    }

    @Test
    fun roundTrip() {
        val original = mapOf("url" to "https://example.com/path?q=1&r=2")
        assertEquals(original["url"], encoder.decode(encoder.encode(original))["url"])
    }
}
