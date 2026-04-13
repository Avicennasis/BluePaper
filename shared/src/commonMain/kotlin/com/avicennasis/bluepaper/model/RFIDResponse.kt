package com.avicennasis.bluepaper.model

data class RFIDResponse(
    val uuid: String,
    val barcode: String,
    val serial: String,
    val totalLen: Int,
    val usedLen: Int,
    val type: Int,
) {
    companion object {
        fun fromData(data: ByteArray): RFIDResponse? {
            if (data.isEmpty() || (data[0].toInt() and 0xFF) == 0) return null

            return try {
                // data[0] is the "has RFID" flag (non-zero = RFID present), not part of the UUID.
                // UUID occupies bytes 1..8 (8 bytes), so indices differ from the raw protocol
                // spec which documents UUID at data[0:8] — that spec assumes the flag byte has
                // already been stripped by the packet layer.
                val uuid = data.sliceArray(1 until 9)
                    .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

                if (data.size < 10) return null
                val barcodeLen = data[9].toInt() and 0xFF
                if (data.size < 10 + barcodeLen) return null
                val barcode = data.decodeToString(10, 10 + barcodeLen)

                val serialOffset = 10 + barcodeLen
                if (data.size < serialOffset + 1) return null
                val serialLen = data[serialOffset].toInt() and 0xFF
                if (data.size < serialOffset + 1 + serialLen) return null
                val serial = data.decodeToString(serialOffset + 1, serialOffset + 1 + serialLen)

                // trailer: total_len (uint16 BE), used_len (uint16 BE), type (uint8) — 5 bytes
                val trailerOffset = serialOffset + 1 + serialLen
                if (data.size < trailerOffset + 5) return null
                val totalLen = ((data[trailerOffset].toInt() and 0xFF) shl 8) or
                    (data[trailerOffset + 1].toInt() and 0xFF)
                val usedLen = ((data[trailerOffset + 2].toInt() and 0xFF) shl 8) or
                    (data[trailerOffset + 3].toInt() and 0xFF)
                val type = data[trailerOffset + 4].toInt() and 0xFF

                RFIDResponse(uuid, barcode, serial, totalLen, usedLen, type)
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        }
    }
}
