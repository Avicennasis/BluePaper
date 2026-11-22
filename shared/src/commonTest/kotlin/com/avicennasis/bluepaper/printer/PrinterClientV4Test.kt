package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.MockBleTransport
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PrinterClientV4Test {

    private fun resp(type: Int, vararg data: Int) = NiimbotPacket(type, ByteArray(data.size) { data[it].toByte() })
    private val realD110mStatusData = "3030232800c80000000a00030112ee00".chunked(2).map { it.toInt(16) }.toIntArray()

    private val v4Replies = arrayOf(
        resp(0x33, 1), resp(0x31, 1), resp(0x02, 1), resp(0x14, 1, 0), resp(0xE4, 1),
        resp(0xB3, 0x00, 0x01, 0x64, 0x64, 0x21, 0xC5, 0x00, 0x02), // page 1 done — real D110_M reply shape
        resp(0xF4, 1),
    )
    private val rows = listOf(
        ByteArray(15) { 0xFF.toByte() },
        ByteArray(15),
        ByteArray(15).also { it[0] = 0x80.toByte() },
    )

    private fun transport() = MockBleTransport().apply { connectMock() }

    @Test
    fun `printV4 sends NiimBlue's D110M_V4 sequence`() = runTest {
        val t = transport()
        t.enqueueResponses(*v4Replies)

        PrinterClient(t).printV4(rows, width = 119, density = 3, quantity = 1, printheadPixels = 96)

        assertEquals(listOf(0x23, 0x21, 0x01, 0x13, 0xE3, 0xA3, 0xF3), t.sentCommands.map { it.type })
        assertEquals(9, t.sentCommands[2].data.size, "PrintStart is the 9-byte v4 form")
        val pageSize = t.sentCommands[3].data.map { it.toInt() and 0xFF }
        assertEquals(13, pageSize.size, "SetPageSize is the 13-byte v4 form")
        assertEquals(listOf(0, 3, 0, 120, 0, 1), pageSize.take(6), "rows=3, cols padded 119->120, copies=1")
        assertEquals(listOf(0xA3, 0x85, 0x84, 0x85), t.sentRaw.map { it.type }, "one-way status, then rows (blank -> 0x84)")
    }

    @Test
    fun `printJob uses v4 when a D110_M reports protocol 4`() = runTest {
        val t = transport()
        t.enqueueResponses(resp(0xC2, 3), resp(0xB5, *realD110mStatusData), *v4Replies)

        PrinterClient(t).printJob(DeviceRegistry.get("d110_m")!!, rows, width = 119, height = 3, density = 3, quantity = 1)

        assertEquals(listOf(0xC1, 0xA5, 0x23), t.sentCommands.take(3).map { it.type })
        assertEquals(9, t.sentCommands.first { it.type == 0x01 }.data.size)
    }

    @Test
    fun `printJob keeps the legacy sequence when the printer is not v4`() = runTest {
        val t = transport()
        t.enqueueResponses(
            resp(0xC2, 1), // plain "connected" — protocol below 4
            resp(0x31, 1), resp(0x33, 1), resp(0x02, 1), resp(0x04, 1), resp(0x14, 1, 0), resp(0x16, 1),
            resp(0xE4, 1), resp(0xB3, 0x00, 0x01, 0x64, 0x64), resp(0xF4, 1),
        )

        PrinterClient(t).printJob(DeviceRegistry.get("d110_m")!!, rows, width = 119, height = 3, density = 3, quantity = 1)

        assertEquals(1, t.sentCommands.first { it.type == 0x01 }.data.size, "legacy 1-byte PrintStart")
    }

    @Test
    fun `models without v4 support never negotiate`() = runTest {
        val t = transport()
        t.enqueueResponses(
            resp(0x31, 1), resp(0x33, 1), resp(0x02, 1), resp(0x04, 1), resp(0x14, 1, 0), resp(0x16, 1),
            resp(0xE4, 1), resp(0xB3, 0x00, 0x01, 0x64, 0x64), resp(0xF4, 1),
        )

        PrinterClient(t).printJob(DeviceRegistry.get("d110")!!, rows, width = 119, height = 3, density = 3, quantity = 1)

        assertEquals(0x21, t.sentCommands.first().type)
    }
}
