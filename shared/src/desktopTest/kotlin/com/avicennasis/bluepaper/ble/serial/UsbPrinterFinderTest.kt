package com.avicennasis.bluepaper.ble.serial

import com.avicennasis.bluepaper.ble.DeviceTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsbPrinterFinderTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `a YICHIP port answering model 2320 is listed as a D110_M`() {
        val printer = FakeSerialLink { listOf(hex("55554802091053aaaa")) }
        val other = FakeSerialLink()
        val links = mapOf("/dev/ttyACM0" to printer, "/dev/ttyACM1" to other)
        val finder = UsbPrinterFinder(
            listPorts = { listOf(SerialPortInfo("/dev/ttyACM0", 0x3513, 0x0001), SerialPortInfo("/dev/ttyACM1", 0x2341, 0x0043)) },
            openLink = { links.getValue(it) },
        )

        val found = finder.find()

        assertEquals(1, found.size)
        assertEquals("D110_M (USB)", found[0].name)
        assertEquals("/dev/ttyACM0", found[0].address)
        assertEquals(DeviceTransport.USB, found[0].transport)
        assertTrue(other.written.isEmpty(), "never writes to ports that aren't known printer chips")
        assertTrue(printer.closed, "probe closes the port")
    }

    @Test
    fun `a known port that doesn't answer is skipped`() {
        val silent = FakeSerialLink()
        val finder = UsbPrinterFinder(
            listPorts = { listOf(SerialPortInfo("/dev/ttyACM0", 0x3513, 0x0001)) },
            openLink = { silent },
            probeTimeoutMs = 200,
        )
        assertEquals(emptyList(), finder.find())
        assertTrue(silent.closed)
    }
}
