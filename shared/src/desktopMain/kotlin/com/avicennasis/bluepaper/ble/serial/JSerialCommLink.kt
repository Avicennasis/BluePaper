package com.avicennasis.bluepaper.ble.serial

import com.avicennasis.bluepaper.printer.PrinterException
import com.fazecast.jSerialComm.SerialPort

/** [SerialLink] over jSerialComm (Linux, macOS, Windows). */
class JSerialCommLink private constructor(private val port: SerialPort) : SerialLink {
    private var readTimeoutMs = -1

    override fun write(bytes: ByteArray) {
        val written = port.writeBytes(bytes, bytes.size)
        if (written != bytes.size) {
            throw PrinterException("Serial write to ${port.systemPortPath} failed ($written of ${bytes.size} bytes)")
        }
    }

    override fun read(buffer: ByteArray, timeoutMs: Int): Int {
        if (!port.isOpen) return -1
        if (timeoutMs != readTimeoutMs) {
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeoutMs, 0)
            readTimeoutMs = timeoutMs
        }
        return port.readBytes(buffer, buffer.size) // -1 on error, e.g. the cable was unplugged
    }

    override fun close() {
        port.closePort()
    }

    companion object {
        fun open(path: String): SerialLink {
            val port = SerialPort.getCommPort(path)
            port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
            if (!port.openPort()) {
                throw PrinterException(
                    "Couldn't open $path — it may be in use, or (on Linux) your user needs the dialout group",
                )
            }
            return JSerialCommLink(port)
        }

        fun listPorts(): List<SerialPortInfo> =
            SerialPort.getCommPorts().map { SerialPortInfo(it.systemPortPath, it.vendorID, it.productID) }
    }
}
