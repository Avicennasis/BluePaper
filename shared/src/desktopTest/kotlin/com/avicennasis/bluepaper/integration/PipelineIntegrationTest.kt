package com.avicennasis.bluepaper.integration

import com.avicennasis.bluepaper.image.MonochromeEncoder
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineIntegrationTest {

    @Test
    fun pixelsToPacketsRoundTrip() {
        // Create a small 8x2 pixel image (all black).
        // ARGB black = alpha=0xFF, R=0, G=0, B=0
        val width = 8
        val height = 2
        val blackPixel = (0xFF shl 24) // ARGB black
        val pixels = IntArray(width * height) { blackPixel }

        // Encode to monochrome rows (no offset)
        val rows = MonochromeEncoder.encode(pixels, width, height)
        assertEquals(height, rows.size)

        // Each row should be 1 byte (8 pixels packed).
        // All black pixels → grayscale 0 → inverted 255 → bit=1 → all bits set → 0xFF
        rows.forEach { row ->
            assertEquals(1, row.size)
            assertEquals(0xFF.toByte(), row[0])
        }

        // Build image row packets
        val packets = rows.mapIndexed { y, rowData ->
            CommandBuilder.imageRow(y, rowData)
        }

        // Verify packets are well-formed
        assertEquals(height, packets.size)
        packets.forEach { packet ->
            val bytes = packet.toBytes()
            assertTrue(bytes.size >= 7, "Packet too small: ${bytes.size} bytes")
            assertEquals(NiimbotPacket.HEADER, bytes[0])
            assertEquals(NiimbotPacket.HEADER, bytes[1])
            assertEquals(NiimbotPacket.FOOTER, bytes[bytes.size - 1])
            assertEquals(NiimbotPacket.FOOTER, bytes[bytes.size - 2])
        }

        // Verify packets can be re-parsed from their raw bytes
        packets.forEach { packet ->
            val bytes = packet.toBytes()
            val parsed = NiimbotPacket.fromBytes(bytes)
            assertEquals(packet.type, parsed.type)
            assertTrue(packet.data.contentEquals(parsed.data))
        }
    }

    @Test
    fun whiteImageProducesZeroRows() {
        val width = 8
        val height = 2
        // ARGB white: alpha=0xFF, R=0xFF, G=0xFF, B=0xFF
        val whitePixel = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        val pixels = IntArray(width * height) { whitePixel }

        val rows = MonochromeEncoder.encode(pixels, width, height)
        assertEquals(height, rows.size)
        // White pixels → grayscale 255 → inverted 0 → bit=0 → all bits clear → 0x00
        rows.forEach { row ->
            assertEquals(1, row.size)
            assertEquals(0x00.toByte(), row[0])
        }
    }

    @Test
    fun mixedPixelsEncodedCorrectly() {
        // 8 pixels wide: first 4 black, last 4 white → byte = 0xF0
        val width = 8
        val height = 1
        val black = (0xFF shl 24)
        val white = (0xFF shl 24) or (0xFF shl 16) or (0xFF shl 8) or 0xFF
        val pixels = IntArray(width) { i -> if (i < 4) black else white }

        val rows = MonochromeEncoder.encode(pixels, width, height)
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size)
        // First 4 bits set (black), last 4 clear (white) → 1111_0000 = 0xF0
        assertEquals(0xF0.toByte(), rows[0][0])

        // Wrap in a packet and verify round-trip
        val packet = CommandBuilder.imageRow(0, rows[0])
        val bytes = packet.toBytes()
        val parsed = NiimbotPacket.fromBytes(bytes)
        assertTrue(packet.data.contentEquals(parsed.data))
    }

    @Test
    fun multiRowPacketsHaveCorrectYIndices() {
        // Verify the y-index is correctly encoded in each packet's data header
        val width = 8
        val height = 4
        val blackPixel = (0xFF shl 24)
        val pixels = IntArray(width * height) { blackPixel }

        val rows = MonochromeEncoder.encode(pixels, width, height)
        assertEquals(height, rows.size)

        val packets = rows.mapIndexed { y, rowData ->
            CommandBuilder.imageRow(y, rowData)
        }

        packets.forEachIndexed { y, packet ->
            // The image row data starts with a 6-byte header:
            // bytes[0..1] = y index (big-endian 16-bit)
            val yHigh = packet.data[0].toInt() and 0xFF
            val yLow = packet.data[1].toInt() and 0xFF
            val encodedY = (yHigh shl 8) or yLow
            assertEquals(y, encodedY, "Y index mismatch at row $y")
        }
    }

    @Test
    fun wideImageMultipleBytesPerRow() {
        // 16 pixels wide (2 bytes per row), all black
        val width = 16
        val height = 1
        val blackPixel = (0xFF shl 24)
        val pixels = IntArray(width * height) { blackPixel }

        val rows = MonochromeEncoder.encode(pixels, width, height)
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(0xFF.toByte(), rows[0][0])
        assertEquals(0xFF.toByte(), rows[0][1])

        // Build packet and verify it serializes/parses
        val packet = CommandBuilder.imageRow(0, rows[0])
        val bytes = packet.toBytes()
        val parsed = NiimbotPacket.fromBytes(bytes)
        assertEquals(packet.type, parsed.type)
        // Data should be 6-byte header + 2 bytes row data = 8 bytes
        assertEquals(8, parsed.data.size)
    }
}
