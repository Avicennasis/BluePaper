package com.avicennasis.bluepaper.model

import com.avicennasis.bluepaper.protocol.NiimbotPacket

/** Lid / labels / RFID state from a heartbeat reply; null fields mean "can't tell". */
data class HeartbeatStatus(val lidClosed: Boolean?, val paperInserted: Boolean?, val rfidRead: Boolean?) {
    companion object {
        /**
         * Decodes only the "advanced 2" reply (0xD9, the answer to heartbeat type 4), whose layout
         * (niimbluelib parseHeartbeatAdvanced2Response) was confirmed on a D110_M. Other replies
         * put these bytes in model-specific places — the basic 0xDD reply reads "lid open" on a
         * closed D110_M — so they are reported as unknown.
         */
        fun fromReply(packet: NiimbotPacket): HeartbeatStatus {
            val d = packet.data
            if (packet.type != 0xD9 || d.size < 7) return HeartbeatStatus(null, null, null)
            return HeartbeatStatus(
                lidClosed = d[4].toInt() == 0,
                paperInserted = d[5].toInt() == 0,
                rfidRead = d[6].toInt() != 0,
            )
        }
    }
}
