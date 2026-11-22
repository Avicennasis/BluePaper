package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private class RecordingTransport(val name: String) : BleTransport {
    val calls = mutableListOf<String>()
    override val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override suspend fun connect(device: ScannedDevice) { calls += "connect"; connectionState.value = ConnectionState.CONNECTED }
    override suspend fun disconnect() { calls += "disconnect"; connectionState.value = ConnectionState.DISCONNECTED }
    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket { calls += "send"; return packet }
    override suspend fun writeRaw(packet: NiimbotPacket) { calls += "raw" }
}

class DesktopTransportTest {
    private val ble = RecordingTransport("ble")
    private val usb = RecordingTransport("usb")
    private val transport = DesktopTransport(bluetooth = ble, usb = usb)
    private fun device(t: DeviceTransport) = ScannedDevice("D110_M", "x", 0, emptyList(), transport = t)

    @Test
    fun `usb devices go over serial`() = runBlocking {
        transport.connect(device(DeviceTransport.USB))
        transport.sendCommand(CommandBuilder.heartbeat())
        assertEquals(listOf("connect", "send"), usb.calls)
        assertEquals(emptyList(), ble.calls)
        assertEquals(ConnectionState.CONNECTED, transport.connectionState.value)
    }

    @Test
    fun `bluetooth devices go over bluetooth and switching disconnects the other`() = runBlocking {
        transport.connect(device(DeviceTransport.USB))
        transport.connect(device(DeviceTransport.BLUETOOTH))
        transport.writeRaw(CommandBuilder.emptyRow(0, 1))
        assertEquals(listOf("connect", "disconnect"), usb.calls)
        assertEquals(listOf("connect", "raw"), ble.calls)
        transport.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
    }
}
