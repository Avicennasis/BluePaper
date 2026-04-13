package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    val sentCommands = mutableListOf<NiimbotPacket>()
    val sentRaw = mutableListOf<NiimbotPacket>()
    private val responseQueue = ArrayDeque<NiimbotPacket>()

    fun enqueueResponse(packet: NiimbotPacket) {
        responseQueue.addLast(packet)
    }

    fun enqueueResponses(vararg packets: NiimbotPacket) {
        packets.forEach { responseQueue.addLast(it) }
    }

    override suspend fun connect(device: ScannedDevice) {
        _connectionState.value = ConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            throw PrinterNotConnectedException()
        }
        sentCommands.add(packet)
        if (responseQueue.isEmpty()) {
            throw PrinterTimeoutException("No mock response queued")
        }
        return responseQueue.removeFirst()
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            throw PrinterNotConnectedException()
        }
        sentRaw.add(packet)
    }

    fun connectMock() {
        _connectionState.value = ConnectionState.CONNECTED
    }

    fun reset() {
        sentCommands.clear()
        sentRaw.clear()
        responseQueue.clear()
    }
}
