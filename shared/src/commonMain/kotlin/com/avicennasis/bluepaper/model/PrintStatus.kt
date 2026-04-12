package com.avicennasis.bluepaper.model

data class PrintStatus(
    val page: Int,
    val progress1: Int,
    val progress2: Int,
) {
    companion object {
        fun fromData(data: ByteArray): PrintStatus {
            require(data.size >= 4) { "PrintStatus needs at least 4 bytes, got ${data.size}" }
            val page = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val progress1 = data[2].toInt() and 0xFF
            val progress2 = data[3].toInt() and 0xFF
            return PrintStatus(page, progress1, progress2)
        }
    }
}
