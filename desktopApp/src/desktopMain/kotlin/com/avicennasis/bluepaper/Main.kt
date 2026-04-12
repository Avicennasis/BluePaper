package com.avicennasis.bluepaper

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.avicennasis.bluepaper.ble.*
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.ui.BluePaperApp
import com.avicennasis.bluepaper.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

class DesktopBleScanner : BleScanner {
    override fun scan(namePrefix: String): Flow<ScannedDevice> = emptyFlow()
}

class DesktopBleTransport : BleTransport {
    override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)
    override suspend fun connect(device: ScannedDevice) { }
    override suspend fun disconnect() { }
    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket =
        NiimbotPacket(packet.type, byteArrayOf(0x01))
    override suspend fun writeRaw(packet: NiimbotPacket) { }
}

fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    Window(
        onCloseRequest = ::exitApplication,
        title = "BluePaper",
        state = rememberWindowState(width = 500.dp, height = 900.dp),
    ) {
        BluePaperApp(DesktopBleScanner(), DesktopBleTransport(), scope, startScreen = Screen.Editor)
    }
}
