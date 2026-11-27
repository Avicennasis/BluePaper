package com.avicennasis.bluepaper.protocol

object ProtocolVersion {
    /** Protocol version from a PrinterStatusData (0xB5) reply, as niimbluelib parses it. */
    fun fromStatusData(data: ByteArray): Int {
        if (data.size < 13) return 0
        val n = (data[11].toInt() and 0xFF) * 100 + (data[12].toInt() and 0xFF)
        return when {
            n >= 302 -> 5
            n >= 300 -> 4
            n >= 204 -> 3
            else -> 0
        }
    }
}
