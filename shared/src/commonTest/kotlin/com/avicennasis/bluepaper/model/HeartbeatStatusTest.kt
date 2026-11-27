package com.avicennasis.bluepaper.model

import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartbeatStatusTest {
    private fun packet(type: Int, vararg data: Int) = NiimbotPacket(type, ByteArray(data.size) { data[it].toByte() })

    @Test
    fun `real D110_M advanced heartbeat with no RFID tag`() {
        // Reply to heartbeat type 4 from a D110_M with the roll's tag removed (lid closed, labels in).
        val s = HeartbeatStatus.fromReply(packet(0xD9, 0x0f, 0x09, 0x02, 0x4a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        assertEquals(HeartbeatStatus(lidClosed = true, paperInserted = true, rfidRead = false), s)
    }

    @Test
    fun `lid open and no paper are reported`() {
        val s = HeartbeatStatus.fromReply(packet(0xD9, 0x0f, 0x09, 0x02, 0x4a, 0x01, 0x01, 0x01, 0x00, 0x00))
        assertEquals(HeartbeatStatus(lidClosed = false, paperInserted = false, rfidRead = true), s)
    }

    @Test
    fun `other heartbeat formats are unknown`() {
        // Basic D110 reply: its lid byte reads "open" on a closed D110_M, so it isn't trusted.
        val s = HeartbeatStatus.fromReply(packet(0xDD, 0x0f, 0x2a, 0x00, 0x66, 0x00, 0x66, 0x32, 0x4a, 0x01, 0x02))
        assertEquals(HeartbeatStatus(null, null, null), s)
    }
}
