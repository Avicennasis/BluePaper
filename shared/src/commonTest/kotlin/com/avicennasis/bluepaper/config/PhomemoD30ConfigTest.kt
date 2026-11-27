package com.avicennasis.bluepaper.config

import com.avicennasis.bluepaper.image.MonochromeEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhomemoD30ConfigTest {

    @Test
    fun d30IsRegisteredAsEscPosRaster() {
        val cfg = DeviceRegistry.get("d30")!!
        assertEquals(PrinterProtocol.ESC_POS_RASTER, cfg.protocol)
        assertEquals(203, cfg.dpi)
        assertEquals(96, cfg.maxWidthPx)
        val gatt = cfg.gatt!!
        assertEquals("0000ff00-0000-1000-8000-00805f9b34fb", gatt.serviceUuid)
        assertEquals("0000ff02-0000-1000-8000-00805f9b34fb", gatt.writeCharUuid)
        assertTrue(gatt.writeWithResponse)
    }

    @Test
    fun d30TapeFitsThe96DotHead() {
        for (size in DeviceRegistry.get("d30")!!.labelSizes) {
            val bytes = MonochromeEncoder.bytesPerRow(size.heightPx)
            assertTrue(size.heightPx <= 96, "${size.displayName} exceeds the 12 mm head: ${size.heightPx}")
            assertTrue(bytes in 6..12, "${size.displayName} row width $bytes bytes")
        }
    }

    @Test
    fun d30TapeWidthsCovered() {
        val widths = DeviceRegistry.get("d30")!!.labelSizes.map { it.heightMm }.toSet()
        assertTrue(6.0 in widths && 12.0 in widths && 14.0 in widths && 15.0 in widths)
    }

    @Test
    fun d30NameMatchesAndResolves() {
        assertTrue(DeviceRegistry.isSupportedPrinterName("D30"))
        assertEquals("d30", DeviceRegistry.modelForName("D30"))
    }
}
