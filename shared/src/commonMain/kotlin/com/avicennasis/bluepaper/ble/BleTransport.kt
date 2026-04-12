package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.StateFlow

interface BleTransport {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(device: ScannedDevice)
    suspend fun disconnect()
    suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long = 10_000L): NiimbotPacket
    suspend fun writeRaw(packet: NiimbotPacket)
}
