package com.avicennasis.bluepaper.ble.serial

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Scripted serial device: [respond] maps each written frame to the byte chunks it "sends back". */
class FakeSerialLink(private val respond: (ByteArray) -> List<ByteArray> = { emptyList() }) : SerialLink {
    val written = mutableListOf<ByteArray>()
    val writeTimesNanos = mutableListOf<Long>()
    var closed = false
    private val incoming = LinkedBlockingQueue<ByteArray>()

    override fun write(bytes: ByteArray) {
        check(!closed) { "closed" }
        written += bytes
        writeTimesNanos += System.nanoTime()
        respond(bytes).forEach { incoming.put(it) }
    }

    override fun read(buffer: ByteArray, timeoutMs: Int): Int {
        if (closed) return -1
        val chunk = incoming.poll(timeoutMs.toLong(), TimeUnit.MILLISECONDS) ?: return 0
        chunk.copyInto(buffer)
        return chunk.size
    }

    override fun close() { closed = true }
}
