package com.avicennasis.bluepaper.ble.serial

/** A byte pipe to a serial port, so the transport can be tested without hardware. */
interface SerialLink : AutoCloseable {
    fun write(bytes: ByteArray)

    /** Reads into [buffer], waiting up to [timeoutMs]; returns the byte count, 0 on timeout, -1 once closed. */
    fun read(buffer: ByteArray, timeoutMs: Int): Int
}

data class SerialPortInfo(val path: String, val vendorId: Int, val productId: Int)
