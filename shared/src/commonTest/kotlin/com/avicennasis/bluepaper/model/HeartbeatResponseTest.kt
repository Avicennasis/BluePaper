package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeartbeatResponseTest {

    @Test
    fun parse9ByteResponse() {
        val data = ByteArray(9).also { it[8] = 1 }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.closingState)
        assertNull(hb.powerLevel)
        assertNull(hb.paperState)
        assertNull(hb.rfidReadState)
    }

    @Test
    fun parse10ByteResponse() {
        val data = ByteArray(10).also { it[8] = 0; it[9] = 85.toByte() }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(0, hb.closingState)
        assertEquals(85, hb.powerLevel)
        assertNull(hb.paperState)
    }

    @Test
    fun parse13ByteResponse() {
        val data = ByteArray(13).also { it[9] = 1; it[10] = 75.toByte(); it[11] = 2; it[12] = 3 }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.closingState)
        assertEquals(75, hb.powerLevel)
        assertEquals(2, hb.paperState)
        assertEquals(3, hb.rfidReadState)
    }

    @Test
    fun parse19ByteResponse() {
        val data = ByteArray(19).also { it[15] = 0; it[16] = 100.toByte(); it[17] = 1; it[18] = 0 }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(0, hb.closingState)
        assertEquals(100, hb.powerLevel)
        assertEquals(1, hb.paperState)
        assertEquals(0, hb.rfidReadState)
    }

    @Test
    fun parse20ByteResponse() {
        val data = ByteArray(20).also { it[18] = 1; it[19] = 1 }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.paperState)
        assertEquals(1, hb.rfidReadState)
    }

    @Test
    fun unknownLengthReturnsAllNull() {
        val hb = HeartbeatResponse.fromData(ByteArray(5))
        assertNull(hb.closingState)
        assertNull(hb.powerLevel)
        assertNull(hb.paperState)
        assertNull(hb.rfidReadState)
    }
}
