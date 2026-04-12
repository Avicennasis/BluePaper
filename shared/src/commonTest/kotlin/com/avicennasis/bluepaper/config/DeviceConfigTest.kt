package com.avicennasis.bluepaper.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceConfigTest {

    @Test
    fun allEightModelsRegistered() {
        val models = listOf("d110", "d11", "d11_h", "d101", "d110_m", "b18", "b21", "b1")
        for (model in models) {
            assertNotNull(DeviceRegistry.get(model), "Missing model: $model")
        }
    }

    @Test
    fun unknownModelReturnsNull() {
        assertNull(DeviceRegistry.get("z99"))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        assertNotNull(DeviceRegistry.get("D110"))
        assertNotNull(DeviceRegistry.get("B21"))
    }

    @Test
    fun d110Config() {
        val cfg = DeviceRegistry.get("d110")!!
        assertEquals(203, cfg.dpi)
        assertEquals(3, cfg.maxDensity)
        assertEquals(-90, cfg.rotation)
        assertFalse(cfg.isV2)
        assertEquals(240, cfg.maxWidthPx)
        assertTrue(cfg.labelSizes.isNotEmpty())
    }

    @Test
    fun b21Config() {
        val cfg = DeviceRegistry.get("b21")!!
        assertEquals(203, cfg.dpi)
        assertEquals(5, cfg.maxDensity)
        assertEquals(0, cfg.rotation)
        assertTrue(cfg.isV2)
        assertEquals(384, cfg.maxWidthPx)
    }

    @Test
    fun d11hIsHighDpi() {
        assertEquals(300, DeviceRegistry.get("d11_h")!!.dpi)
    }

    @Test
    fun d110mIsHighDpi() {
        assertEquals(300, DeviceRegistry.get("d110_m")!!.dpi)
    }

    @Test
    fun v2ModelsAreOnlyBSeries() {
        val v2 = DeviceRegistry.all().filter { it.isV2 }.map { it.model }
        assertEquals(setOf("b1", "b18", "b21"), v2.toSet())
    }

    @Test
    fun dSeriesUseNeg90Rotation() {
        for (model in listOf("d110", "d11", "d11_h", "d101", "d110_m")) {
            assertEquals(-90, DeviceRegistry.get(model)!!.rotation, "Model $model rotation")
        }
    }

    @Test
    fun bSeriesUse0Rotation() {
        for (model in listOf("b1", "b18", "b21")) {
            assertEquals(0, DeviceRegistry.get(model)!!.rotation, "Model $model rotation")
        }
    }
}
