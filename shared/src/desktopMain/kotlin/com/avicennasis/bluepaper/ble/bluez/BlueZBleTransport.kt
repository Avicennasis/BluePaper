package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterProtocolException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * BLE transport implementation for Linux using BlueZ D-Bus API.
 */
class BlueZBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var device: Device1? = null
    private var characteristic: GattCharacteristic1? = null
    private var characteristicPath: String? = null
    private var responseChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val commandMutex = Mutex()
    private var notifyHandler: AbstractPropertiesChangedHandler? = null

    override suspend fun connect(device: ScannedDevice) {
        println("[BlueZBleTransport] connect() called for ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING
        responseChannel = Channel(Channel.UNLIMITED)

        try {
            val conn = BlueZConnection.get()

            val devicePath = BlueZPaths.devicePath(BlueZPaths.ADAPTER_PATH, device.address)
            println("[BlueZBleTransport] Device path: $devicePath")

            val bluezDevice = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                devicePath,
                Device1::class.java
            )
            this.device = bluezDevice

            // Connect to the device
            withContext(Dispatchers.IO) {
                bluezDevice.Connect()
            }
            println("[BlueZBleTransport] Connected to device, discovering services...")

            // Wait briefly for services to be discovered
            withContext(Dispatchers.IO) {
                Thread.sleep(2000)
            }

            // Find the GATT characteristic for Niimbot printer
            val objectManager = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                "/",
                ObjectManager::class.java
            )

            val objects = withContext(Dispatchers.IO) {
                objectManager.GetManagedObjects()
            }
            var foundCharPath: String? = null

            for ((path, interfaces) in objects) {
                if (!path.path.startsWith(devicePath)) continue
                if (BlueZPaths.GATT_CHARACTERISTIC_INTERFACE !in interfaces) continue

                // Skip standard GATT characteristics (UUID starts with 0000)
                val charProps = interfaces[BlueZPaths.GATT_CHARACTERISTIC_INTERFACE] ?: continue
                val uuid = (charProps["UUID"]?.value as? String) ?: continue
                if (uuid.startsWith("0000")) continue

                foundCharPath = path.path
                println("[BlueZBleTransport] Found characteristic: $uuid at $foundCharPath")
                break
            }

            if (foundCharPath == null) {
                throw PrinterProtocolException("No suitable GATT characteristic found")
            }

            characteristicPath = foundCharPath
            val gattChar = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                foundCharPath,
                GattCharacteristic1::class.java
            )
            characteristic = gattChar

            // Set up notification handler for PropertiesChanged signals
            val charPath = foundCharPath
            notifyHandler = object : AbstractPropertiesChangedHandler() {
                override fun handle(signal: Properties.PropertiesChanged) {
                    if (signal.path != charPath) return
                    val props = signal.propertiesChanged ?: return
                    val value = props["Value"]?.value
                    if (value is ByteArray) {
                        println("[BlueZBleTransport] Received notification: ${value.size} bytes")
                        responseChannel.trySend(value)
                    }
                }
            }

            conn.addSigHandler(Properties.PropertiesChanged::class.java, notifyHandler)

            // Start notifications
            withContext(Dispatchers.IO) {
                gattChar.StartNotify()
            }
            println("[BlueZBleTransport] Started notifications")

            _connectionState.value = ConnectionState.CONNECTED
            println("[BlueZBleTransport] Connection established successfully")

        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw PrinterException("Failed to connect: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        println("[BlueZBleTransport] disconnect() called")
        _connectionState.value = ConnectionState.DISCONNECTING

        try {
            characteristic?.StopNotify()
        } catch (_: Exception) { }

        notifyHandler?.let { handler ->
            try {
                BlueZConnection.get().removeSigHandler(Properties.PropertiesChanged::class.java, handler)
            } catch (_: Exception) { }
        }

        try {
            device?.Disconnect()
        } catch (_: Exception) { }

        // Don't close the shared connection

        device = null
        characteristic = null
        characteristicPath = null
        notifyHandler = null
        responseChannel.close()

        _connectionState.value = ConnectionState.DISCONNECTED
        println("[BlueZBleTransport] Disconnected")
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        val data = packet.toBytes()
        println("[BlueZBleTransport] sendCommand: type=0x${packet.type.toString(16)}, ${data.size} bytes, data=${data.joinToString(" ") { "%02x".format(it) }}")

        return commandMutex.withLock {
            val char = characteristic ?: throw PrinterNotConnectedException()

            // Clear any stale responses
            while (responseChannel.tryReceive().isSuccess) { }

            // Write the command with options
            val options = mapOf<String, Variant<*>>(
                "type" to Variant("command"),
            )
            try {
                withContext(Dispatchers.IO) {
                    char.WriteValue(data, options)
                }
                println("[BlueZBleTransport] WriteValue completed")
            } catch (e: Exception) {
                println("[BlueZBleTransport] WriteValue failed: ${e.message}")
                e.printStackTrace()
                throw PrinterException("Write failed: ${e.message}", e)
            }

            // Wait for response
            val responseData = try {
                withTimeout(timeoutMs) {
                    responseChannel.receive()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw PrinterTimeoutException("No response within ${timeoutMs}ms")
            } catch (e: ClosedReceiveChannelException) {
                throw PrinterNotConnectedException()
            }

            NiimbotPacket.fromBytes(responseData)
        }
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        val data = packet.toBytes()
        println("[BlueZBleTransport] writeRaw: ${data.size} bytes")
        commandMutex.withLock {
            val char = characteristic ?: throw PrinterNotConnectedException()
            val options = mapOf<String, Variant<*>>(
                "type" to Variant("command"),
            )
            withContext(Dispatchers.IO) {
                char.WriteValue(data, options)
            }
        }
    }
}
