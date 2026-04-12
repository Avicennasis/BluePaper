package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoEncoderTest {

    @Test
    fun standardIsGeo() {
        assertEquals(DataStandard.GEO, GeoEncoder.standard)
    }

    @Test
    fun fieldsContainLatitudeAndLongitude() {
        val fields = GeoEncoder.fields()
        assertEquals(2, fields.size)
        assertEquals("latitude", fields[0].key)
        assertTrue(fields[0].required)
        assertEquals(FieldType.NUMBER, fields[0].fieldType)
        assertEquals("longitude", fields[1].key)
        assertTrue(fields[1].required)
        assertEquals(FieldType.NUMBER, fields[1].fieldType)
    }

    @Test
    fun encodeCoordinates() {
        val result = GeoEncoder.encode(mapOf("latitude" to "40.7128", "longitude" to "-74.0060"))
        assertEquals("geo:40.7128,-74.0060", result)
    }

    @Test
    fun decodeCoordinates() {
        val result = GeoEncoder.decode("geo:40.7128,-74.0060")
        assertEquals("40.7128", result["latitude"])
        assertEquals("-74.0060", result["longitude"])
    }

    @Test
    fun roundTrip() {
        val original = mapOf("latitude" to "51.5074", "longitude" to "-0.1278")
        val encoded = GeoEncoder.encode(original)
        val decoded = GeoEncoder.decode(encoded)
        assertEquals(original["latitude"], decoded["latitude"])
        assertEquals(original["longitude"], decoded["longitude"])
    }

    @Test
    fun encodeNegativeCoordinates() {
        val result = GeoEncoder.encode(mapOf("latitude" to "-33.8688", "longitude" to "151.2093"))
        assertEquals("geo:-33.8688,151.2093", result)
    }

    @Test
    fun decodeNegativeCoordinates() {
        val result = GeoEncoder.decode("geo:-33.8688,151.2093")
        assertEquals("-33.8688", result["latitude"])
        assertEquals("151.2093", result["longitude"])
    }
}
