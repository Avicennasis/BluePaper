package com.avicennasis.bluepaper.protocol

enum class InfoEnum(val key: Int) {
    DENSITY(1),
    PRINT_SPEED(2),
    LABEL_TYPE(3),
    LANGUAGE_TYPE(6),
    AUTO_SHUTDOWN_TIME(7),
    DEVICE_TYPE(8),
    SOFTWARE_VERSION(9),
    BATTERY(10),
    DEVICE_SERIAL(11),
    HARDWARE_VERSION(12),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }
        fun fromKey(key: Int): InfoEnum? = byKey[key]
    }
}
