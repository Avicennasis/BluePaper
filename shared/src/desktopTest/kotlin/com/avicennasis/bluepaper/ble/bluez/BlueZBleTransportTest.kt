package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.DesktopBleFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueZBleTransportTest {

    @Test
    fun `factory creates transport on Linux`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test on ${System.getProperty("os.name")}")
            return
        }

        val transport = DesktopBleFactory.createTransport()
        assertNotNull(transport)
        assertTrue(transport is BlueZBleTransport)
    }

    @Test
    fun `transport starts disconnected`() {
        val transport = DesktopBleFactory.createTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
    }

    @Test
    fun `BlueZ transport initial state is disconnected`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test")
            return
        }

        val transport = BlueZBleTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
    }
}
