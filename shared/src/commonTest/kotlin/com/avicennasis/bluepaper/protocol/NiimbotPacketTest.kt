package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class NiimbotPacketTest {

    @Test
    fun constructPacketWithTypeAndData() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        assertEquals(0x40, packet.type)
        assertContentEquals(byteArrayOf(0x01), packet.data)
    }

    @Test
    fun constructPacketWithEmptyData() {
        val packet = NiimbotPacket(type = 0xDC, data = byteArrayOf())
        assertEquals(0xDC.toByte().toInt() and 0xFF, packet.type)
        assertContentEquals(byteArrayOf(), packet.data)
    }

    @Test
    fun checksumIsXorOfTypeLengthAndData() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        assertEquals(0x40, packet.checksum)
    }

    @Test
    fun checksumWithEmptyData() {
        val packet = NiimbotPacket(type = 0xDC, data = byteArrayOf())
        assertEquals(0xDC, packet.checksum)
    }

    @Test
    fun checksumWithMultipleDataBytes() {
        val data = byteArrayOf(0x00, 0x64, 0x00, 0xF0.toByte())
        val packet = NiimbotPacket(type = 0x13, data = data)
        val expected = 0x13 xor 0x04 xor 0x00 xor 0x64 xor 0x00 xor 0xF0
        assertEquals(expected and 0xFF, packet.checksum)
    }

    @Test
    fun toBytesProducesCorrectWireFormat() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        val bytes = packet.toBytes()
        val expected = byteArrayOf(
            0x55, 0x55, 0x40, 0x01, 0x01, 0x40, 0xAA.toByte(), 0xAA.toByte()
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun toBytesEmptyDataPacket() {
        val packet = NiimbotPacket(type = 0x01, data = byteArrayOf())
        val bytes = packet.toBytes()
        val expected = byteArrayOf(
            0x55, 0x55, 0x01, 0x00, 0x01, 0xAA.toByte(), 0xAA.toByte()
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun minimumPacketSizeIs7Bytes() {
        val packet = NiimbotPacket(type = 0x01, data = byteArrayOf())
        assertEquals(7, packet.toBytes().size)
    }

    @Test
    fun fromBytesRoundTrip() {
        val original = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        val decoded = NiimbotPacket.fromBytes(original.toBytes())
        assertEquals(original.type, decoded.type)
        assertContentEquals(original.data, decoded.data)
    }

    @Test
    fun fromBytesEmptyDataRoundTrip() {
        val original = NiimbotPacket(type = 0xF3, data = byteArrayOf())
        val decoded = NiimbotPacket.fromBytes(original.toBytes())
        assertEquals(original.type, decoded.type)
        assertContentEquals(original.data, decoded.data)
    }

    @Test
    fun fromBytesWithTrailingBytesAccepted() {
        val wire = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01)).toBytes()
        val padded = wire + byteArrayOf(0xFF.toByte(), 0x00)
        val decoded = NiimbotPacket.fromBytes(padded)
        assertEquals(0x40, decoded.type)
        assertContentEquals(byteArrayOf(0x01), decoded.data)
    }

    @Test
    fun fromBytesRejectsTooShort() {
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(byteArrayOf(0x55, 0x55, 0x01))
        }
    }

    @Test
    fun fromBytesRejectsBadHeader() {
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(byteArrayOf(0xAA.toByte(), 0x55, 0x01, 0x00, 0x01, 0xAA.toByte(), 0xAA.toByte()))
        }
    }

    @Test
    fun fromBytesRejectsBadChecksum() {
        val wire = byteArrayOf(
            0x55, 0x55, 0x40, 0x01, 0x01,
            0x00, // wrong checksum
            0xAA.toByte(), 0xAA.toByte()
        )
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(wire)
        }
    }

    @Test
    fun checksumNonCoincidentalValues() {
        // type=0x40, data=[0x03], len=1 → checksum = 0x40 XOR 0x01 XOR 0x03 = 0x42
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x03))
        assertEquals(0x42, packet.checksum)
    }

    @Test
    fun imageRowLargeYValue() {
        // Verify big-endian y encoding for y > 255
        val rowData = byteArrayOf(0xFF.toByte())
        val packet = CommandBuilder.imageRow(256, rowData)
        // y=256 → header[0]=0x01, header[1]=0x00
        assertEquals(0x01.toByte(), packet.data[0])
        assertEquals(0x00.toByte(), packet.data[1])
    }

    @Test
    fun dataToIntSingleByte() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x03))
        assertEquals(3L, packet.dataToInt())
    }

    @Test
    fun dataToIntTwoBytes() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01, 0x00))
        assertEquals(256L, packet.dataToInt())
    }

    @Test
    fun dataToIntEmpty() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf())
        assertEquals(0L, packet.dataToInt())
    }
}
