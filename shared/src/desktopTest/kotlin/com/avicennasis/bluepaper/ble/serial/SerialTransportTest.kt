package com.avicennasis.bluepaper.ble.serial

import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.DeviceTransport
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SerialTransportTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val device = ScannedDevice("D110_M (USB)", "/dev/ttyACM0", 0, emptyList(), transport = DeviceTransport.USB)
    private var transport: SerialTransport? = null

    private fun connected(link: FakeSerialLink): SerialTransport {
        val t = SerialTransport(openLink = { path -> assertEquals("/dev/ttyACM0", path); link })
        runBlocking { t.connect(device) }
        transport = t
        return t
    }

    @AfterTest
    fun tearDown() = runBlocking { transport?.disconnect(); Unit }

    @Test
    fun `reply split into chunks after a check-line notice is matched`() = runBlocking {
        val reply = hex("55554802091053aaaa")
        val link = FakeSerialLink { listOf(hex("5555d30300c70116aaaa"), reply.copyOfRange(0, 4), reply.copyOfRange(4, reply.size)) }
        val t = connected(link)
        assertEquals(ConnectionState.CONNECTED, t.connectionState.value)

        val got = t.sendCommand(CommandBuilder.getInfo(InfoEnum.DEVICE_TYPE))

        assertEquals(NiimbotPacket.fromBytes(reply), got)
        assertEquals(CommandBuilder.getInfo(InfoEnum.DEVICE_TYPE).toBytes().toList(), link.written.single().toList())
    }

    @Test
    fun `printer error becomes a readable exception`() = runBlocking {
        val t = connected(FakeSerialLink { listOf(hex("5555db0114ceaaaa")) })
        val e = assertFailsWith<PrinterException> { runBlocking { t.sendCommand(CommandBuilder.startPrintV4(1)) } }
        assertTrue("RFID" in e.message!!)
    }

    @Test
    fun `no reply times out`() = runBlocking {
        val t = connected(FakeSerialLink())
        assertFailsWith<PrinterTimeoutException> { runBlocking { t.sendCommand(CommandBuilder.heartbeat(), timeoutMs = 200) } }
        Unit
    }

    @Test
    fun `raw writes are paced like NiimBlue`() = runBlocking {
        val link = FakeSerialLink()
        val t = connected(link)
        repeat(3) { t.writeRaw(CommandBuilder.emptyRow(it, 1)) }
        assertEquals(3, link.written.size)
        val gapsMs = link.writeTimesNanos.zipWithNext { a, b -> (b - a) / 1_000_000.0 }
        assertTrue(gapsMs.all { it >= 9.0 }, "gaps $gapsMs")
    }

    @Test
    fun `disconnect closes the port`() = runBlocking {
        val link = FakeSerialLink()
        val t = connected(link)
        t.disconnect()
        assertTrue(link.closed)
        assertEquals(ConnectionState.DISCONNECTED, t.connectionState.value)
    }
}
