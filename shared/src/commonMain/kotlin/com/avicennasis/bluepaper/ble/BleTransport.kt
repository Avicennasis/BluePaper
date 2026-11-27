package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.config.GattProfile
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.StateFlow

interface BleTransport {
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(device: ScannedDevice)

    /**
     * Connect using explicit GATT endpoints for printers that aren't Niimbot (e.g. Phomemo D30).
     * Transports that don't support a custom profile fall back to [connect].
     */
    suspend fun connect(device: ScannedDevice, gatt: GattProfile?) {
        connect(device)
    }

    suspend fun disconnect()
    suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long = 10_000L): NiimbotPacket
    suspend fun writeRaw(packet: NiimbotPacket)

    /** Raw GATT write to the selected characteristic. Defaults to unsupported. */
    suspend fun writeBytes(data: ByteArray, withResponse: Boolean = false) {
        throw PrinterException("Raw byte writes are not supported by this transport")
    }
}
