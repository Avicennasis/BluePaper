package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.ble.bluez.BlueZBleScanner
import com.avicennasis.bluepaper.ble.bluez.BlueZBleTransport
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Factory for creating platform-appropriate BLE implementations on desktop.
 */
object DesktopBleFactory {

    private val os: String = System.getProperty("os.name").lowercase()

    val isLinux: Boolean = os.contains("linux")
    val isMacOS: Boolean = os.contains("mac")
    val isWindows: Boolean = os.contains("windows")

    fun createScanner(): BleScanner {
        return when {
            isLinux -> BlueZBleScanner()
            else -> StubBleScanner()
        }
    }

    fun createTransport(): BleTransport {
        return when {
            isLinux -> BlueZBleTransport()
            else -> StubBleTransport()
        }
    }
}

/** Stub scanner for unsupported platforms */
internal class StubBleScanner : BleScanner {
    override fun scan(namePrefix: String): Flow<ScannedDevice> {
        println("[StubBleScanner] BLE scanning not supported on this platform")
        return emptyFlow()
    }
}

/** Stub transport for unsupported platforms */
internal class StubBleTransport : BleTransport {
    override val connectionState: StateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.DISCONNECTED)

    override suspend fun connect(device: ScannedDevice) {
        println("[StubBleTransport] BLE not supported on this platform")
    }

    override suspend fun disconnect() { }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket =
        NiimbotPacket(packet.type, byteArrayOf(0x01))

    override suspend fun writeRaw(packet: NiimbotPacket) { }
}
