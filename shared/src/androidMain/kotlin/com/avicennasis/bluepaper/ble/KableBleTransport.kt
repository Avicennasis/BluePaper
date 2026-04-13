package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterProtocolException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.juul.kable.Advertisement
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalUuidApi::class)
class KableBleTransport : BleTransport, AdvertisementCache {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var peripheral: Peripheral? = null
    private var notifyCharacteristic: Characteristic? = null
    private var responseChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val commandMutex = Mutex()
    private var scope: CoroutineScope? = null

    /** Cache of advertisements seen during scanning, keyed by device address. */
    private val advertisementMap = ConcurrentHashMap<String, Advertisement>()
    private companion object {
        const val MAX_CACHE_SIZE = 50
    }

    override fun cache(deviceId: String, advertisement: Any) {
        require(advertisement is Advertisement) {
            "KableBleTransport expects a Kable Advertisement, got ${advertisement::class}"
        }
        advertisementMap[deviceId] = advertisement
        // Evict oldest entries if cache exceeds cap
        while (advertisementMap.size > MAX_CACHE_SIZE) {
            val oldest = advertisementMap.keys().asIterator().next()
            advertisementMap.remove(oldest)
        }
        println("[KableBleTransport] Cached advertisement for $deviceId")
    }

    override fun get(deviceId: String): Any? = advertisementMap[deviceId]

    /**
     * Clear the advertisement cache. Call when scanning stops to free memory.
     */
    fun clearCache() {
        advertisementMap.clear()
        println("[KableBleTransport] Advertisement cache cleared")
    }

    override suspend fun connect(device: ScannedDevice) {
        println("[KableBleTransport] connect() called for ${device.name} (${device.address})")
        val advertisement = advertisementMap[device.address] as? Advertisement
            ?: throw PrinterException(
                "No cached advertisement for address ${device.address}. " +
                "Ensure the scanner is using this transport's AdvertisementCache."
            )

        val kablePeripheral = Peripheral(advertisement)
        connectWithPeripheral(kablePeripheral)
    }

    suspend fun connectWithPeripheral(kablePeripheral: Peripheral) {
        println("[KableBleTransport] connectWithPeripheral() starting")
        _connectionState.value = ConnectionState.CONNECTING
        // Recreate channel to ensure clean state (old one may have been closed by disconnect)
        responseChannel = Channel(Channel.UNLIMITED)

        try {
            peripheral = kablePeripheral
            kablePeripheral.connect()
            println("[KableBleTransport] BLE connected, discovering services...")

            val services = kablePeripheral.services.value
                ?: throw PrinterProtocolException("No services discovered")

            // TODO: Filter characteristics by properties (read + write-without-response + notify)
            // when Kable exposes CharacteristicProperty API. For now, prefer characteristics
            // from non-standard services (skip Device Information, Generic Access, etc.)
            var foundCharacteristic: Characteristic? = null
            outer@ for (service in services) {
                val uuid = service.serviceUuid.toString().lowercase()
                // Skip well-known GATT services (0x180x range)
                if (uuid.startsWith("0000180")) continue
                for (char in service.characteristics) {
                    foundCharacteristic = characteristicOf(
                        service = service.serviceUuid,
                        characteristic = char.characteristicUuid,
                    )
                    println(
                        "[KableBleTransport] Found characteristic: " +
                        "service=${service.serviceUuid}, char=${char.characteristicUuid}"
                    )
                    break@outer
                }
            }

            if (foundCharacteristic == null) {
                throw PrinterProtocolException("Cannot find BLE characteristic with read + write-without-response + notify")
            }

            notifyCharacteristic = foundCharacteristic

            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            scope!!.launch {
                kablePeripheral.observe(foundCharacteristic).collect { data ->
                    responseChannel.send(data)
                }
            }

            scope!!.launch {
                kablePeripheral.state.collect { state ->
                    _connectionState.value = when (state) {
                        is State.Connecting -> ConnectionState.CONNECTING
                        is State.Connected -> ConnectionState.CONNECTED
                        is State.Disconnecting -> ConnectionState.DISCONNECTING
                        is State.Disconnected -> ConnectionState.DISCONNECTED
                    }
                }
            }

            // State is managed by the kablePeripheral.state monitoring coroutine above
            println("[KableBleTransport] Connection established successfully")
        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw PrinterException("Failed to connect: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        println("[KableBleTransport] disconnect() called")
        _connectionState.value = ConnectionState.DISCONNECTING
        scope?.cancel()
        scope = null
        try {
            peripheral?.disconnect()
        } catch (_: Exception) { }
        peripheral = null
        notifyCharacteristic = null
        // Close the channel so any suspended receive() throws immediately
        responseChannel.close()
        _connectionState.value = ConnectionState.DISCONNECTED
        println("[KableBleTransport] Disconnected")
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        println("[KableBleTransport] sendCommand: type=0x${packet.type.toString(16)}, ${packet.toBytes().size} bytes, timeout=${timeoutMs}ms")

        return commandMutex.withLock {
            val p = peripheral ?: throw PrinterNotConnectedException()
            val char = notifyCharacteristic ?: throw PrinterNotConnectedException()

            while (responseChannel.tryReceive().isSuccess) { }

            p.write(char, packet.toBytes(), WriteType.WithoutResponse)

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
        println("[KableBleTransport] writeRaw: ${packet.toBytes().size} bytes")
        commandMutex.withLock {
            val p = peripheral ?: throw PrinterNotConnectedException()
            val char = notifyCharacteristic ?: throw PrinterNotConnectedException()
            p.write(char, packet.toBytes(), WriteType.WithoutResponse)
        }
    }
}
