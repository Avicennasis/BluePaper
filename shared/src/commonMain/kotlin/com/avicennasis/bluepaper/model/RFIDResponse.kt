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

            val uuid = data.sliceArray(1 until 9)
                .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

            val barcodeLen = data[9].toInt() and 0xFF
            val barcode = data.decodeToString(10, 10 + barcodeLen)

            val serialOffset = 10 + barcodeLen
            val serialLen = data[serialOffset].toInt() and 0xFF
            val serial = data.decodeToString(serialOffset + 1, serialOffset + 1 + serialLen)

            val trailerOffset = serialOffset + 1 + serialLen
            val totalLen = ((data[trailerOffset].toInt() and 0xFF) shl 8) or
                (data[trailerOffset + 1].toInt() and 0xFF)
            val usedLen = ((data[trailerOffset + 2].toInt() and 0xFF) shl 8) or
                (data[trailerOffset + 3].toInt() and 0xFF)
            val type = data[trailerOffset + 4].toInt() and 0xFF

            return RFIDResponse(uuid, barcode, serial, totalLen, usedLen, type)
        }
    }
}
