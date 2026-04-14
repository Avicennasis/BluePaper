package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.DesktopBleFactory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueZBleScannerTest {

    @Test
    fun `factory creates scanner on Linux`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test on ${System.getProperty("os.name")}")
            return
        }

        val scanner = DesktopBleFactory.createScanner()
        assertNotNull(scanner)
        assertTrue(scanner is BlueZBleScanner)
    }

    @Test
    fun `factory creates stub scanner on non-Linux`() {
        if (DesktopBleFactory.isLinux) {
            println("Skipping non-Linux test on ${System.getProperty("os.name")}")
            return
        }

        val scanner = DesktopBleFactory.createScanner()
        assertNotNull(scanner)
        // Should be StubBleScanner, but that's internal
    }

    @Test
    fun `platform detection is consistent`() {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("linux") -> assertTrue(DesktopBleFactory.isLinux)
            os.contains("mac") -> assertTrue(DesktopBleFactory.isMacOS)
            os.contains("windows") -> assertTrue(DesktopBleFactory.isWindows)
        }
    }
}
