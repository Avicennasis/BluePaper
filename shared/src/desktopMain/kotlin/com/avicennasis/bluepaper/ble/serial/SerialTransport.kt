package com.avicennasis.bluepaper.ble.serial

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.protocol.PacketStreamReader
import com.avicennasis.bluepaper.protocol.PrinterErrors
import com.avicennasis.bluepaper.protocol.ResponseCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Gap between packets, as niimbluelib's serial client (packetIntervalMs = 10). */
private const val PACKET_INTERVAL_NS = 10_000_000L
private const val READ_POLL_MS = 100

/**
 * Talks to a printer over its USB serial port (a D110_M shows up as a CDC-ACM device). Same
 * packets as Bluetooth; replies are reassembled from the byte stream and matched by type.
 */
class SerialTransport(
    private val openLink: (String) -> SerialLink = JSerialCommLink::open,
) : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    @Volatile private var link: SerialLink? = null
    private var readerThread: Thread? = null
    private val received = LinkedBlockingQueue<NiimbotPacket>()
    private val mutex = Mutex()
    private var lastWriteNanos = 0L

    override suspend fun connect(device: ScannedDevice) = withContext(Dispatchers.IO) {
        closeLink()
        _connectionState.value = ConnectionState.CONNECTING
        try {
            val opened = openLink(device.address)
            received.clear()
            link = opened
            readerThread = thread(isDaemon = true, name = "niimbot-serial-reader") { readLoop(opened) }
            _connectionState.value = ConnectionState.CONNECTED
            println("[SerialTransport] Connected to ${device.name} on ${device.address}")
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e as? PrinterException ?: PrinterException("Couldn't open ${device.address}: ${e.message}", e)
        }
    }

    private fun readLoop(source: SerialLink) {
        val reader = PacketStreamReader()
        val buffer = ByteArray(512)
        while (link === source) {
            val count = try { source.read(buffer, READ_POLL_MS) } catch (_: Exception) { -1 }
            if (count < 0) {
                if (link === source) { // not a deliberate close: the port went away
                    link = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                    println("[SerialTransport] Port closed unexpectedly")
                }
                return
            }
            if (count > 0) reader.feed(buffer.copyOf(count)).forEach { received.put(it) }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        if (link != null) _connectionState.value = ConnectionState.DISCONNECTING
        closeLink()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun closeLink() {
        val current = link ?: return
        link = null
        runCatching { current.close() }
        readerThread?.join(1_000)
        readerThread = null
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket = mutex.withLock {
        withContext(Dispatchers.IO) {
            val current = link ?: throw PrinterNotConnectedException()
            received.clear() // anything still queued predates this command
            write(current, packet)
            val expected = ResponseCodes.expectedReplies(packet)
            val deadline = System.nanoTime() + timeoutMs * 1_000_000
            while (true) {
                val remainingMs = (deadline - System.nanoTime()) / 1_000_000
                if (remainingMs <= 0) {
                    throw PrinterTimeoutException("No reply to 0x${packet.type.toString(16)} within ${timeoutMs}ms")
                }
                val reply = received.poll(remainingMs, TimeUnit.MILLISECONDS) ?: continue
                if (!ResponseCodes.isReply(reply, expected)) continue
                if (reply.type == ResponseCodes.PRINT_ERROR) {
                    throw PrinterException(PrinterErrors.describe(reply.data.firstOrNull()?.let { it.toInt() and 0xFF } ?: -1))
                }
                return@withContext reply
            }
            @Suppress("UNREACHABLE_CODE")
            throw IllegalStateException()
        }
    }

    override suspend fun writeRaw(packet: NiimbotPacket) = mutex.withLock {
        withContext(Dispatchers.IO) { write(link ?: throw PrinterNotConnectedException(), packet) }
    }

    private fun write(target: SerialLink, packet: NiimbotPacket) {
        val waitNanos = PACKET_INTERVAL_NS - (System.nanoTime() - lastWriteNanos)
        if (waitNanos > 0) Thread.sleep(waitNanos / 1_000_000, (waitNanos % 1_000_000).toInt())
        target.write(packet.toBytes())
        lastWriteNanos = System.nanoTime()
    }
}
