package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class AamvaEncoderTest {

    private val encoder = AamvaEncoder()

    @Test
    fun standardIsAamva() {
        assertEquals(DataStandard.AAMVA, encoder.standard)
    }

    @Test
    fun fieldsContainRequiredEntries() {
        val fields = encoder.fields()
        val required = fields.filter { it.required }.map { it.key }
        assertContains(required, "firstName")
        assertContains(required, "lastName")
        assertContains(required, "dob")
        assertContains(required, "licenseNumber")
        assertContains(required, "city")
        assertContains(required, "state")
        assertContains(required, "zip")
    }

    @Test
    fun encodeContainsFieldTags() {
        val input = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "dob" to "01151990",
            "licenseNumber" to "D1234567",
            "city" to "Pittsburgh",
            "state" to "PA",
            "zip" to "15213",
        )
        val result = encoder.encode(input)
        assertContains(result, "DCSDoe")
        assertContains(result, "DACJohn")
        assertContains(result, "DBB01151990")
        assertContains(result, "DAQD1234567")
        assertContains(result, "DAIPittsburgh")
        assertContains(result, "DAJPA")
        assertContains(result, "DAK15213")
    }

    @Test
    fun encodeIncludesOptionalFields() {
        val input = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith",
            "dob" to "06301985",
            "licenseNumber" to "S9876543",
            "city" to "Harrisburg",
            "state" to "PA",
            "zip" to "17101",
            "street" to "100 State St",
            "country" to "USA",
            "sex" to "2",
            "height" to "504",
            "weight" to "120",
            "eyeColor" to "BRO",
            "hairColor" to "BLK",
            "expirationDate" to "06302030",
        )
        val result = encoder.encode(input)
        assertContains(result, "DAG100 State St")
        assertContains(result, "DCGUSA")
        assertContains(result, "DBC2")
        assertContains(result, "DAU504")
        assertContains(result, "DAW120")
        assertContains(result, "DAYBRO")
        assertContains(result, "DAZBLK")
        assertContains(result, "DBA06302030")
    }

    @Test
    fun encodeOmitsEmptyOptionalFields() {
        val input = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "dob" to "01011990",
            "licenseNumber" to "X0000001",
            "city" to "Philly",
            "state" to "PA",
            "zip" to "19101",
        )
        val result = encoder.encode(input)
        assertTrue(result.lines().none { it.startsWith("DAG") })
        assertTrue(result.lines().none { it.startsWith("DCG") })
        assertTrue(result.lines().none { it.startsWith("DBC") })
    }

    @Test
    fun decodeRoundTrip() {
        val input = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "dob" to "01151990",
            "licenseNumber" to "D1234567",
            "street" to "123 Main St",
            "city" to "Pittsburgh",
            "state" to "PA",
            "zip" to "15213",
            "country" to "USA",
            "sex" to "1",
            "height" to "510",
            "weight" to "180",
            "eyeColor" to "BLU",
            "hairColor" to "BRO",
            "expirationDate" to "01152030",
        )
        val encoded = encoder.encode(input)
        val decoded = encoder.decode(encoded)

        assertEquals("John", decoded["firstName"])
        assertEquals("Doe", decoded["lastName"])
        assertEquals("01151990", decoded["dob"])
        assertEquals("D1234567", decoded["licenseNumber"])
        assertEquals("123 Main St", decoded["street"])
        assertEquals("Pittsburgh", decoded["city"])
        assertEquals("PA", decoded["state"])
        assertEquals("15213", decoded["zip"])
        assertEquals("USA", decoded["country"])
        assertEquals("1", decoded["sex"])
        assertEquals("510", decoded["height"])
        assertEquals("180", decoded["weight"])
        assertEquals("BLU", decoded["eyeColor"])
        assertEquals("BRO", decoded["hairColor"])
        assertEquals("01152030", decoded["expirationDate"])
    }

    @Test
    fun decodeHandlesUnknownTags() {
        val data = """
            DCSSmith
            DACAlice
            DBB12251995
            ZZZunknown
        """.trimIndent()
        val decoded = encoder.decode(data)
        assertEquals("Smith", decoded["lastName"])
        assertEquals("Alice", decoded["firstName"])
        assertEquals("12251995", decoded["dob"])
        assertEquals(3, decoded.size)
    }

    @Test
    fun fieldCount() {
        assertEquals(15, encoder.fields().size)
    }
}
