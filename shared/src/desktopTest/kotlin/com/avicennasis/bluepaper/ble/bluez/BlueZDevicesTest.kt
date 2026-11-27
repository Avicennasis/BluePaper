package com.avicennasis.bluepaper.ble.bluez

import org.freedesktop.dbus.types.Variant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlueZDevicesTest {

    private fun props(vararg pairs: Pair<String, Variant<*>>): Map<String, Variant<*>> = mapOf(*pairs)

    @Test
    fun `paired device properties map to a paired ScannedDevice`() {
        val device = BlueZDevices.toScannedDevice(
            props(
                "Name" to Variant("D110_M-HC23072563"),
                "Address" to Variant("C9:36:C4:07:25:63"),
                "RSSI" to Variant((-42).toShort()),
                "UUIDs" to Variant(listOf("e7810a71-73ae-499d-8c15-faa9aef0c3f2"), "as"),
                "Paired" to Variant(true),
            )
        )
        assertNotNull(device)
        assertEquals("D110_M-HC23072563", device.name)
        assertEquals("C9:36:C4:07:25:63", device.address)
        assertEquals(-42, device.rssi)
        assertEquals(listOf("e7810a71-73ae-499d-8c15-faa9aef0c3f2"), device.serviceUuids)
        assertTrue(device.paired)
    }

    @Test
    fun `bonded counts as paired`() {
        val device = BlueZDevices.toScannedDevice(
            props("Name" to Variant("B21-1"), "Address" to Variant("BB:BB:BB:BB:BB:BB"), "Bonded" to Variant(true))
        )
        assertTrue(device!!.paired)
    }

    @Test
    fun `missing optional properties use defaults`() {
        val device = BlueZDevices.toScannedDevice(
            props("Name" to Variant("B21-1"), "Address" to Variant("BB:BB:BB:BB:BB:BB"))
        )
        assertNotNull(device)
        assertEquals(-100, device.rssi)
        assertEquals(emptyList(), device.serviceUuids)
        assertFalse(device.paired)
    }

    @Test
    fun `devices without a name or address are skipped`() {
        assertNull(BlueZDevices.toScannedDevice(props("Address" to Variant("BB:BB:BB:BB:BB:BB"))))
        assertNull(BlueZDevices.toScannedDevice(props("Name" to Variant("B21-1"))))
    }
}
