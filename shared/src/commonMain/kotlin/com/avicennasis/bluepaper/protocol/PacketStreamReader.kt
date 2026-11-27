package com.avicennasis.bluepaper.protocol

/**
 * Reassembles Niimbot packets from a byte stream. Bluetooth delivers one packet per
 * notification, but USB serial delivers arbitrary chunks: a packet can span reads and one
 * read can hold several packets. Bytes before a 0x55 0x55 header and frames that fail
 * validation are skipped.
 */
class PacketStreamReader {
    private var buffer = ByteArray(0)

    fun feed(bytes: ByteArray): List<NiimbotPacket> {
        buffer += bytes
        val packets = mutableListOf<NiimbotPacket>()
        while (true) {
            val start = headerIndex()
            if (start < 0) {
                // Keep a trailing 0x55: it may be the first half of the next header.
                buffer = if (buffer.lastOrNull() == NiimbotPacket.HEADER) byteArrayOf(NiimbotPacket.HEADER) else ByteArray(0)
                break
            }
            if (start > 0) buffer = buffer.copyOfRange(start, buffer.size)
            if (buffer.size < 4) break
            val frameSize = 4 + (buffer[3].toInt() and 0xFF) + 3
            if (buffer.size < frameSize) break
            val packet = runCatching { NiimbotPacket.fromBytes(buffer.copyOfRange(0, frameSize)) }.getOrNull()
            buffer = if (packet != null) {
                packets += packet
                buffer.copyOfRange(frameSize, buffer.size)
            } else {
                buffer.copyOfRange(1, buffer.size) // resynchronise past a corrupt frame
            }
        }
        return packets
    }

    private fun headerIndex(): Int {
        for (i in 0 until buffer.size - 1) {
            if (buffer[i] == NiimbotPacket.HEADER && buffer[i + 1] == NiimbotPacket.HEADER) return i
        }
        return -1
    }
}
