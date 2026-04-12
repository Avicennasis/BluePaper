package com.avicennasis.bluepaper.model

data class HeartbeatResponse(
    val closingState: Int?,
    val powerLevel: Int?,
    val paperState: Int?,
    val rfidReadState: Int?,
) {
    companion object {
        fun fromData(data: ByteArray): HeartbeatResponse {
            fun b(i: Int): Int = data[i].toInt() and 0xFF

            return when (data.size) {
                9 -> HeartbeatResponse(closingState = b(8), powerLevel = null, paperState = null, rfidReadState = null)
                10 -> HeartbeatResponse(closingState = b(8), powerLevel = b(9), paperState = null, rfidReadState = null)
                13 -> HeartbeatResponse(closingState = b(9), powerLevel = b(10), paperState = b(11), rfidReadState = b(12))
                19 -> HeartbeatResponse(closingState = b(15), powerLevel = b(16), paperState = b(17), rfidReadState = b(18))
                20 -> HeartbeatResponse(closingState = null, powerLevel = null, paperState = b(18), rfidReadState = b(19))
                else -> HeartbeatResponse(null, null, null, null)
            }
        }
    }
}
