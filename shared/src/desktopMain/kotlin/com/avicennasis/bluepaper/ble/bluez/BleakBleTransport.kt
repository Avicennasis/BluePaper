package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.File

/**
 * BLE transport using Python Bleak via subprocess.
 * More reliable than dbus-java for GATT operations.
 */
class BleakBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var connectedAddress: String? = null
    private val commandMutex = Mutex()
    private var pythonProcess: Process? = null

    companion object {
        private fun findBridgeScript(): String {
            // Try to find the script in various locations
            val locations = listOf(
                "ble_bridge.py",
                "shared/src/desktopMain/resources/ble_bridge.py",
                System.getProperty("user.home") + "/.local/share/bluepaper/ble_bridge.py",
            )
            for (loc in locations) {
                if (File(loc).exists()) return loc
            }
            // Fall back to classpath resource
            val resource = BleakBleTransport::class.java.getResource("/ble_bridge.py")
            if (resource != null) {
                // Extract to temp file
                val tempFile = File.createTempFile("ble_bridge", ".py")
                tempFile.deleteOnExit()
                resource.openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return tempFile.absolutePath
            }
            throw PrinterException("ble_bridge.py not found")
        }

        private suspend fun runBridge(vararg args: String): JsonObject = withContext(Dispatchers.IO) {
            val script = findBridgeScript()
            val cmd = listOf("python3", script) + args.toList()
            println("[BleakBleTransport] Running: ${cmd.joinToString(" ")}")

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            println("[BleakBleTransport] Output: $output")

            if (output.isBlank()) {
                throw PrinterException("No output from ble_bridge.py")
            }

            try {
                Json.parseToJsonElement(output).jsonObject
            } catch (e: Exception) {
                throw PrinterException("Invalid JSON from ble_bridge.py: $output")
            }
        }
    }

    override suspend fun connect(device: ScannedDevice) {
        println("[BleakBleTransport] connect() called for ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING

        try {
            val result = runBridge("connect", device.address)

            if (result.containsKey("error")) {
                throw PrinterException(result["error"]!!.jsonPrimitive.content)
            }

            connectedAddress = device.address
            _connectionState.value = ConnectionState.CONNECTED
            println("[BleakBleTransport] Connection established successfully")

        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw PrinterException("Failed to connect: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        println("[BleakBleTransport] disconnect() called")
        _connectionState.value = ConnectionState.DISCONNECTING

        try {
            runBridge("disconnect")
        } catch (_: Exception) { }

        connectedAddress = null
        _connectionState.value = ConnectionState.DISCONNECTED
        println("[BleakBleTransport] Disconnected")
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        val data = packet.toBytes()
        val hexData = data.joinToString("") { "%02x".format(it) }
        println("[BleakBleTransport] sendCommand: type=0x${packet.type.toString(16)}, data=$hexData")

        return commandMutex.withLock {
            if (connectedAddress == null) throw PrinterNotConnectedException()

            val result = runBridge("write", hexData)

            if (result.containsKey("error")) {
                val error = result["error"]!!.jsonPrimitive.content
                if (error == "timeout") {
                    throw PrinterTimeoutException("No response within ${timeoutMs}ms")
                }
                throw PrinterException(error)
            }

            val responseHex = result["response"]?.jsonPrimitive?.content
                ?: throw PrinterException("No response data")

            val responseBytes = responseHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            println("[BleakBleTransport] Response: $responseHex")

            NiimbotPacket.fromBytes(responseBytes)
        }
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        val data = packet.toBytes()
        val hexData = data.joinToString("") { "%02x".format(it) }
        println("[BleakBleTransport] writeRaw: $hexData")

        commandMutex.withLock {
            if (connectedAddress == null) throw PrinterNotConnectedException()
            runBridge("write", hexData)
        }
    }
}
