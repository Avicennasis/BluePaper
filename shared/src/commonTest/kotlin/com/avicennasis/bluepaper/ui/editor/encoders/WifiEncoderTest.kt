package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WifiEncoderTest {

    private val encoder = WifiEncoder()

    @Test
    fun standardIsWifi() {
        assertEquals(DataStandard.WIFI, encoder.standard)
    }

    @Test
    fun fieldsContainSsidPasswordEncryption() {
        val fields = encoder.fields()
        assertEquals(3, fields.size)
        assertEquals("ssid", fields[0].key)
        assertTrue(fields[0].required)
        assertEquals("password", fields[1].key)
        assertEquals("encryption", fields[2].key)
        assertEquals("WPA", fields[2].hint)
    }

    @Test
    fun encodeProducesWifiString() {
        val result = encoder.encode(
            mapOf(
                "ssid" to "MyNet",
                "password" to "secret",
                "encryption" to "WPA",
            )
        )
        assertEquals("WIFI:T:WPA;S:MyNet;P:secret;;", result)
    }

    @Test
    fun encodeHandlesEmptyPassword() {
        val result = encoder.encode(
            mapOf(
                "ssid" to "OpenNet",
                "password" to "",
                "encryption" to "nopass",
            )
        )
        assertEquals("WIFI:T:nopass;S:OpenNet;P:;;", result)
    }

    @Test
    fun decodeExtractsSsidPasswordEncryption() {
        val result = encoder.decode("WIFI:T:WPA;S:MyNet;P:secret;;")
        assertEquals("MyNet", result["ssid"])
        assertEquals("secret", result["password"])
        assertEquals("WPA", result["encryption"])
    }

    @Test
    fun decodeReturnsEmptyForInvalidPrefix() {
        val result = encoder.decode("NOT_WIFI:T:WPA;S:Net;;")
        assertTrue(result.isEmpty())
    }

    @Test
    fun encryptionFieldTypeIsText() {
        val encryption = encoder.fields().first { it.key == "encryption" }
        assertEquals(FieldType.TEXT, encryption.fieldType)
    }

    @Test
    fun roundTripWithSpecialChars() {
        val original = mapOf("ssid" to "My;Net\\work", "password" to "p@ss;word", "encryption" to "WPA")
        val encoded = encoder.encode(original)
        val decoded = encoder.decode(encoded)
        assertEquals(original["ssid"], decoded["ssid"])
        assertEquals(original["password"], decoded["password"])
    }

    @Test
    fun encryptionWithSemicolonsRoundTrips() {
        val original = mapOf("ssid" to "TestNet", "password" to "pass", "encryption" to "WPA;2")
        val encoded = encoder.encode(original)
        val decoded = encoder.decode(encoded)
        assertEquals("WPA;2", decoded["encryption"])
        assertEquals("TestNet", decoded["ssid"])
        assertEquals("pass", decoded["password"])
    }
}
