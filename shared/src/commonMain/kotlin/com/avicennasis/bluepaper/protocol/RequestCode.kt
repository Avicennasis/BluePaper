package com.avicennasis.bluepaper.protocol

enum class RequestCode(val code: Int) {
    GET_INFO(0x40),
    GET_RFID(0x1A),
    HEARTBEAT(0xDC),
    SET_LABEL_TYPE(0x23),
    SET_LABEL_DENSITY(0x21),
    START_PRINT(0x01),
    END_PRINT(0xF3),
    START_PAGE_PRINT(0x03),
    END_PAGE_PRINT(0xE3),
    SET_DIMENSION(0x13),
    SET_QUANTITY(0x15),
    GET_PRINT_STATUS(0xA3),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }
        fun fromCode(code: Int): RequestCode? = byCode[code]
    }
}
