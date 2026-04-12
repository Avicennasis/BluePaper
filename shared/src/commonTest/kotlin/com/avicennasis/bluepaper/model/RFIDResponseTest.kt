package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RFIDResponseTest {

    @Test
    fun parseFullRfidResponse() {
        val data = byteArrayOf(
            0x01,
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x03,
            0x41, 0x42, 0x43,
            0x02,
            0x58, 0x59,
            0x00, 0x64,
            0x00, 0x0A,
            0x01,
        )
        val rfid = RFIDResponse.fromData(data)!!
        assertEquals("0102030405060708", rfid.uuid)
        assertEquals("ABC", rfid.barcode)
        assertEquals("XY", rfid.serial)
        assertEquals(100, rfid.totalLen)
        assertEquals(10, rfid.usedLen)
        assertEquals(1, rfid.type)
    }

    @Test
    fun emptyDataReturnsNull() {
        assertNull(RFIDResponse.fromData(byteArrayOf(0x00)))
    }

    @Test
    fun zeroLengthDataReturnsNull() {
        assertNull(RFIDResponse.fromData(byteArrayOf()))
    }
}
