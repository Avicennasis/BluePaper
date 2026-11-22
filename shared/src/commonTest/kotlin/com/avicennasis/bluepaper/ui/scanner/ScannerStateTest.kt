package com.avicennasis.bluepaper.ui.scanner

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.MockBleTransport
import com.avicennasis.bluepaper.ble.ScannedDevice
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fake that behaves like BlueZBleScanner: one discovery session at a time (a second
 * concurrent scan() ends immediately with nothing), prefix filtering on the name,
 * and discovery that keeps running until cancelled.
 */
private class FakeBleScanner(
    private val paired: List<ScannedDevice> = emptyList(),
    private val discoverable: List<ScannedDevice> = emptyList(),
) : BleScanner {
    val scanCalls = mutableListOf<String>()
    var pairedCalls = 0
    private var busy = false

    override fun scan(namePrefix: String): Flow<ScannedDevice> = flow {
        scanCalls += namePrefix
        if (busy) return@flow
        busy = true
        try {
            discoverable
                .filter { it.name.lowercase().startsWith(namePrefix.lowercase()) }
                .forEach { emit(it) }
            awaitCancellation()
        } finally {
            busy = false
        }
    }

    override suspend fun pairedDevices(): List<ScannedDevice> {
        pairedCalls++
        return paired
    }
}

private fun dev(name: String, address: String, paired: Boolean = false) =
    ScannedDevice(name = name, address = address, rssi = -50, serviceUuids = emptyList(), paired = paired)

class ScannerStateTest {

    private val d110m = dev("D110_M-HC23072563", "C9:36:C4:07:25:63", paired = true)
    private val buds = dev("Pixel Buds Pro", "AA:AA:AA:AA:AA:AA", paired = true)
    private val b21 = dev("B21-0042", "BB:BB:BB:BB:BB:BB")
    private val speaker = dev("JBL Flip 5", "CC:CC:CC:CC:CC:CC")

    @Test
    fun `paired printers are listed without scanning`() = runTest {
        val scanner = FakeBleScanner(paired = listOf(d110m, buds))
        val state = ScannerState(scanner, MockBleTransport(), this)

        state.loadPairedDevices()
        advanceUntilIdle()

        assertEquals(listOf(d110m), state.devices.value)
        assertTrue(state.devices.value.single().paired)
        assertEquals(emptyList(), scanner.scanCalls)
    }

    @Test
    fun `scan finds every supported model in one discovery session`() = runTest {
        val scanner = FakeBleScanner(discoverable = listOf(b21, d110m.copy(paired = false), speaker))
        val state = ScannerState(scanner, MockBleTransport(), this)

        state.startScan()
        advanceUntilIdle()

        val names = state.devices.value.map { it.name }.toSet()
        assertEquals(setOf("B21-0042", "D110_M-HC23072563"), names)
        assertEquals(1, scanner.scanCalls.size, "BlueZ allows one discovery session at a time")
        state.stopScan()
    }

    @Test
    fun `scan keeps paired printers and does not duplicate them`() = runTest {
        val scanner = FakeBleScanner(
            paired = listOf(d110m),
            discoverable = listOf(d110m.copy(paired = false, rssi = -40), b21),
        )
        val state = ScannerState(scanner, MockBleTransport(), this)

        state.loadPairedDevices()
        advanceUntilIdle()
        state.startScan()
        advanceUntilIdle()

        val devices = state.devices.value
        assertEquals(listOf("C9:36:C4:07:25:63", "BB:BB:BB:BB:BB:BB"), devices.map { it.address })
        assertTrue(devices.first().paired, "a paired printer stays marked paired after a scan sees it")
        state.stopScan()
    }
}
