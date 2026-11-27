package com.avicennasis.bluepaper.protocol

/**
 * Which reply packet types answer a request, so a transport can skip anything else the
 * printer sends meanwhile (0xD3 check-line notices, late replies to earlier commands).
 * Pairs follow niimbluelib's commandsMap and were confirmed against a D110_M.
 */
object ResponseCodes {
    /** The printer's unsolicited error report; it can arrive in place of any reply. */
    const val PRINT_ERROR = 0xDB

    private val replies: Map<Int, Set<Int>> = mapOf(
        RequestCode.GET_RFID.code to setOf(0x1B),
        RequestCode.HEARTBEAT.code to setOf(0xDD, 0xDE, 0xDF, 0xD9),
        RequestCode.SET_LABEL_TYPE.code to setOf(0x33),
        RequestCode.SET_LABEL_DENSITY.code to setOf(0x31),
        RequestCode.START_PRINT.code to setOf(0x02),
        RequestCode.END_PRINT.code to setOf(0xF4),
        RequestCode.START_PAGE_PRINT.code to setOf(0x04),
        RequestCode.END_PAGE_PRINT.code to setOf(0xE4),
        RequestCode.SET_DIMENSION.code to setOf(0x14),
        RequestCode.SET_QUANTITY.code to setOf(0x16),
        RequestCode.GET_PRINT_STATUS.code to setOf(0xB3),
        RequestCode.CONNECT.code to setOf(0xC2),
        RequestCode.PRINTER_STATUS_DATA.code to setOf(0xB5),
    )

    /** Accepted reply types for [request], or null when it has no fixed reply (e.g. image rows). */
    fun expectedReplies(request: NiimbotPacket): Set<Int>? = when (request.type) {
        // GetInfo answers with 0x40 + the requested key (e.g. battery, key 10 -> 0x4A).
        RequestCode.GET_INFO.code -> request.data.firstOrNull()?.let { setOf(0x40 + (it.toInt() and 0xFF)) }
        else -> replies[request.type]
    }

    /** Unsolicited "rows printed so far" notice sent during a page; never a reply. */
    const val PRINTER_CHECK_LINE = 0xD3

    /** Whether [reply] answers a request expecting [expected] (null: anything but a check-line notice). */
    fun isReply(reply: NiimbotPacket, expected: Set<Int>?): Boolean = when {
        reply.type == PRINT_ERROR -> true
        expected != null -> reply.type in expected
        else -> reply.type != PRINTER_CHECK_LINE
    }
}
