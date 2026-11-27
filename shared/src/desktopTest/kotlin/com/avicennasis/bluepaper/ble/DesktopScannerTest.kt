package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.ble.serial.FakeSerialLink
import com.avicennasis.bluepaper.ble.serial.SerialPortInfo
import com.avicennasis.bluepaper.ble.serial.UsbPrinterFinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopScannerTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val brokenBluetooth = object : BleScanner {
        override fun scan(namePrefix: String): Flow<ScannedDevice> = emptyFlow()
        override suspend fun pairedDevices(): List<ScannedDevice> = throw IllegalStateException("no adapter")
    }
    private fun usb(vararg ports: SerialPortInfo) = UsbPrinterFinder(
        listPorts = { ports.toList() },
        openLink = { FakeSerialLink { listOf(hex("55554802091053aaaa")) } },
    )

    @Test
    fun `usb printers are listed even when bluetooth fails`() = runBlocking {
        val found = DesktopScanner(brokenBluetooth, usb(SerialPortInfo("/dev/ttyACM0", 0x3513, 0x0001))).pairedDevices()
        assertEquals(listOf("D110_M (USB)"), found.map { it.name })
    }

    @Test
    fun `bluetooth failure surfaces when nothing else was found`() = runBlocking {
        assertFailsWith<IllegalStateException> { runBlocking { DesktopScanner(brokenBluetooth, usb()).pairedDevices() } }
        Unit
    }
}
