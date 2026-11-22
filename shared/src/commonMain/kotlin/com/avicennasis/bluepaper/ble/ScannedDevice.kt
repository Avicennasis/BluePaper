package com.avicennasis.bluepaper.ble

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    /** True when the OS already has this device paired/bonded. */
    val paired: Boolean = false,
)
