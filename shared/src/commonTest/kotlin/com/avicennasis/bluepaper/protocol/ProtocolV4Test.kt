package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Protocol-v4 packets (NiimBlue's D110M_V4 print task). Expected bytes follow niimbluelib's
 * PacketGenerator; expected reply types are the replies a real D110_M (fw 4.30) sent.
 */
class ProtocolV4Test {

    private fun bytes(vararg b: Int) = b.map { it.toByte() }
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `connect and status-data packets`() {
        assertEquals(0xC1, CommandBuilder.connect().type)
        assertEquals(bytes(1), CommandBuilder.connect().data.toList())
        assertEquals(0xA5, CommandBuilder.printerStatusData().type)
        assertEquals(bytes(1), CommandBuilder.printerStatusData().data.toList())
    }

    @Test
    fun `startPrintV4 matches printStart9b`() {
        val p = CommandBuilder.startPrintV4(totalPages = 1)
        assertEquals(0x01, p.type)
        assertEquals(bytes(0x00, 0x01, 0, 0, 0, 0, 0, 0, 0), p.data.toList())
        assertEquals(bytes(0x01, 0x2C), CommandBuilder.startPrintV4(300).data.take(2))
    }

    @Test
    fun `setPageSizeV4 matches setPageSize13b`() {
        val p = CommandBuilder.setPageSizeV4(rows = 239, cols = 120, copies = 1)
        assertEquals(0x13, p.type)
        assertEquals(bytes(0x00, 0xEF, 0x00, 0x78, 0x00, 0x01, 0, 0, 0, 0, 0, 0, 0), p.data.toList())
    }

    @Test
    fun `bitmap row splits pixel counts per printhead third when the row fits`() {
        // 96-dot printhead -> 4 bytes per third; 8 + 1 + 2 black pixels.
        val row = bytes(0xFF, 0, 0, 0, 0x01, 0, 0, 0, 0, 0, 0, 0x03).toByteArray()
        val p = CommandBuilder.bitmapRowV4(y = 7, lineData = row, printheadPixels = 96)
        assertEquals(0x85, p.type)
        assertEquals(bytes(0x00, 0x07, 8, 1, 2, 1) + row.toList(), p.data.toList())
    }

    @Test
    fun `bitmap row sends a total pixel count when wider than three thirds`() {
        val row = ByteArray(15) { 0xFF.toByte() } // 120 px > 96-dot printhead
        val p = CommandBuilder.bitmapRowV4(y = 0, lineData = row, printheadPixels = 96)
        assertEquals(bytes(0x00, 0x00, 0x00, 120, 0x00, 1), p.data.take(6)) // [0, low, high] like niimbluelib
    }

    @Test
    fun `empty row packet`() {
        val p = CommandBuilder.emptyRow(y = 300, repeats = 1)
        assertEquals(0x84, p.type)
        assertEquals(bytes(0x01, 0x2C, 0x01), p.data.toList())
    }

    @Test
    fun `protocol version from the real D110_M status data is 4`() {
        assertEquals(4, ProtocolVersion.fromStatusData(hex("3030232800c80000000a00030112ee00")))
    }

    @Test
    fun `protocol version thresholds`() {
        fun data(hi: Int, lo: Int) = ByteArray(13).also { it[11] = hi.toByte(); it[12] = lo.toByte() }
        assertEquals(3, ProtocolVersion.fromStatusData(data(2, 4)))
        assertEquals(4, ProtocolVersion.fromStatusData(data(3, 0)))
        assertEquals(5, ProtocolVersion.fromStatusData(data(3, 2)))
        assertEquals(0, ProtocolVersion.fromStatusData(ByteArray(12)))
    }

    @Test
    fun `expected replies match what a D110_M answered`() {
        val pairs = mapOf(
            CommandBuilder.setLabelDensity(3) to 0x31,
            CommandBuilder.setLabelType(1) to 0x33,
            CommandBuilder.startPrint() to 0x02,
            CommandBuilder.startPagePrint() to 0x04,
            CommandBuilder.setDimension(354, 177) to 0x14,
            CommandBuilder.setQuantity(1) to 0x16,
            CommandBuilder.endPagePrint() to 0xE4,
            CommandBuilder.getPrintStatus() to 0xB3,
            CommandBuilder.endPrint() to 0xF4,
            CommandBuilder.getRfid() to 0x1B,
            CommandBuilder.connect() to 0xC2,
            CommandBuilder.printerStatusData() to 0xB5,
            CommandBuilder.getInfo(InfoEnum.BATTERY) to 0x4A,
        )
        for ((request, reply) in pairs) {
            assertEquals(setOf(reply), ResponseCodes.expectedReplies(request), "request 0x${request.type.toString(16)}")
        }
        assertTrue(0xDD in ResponseCodes.expectedReplies(CommandBuilder.heartbeat())!!)
        assertNull(ResponseCodes.expectedReplies(CommandBuilder.emptyRow(0, 1)))
    }
}
