package com.avicennasis.bluepaper.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceRegistryNameMatchTest {

    @Test
    fun `real D110_M advertised name matches`() {
        // Name reported by BlueZ for a real paired D110_M — note it KEEPS the underscore.
        assertTrue(DeviceRegistry.isSupportedPrinterName("D110_M-HC23072563"))
    }

    @Test
    fun `names without underscores still match`() {
        assertTrue(DeviceRegistry.isSupportedPrinterName("D11H-1234"))
        assertTrue(DeviceRegistry.isSupportedPrinterName("D110M-1234"))
    }

    @Test
    fun `every registered model matches its own name`() {
        for (model in DeviceRegistry.models()) {
            assertTrue(
                DeviceRegistry.isSupportedPrinterName(model.uppercase() + "-ABC123"),
                "model $model should match",
            )
        }
    }

    @Test
    fun `matching is case insensitive`() {
        assertTrue(DeviceRegistry.isSupportedPrinterName("b21-0042"))
    }

    @Test
    fun `non printers do not match`() {
        assertFalse(DeviceRegistry.isSupportedPrinterName("JBL Flip 5"))
        assertFalse(DeviceRegistry.isSupportedPrinterName("Pixel Buds Pro"))
        assertFalse(DeviceRegistry.isSupportedPrinterName(""))
    }

    @Test
    fun `model for a printer name picks the most specific model`() {
        assertEquals("d110_m", DeviceRegistry.modelForName("D110_M-HC23072563"))
        assertEquals("d110", DeviceRegistry.modelForName("D110-ABC123"))
        assertEquals("d11_h", DeviceRegistry.modelForName("D11_H-1"))
        assertEquals("d11", DeviceRegistry.modelForName("D11-1"))
        assertEquals("d101", DeviceRegistry.modelForName("D101-1"))
        assertEquals("b18", DeviceRegistry.modelForName("B18-1"))
        assertEquals("b1", DeviceRegistry.modelForName("B1-1"))
        assertEquals(null, DeviceRegistry.modelForName("JBL Flip 5"))
    }
}
