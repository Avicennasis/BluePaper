package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterProtocolException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalUuidApi::class)
class KableBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var peripheral: Peripheral? = null
    private var notifyCharacteristic: Characteristic? = null
    private val responseChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val commandMutex = Mutex()
    private var scope: CoroutineScope? = null

    override suspend fun connect(device: ScannedDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        try {
            throw PrinterException(
                "KableBleTransport.connect() requires an Advertisement object from KableBleScanner. " +
                "Use connectWithPeripheral() instead."
            )
        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        }
    }

    suspend fun connectWithPeripheral(kablePeripheral: Peripheral) {
        _connectionState.value = ConnectionState.CONNECTING

        try {
            peripheral = kablePeripheral
            kablePeripheral.connect()

            val services = kablePeripheral.services.value
                ?: throw PrinterProtocolException("No services discovered")

            var foundCharacteristic: Characteristic? = null
            outer@ for (service in services) {
                for (char in service.characteristics) {
                    foundCharacteristic = characteristicOf(
                        service = service.serviceUuid,
                        characteristic = char.characteristicUuid,
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

            _connectionState.value = ConnectionState.CONNECTED
        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw PrinterException("Failed to connect: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING
        scope?.cancel()
        scope = null
        try {
            peripheral?.disconnect()
        } catch (_: Exception) { }
        peripheral = null
        notifyCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        val p = peripheral ?: throw PrinterNotConnectedException()
        val char = notifyCharacteristic ?: throw PrinterNotConnectedException()

        return commandMutex.withLock {
            while (responseChannel.tryReceive().isSuccess) { }

            p.write(char, packet.toBytes(), WriteType.WithoutResponse)

            val responseData = try {
                withTimeout(timeoutMs) {
                    responseChannel.receive()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw PrinterTimeoutException("No response within ${timeoutMs}ms")
            }

            NiimbotPacket.fromBytes(responseData)
        }
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        val p = peripheral ?: throw PrinterNotConnectedException()
        val char = notifyCharacteristic ?: throw PrinterNotConnectedException()

        p.write(char, packet.toBytes(), WriteType.WithoutResponse)
    }
}
