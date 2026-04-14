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
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File

/**
 * BLE transport using Python Bleak via persistent subprocess.
 * More reliable than dbus-java for GATT operations.
 */
class BleakBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private val commandMutex = Mutex()

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

    private suspend fun ensureProcess(): Pair<BufferedWriter, BufferedReader> = withContext(Dispatchers.IO) {
        if (process == null || !process!!.isAlive) {
            val script = findBridgeScript()
            println("[BleakBleTransport] Starting bridge: python3 $script")

            process = ProcessBuilder("python3", script)
                .redirectErrorStream(true)
                .start()

            writer = process!!.outputStream.bufferedWriter()
            reader = process!!.inputStream.bufferedReader()

            // Wait for ready signal
            val ready = reader!!.readLine()
            println("[BleakBleTransport] Bridge: $ready")

            val readyJson = Json.parseToJsonElement(ready).jsonObject
            if (!readyJson.containsKey("ready")) {
                throw PrinterException("Bridge failed to start: $ready")
            }
        }
        Pair(writer!!, reader!!)
    }

    private suspend fun sendCommand(cmd: JsonObject): JsonObject = withContext(Dispatchers.IO) {
        val (w, r) = ensureProcess()

        val cmdStr = cmd.toString()
        println("[BleakBleTransport] Sending: $cmdStr")
        w.write(cmdStr)
        w.newLine()
        w.flush()

        val response = r.readLine() ?: throw PrinterException("Bridge closed unexpectedly")
        println("[BleakBleTransport] Received: $response")

        try {
            Json.parseToJsonElement(response).jsonObject
        } catch (e: Exception) {
            throw PrinterException("Invalid JSON from bridge: $response")
        }
    }

    override suspend fun connect(device: ScannedDevice) {
        println("[BleakBleTransport] connect() called for ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING

        try {
            val cmd = buildJsonObject {
                put("cmd", "connect")
                put("address", device.address)
            }

            val result = commandMutex.withLock { sendCommand(cmd) }

            if (result.containsKey("error")) {
                throw PrinterException(result["error"]!!.jsonPrimitive.content)
            }

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
            val cmd = buildJsonObject { put("cmd", "disconnect") }
            commandMutex.withLock { sendCommand(cmd) }
        } catch (_: Exception) { }

        // Kill the process
        try {
            val cmd = buildJsonObject { put("cmd", "quit") }
            writer?.write(cmd.toString())
            writer?.newLine()
            writer?.flush()
        } catch (_: Exception) { }

        process?.destroyForcibly()
        process = null
        writer = null
        reader = null

        _connectionState.value = ConnectionState.DISCONNECTED
        println("[BleakBleTransport] Disconnected")
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        val data = packet.toBytes()
        val hexData = data.joinToString("") { "%02x".format(it) }
        println("[BleakBleTransport] sendCommand: type=0x${packet.type.toString(16)}, data=$hexData")

        val cmd = buildJsonObject {
            put("cmd", "write")
            put("data", hexData)
        }

        val result = commandMutex.withLock { sendCommand(cmd) }

        if (result.containsKey("error")) {
            val error = result["error"]!!.jsonPrimitive.content
            if (error == "timeout") {
                throw PrinterTimeoutException("No response within ${timeoutMs}ms")
            }
            if (error == "not connected") {
                throw PrinterNotConnectedException()
            }
            throw PrinterException(error)
        }

        val responseHex = result["response"]?.jsonPrimitive?.content
            ?: throw PrinterException("No response data")

        val responseBytes = responseHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        println("[BleakBleTransport] Response: $responseHex")

        return NiimbotPacket.fromBytes(responseBytes)
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        val data = packet.toBytes()
        val hexData = data.joinToString("") { "%02x".format(it) }
        println("[BleakBleTransport] writeRaw: $hexData")

        val cmd = buildJsonObject {
            put("cmd", "write")
            put("data", hexData)
        }

        commandMutex.withLock { sendCommand(cmd) }
    }
}
