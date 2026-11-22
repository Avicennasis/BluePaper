package com.avicennasis.bluepaper.config

import kotlin.test.Test
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
}
