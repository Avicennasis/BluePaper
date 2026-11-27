package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.config.GattProfile
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Routes each device to Bluetooth or USB serial; everything above the transport is shared. */
class DesktopTransport(
    private val bluetooth: BleTransport,
    private val usb: BleTransport,
) : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var active: BleTransport? = null

    override suspend fun connect(device: ScannedDevice) = connect(device, null)

    override suspend fun connect(device: ScannedDevice, gatt: GattProfile?) {
        val target = if (device.transport == DeviceTransport.USB) usb else bluetooth
        active?.takeIf { it !== target }?.disconnect()
        active = null
        _connectionState.value = ConnectionState.CONNECTING
        try {
            target.connect(device, gatt)
            active = target
            _connectionState.value = ConnectionState.CONNECTED
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        }
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING
        try {
            active?.disconnect()
        } finally {
            active = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket =
        (active ?: throw PrinterNotConnectedException()).sendCommand(packet, timeoutMs)

    override suspend fun writeRaw(packet: NiimbotPacket) =
        (active ?: throw PrinterNotConnectedException()).writeRaw(packet)

    override suspend fun writeBytes(data: ByteArray, withResponse: Boolean) =
        (active ?: throw PrinterNotConnectedException()).writeBytes(data, withResponse)
}
