package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class PacketStreamReaderTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // Real D110_M replies over USB serial.
    private val modelId = hex("55554802091053aaaa")
    private val heartbeat = hex("5555d90b0f4f034a00000000000000dbaaaa")
    private val checkLine = hex("5555d30300c70116aaaa")

    @Test
    fun `whole packet in one chunk`() {
        val packets = PacketStreamReader().feed(modelId)
        assertEquals(listOf(NiimbotPacket.fromBytes(modelId)), packets)
    }

    @Test
    fun `packet split byte by byte`() {
        val reader = PacketStreamReader()
        val out = heartbeat.flatMap { reader.feed(byteArrayOf(it)) }
        assertEquals(listOf(NiimbotPacket.fromBytes(heartbeat)), out)
    }

    @Test
    fun `several packets in one chunk`() {
        val out = PacketStreamReader().feed(checkLine + modelId)
        assertEquals(listOf(0xD3, 0x48), out.map { it.type })
    }

    @Test
    fun `garbage before a packet is skipped`() {
        val out = PacketStreamReader().feed(hex("00ff55aa") + modelId)
        assertEquals(listOf(0x48), out.map { it.type })
    }

    @Test
    fun `corrupt frame is dropped and the next packet still parses`() {
        val corrupt = modelId.copyOf().also { it[6] = 0x00 } // bad checksum
        val out = PacketStreamReader().feed(corrupt + heartbeat)
        assertEquals(listOf(0xD9), out.map { it.type })
    }
}
