package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        // 13-byte variant reads: closingState=b(9), powerLevel=b(10), paperState=b(11), rfidReadState=b(12)
        // Set distinct values at every byte to prove exact offsets are used
        val data = ByteArray(13) { (it + 10).toByte() }
        data[9] = 7
        data[10] = 88.toByte()
        data[11] = 4
        data[12] = 5
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(7, hb.closingState)
        assertEquals(88, hb.powerLevel)
        assertEquals(4, hb.paperState)
        assertEquals(5, hb.rfidReadState)
    }

    @Test
    fun parse13ByteClosingStateOffsetIsNot8() {
        // Verify 13-byte uses index 9 (not 8) for closingState
        val data = ByteArray(13)
        data[8] = 99.toByte()  // would be closingState in 9-byte/10-byte variants
        data[9] = 42           // actual closingState for 13-byte
        data[10] = 0
        data[11] = 0
        data[12] = 0
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(42, hb.closingState)
    }

    @Test
    fun parse19ByteResponse() {
        // 19-byte variant reads: closingState=b(15), powerLevel=b(16), paperState=b(17), rfidReadState=b(18)
        // Set distinct values at every byte to prove exact offsets are used
        val data = ByteArray(19) { (it + 20).toByte() }
        data[15] = 3
        data[16] = 200.toByte()
        data[17] = 1
        data[18] = 2
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(3, hb.closingState)
        assertEquals(200, hb.powerLevel)
        assertEquals(1, hb.paperState)
        assertEquals(2, hb.rfidReadState)
    }

    @Test
    fun parse19ByteAllFieldsPopulated() {
        // Verify all four fields are non-null for 19-byte variant
        val data = ByteArray(19)
        data[15] = 1
        data[16] = 50
        data[17] = 2
        data[18] = 3
        val hb = HeartbeatResponse.fromData(data)
        assertNotNull(hb.closingState)
        assertNotNull(hb.powerLevel)
        assertNotNull(hb.paperState)
        assertNotNull(hb.rfidReadState)
    }

    @Test
    fun parse20ByteResponse() {
        val data = ByteArray(20).also { it[18] = 1; it[19] = 1 }
        val hb = HeartbeatResponse.fromData(data)
        assertNull(hb.closingState)
        assertNull(hb.powerLevel)
        assertEquals(1, hb.paperState)
        assertEquals(1, hb.rfidReadState)
    }

    @Test
    fun parse20ByteResponseNullFields() {
        val data = ByteArray(20)
        data[18] = 2
        data[19] = 1
        val hb = HeartbeatResponse.fromData(data)
        assertNotNull(hb)
        assertNull(hb.closingState)
        assertNull(hb.powerLevel)
        assertEquals(2, hb.paperState)
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
