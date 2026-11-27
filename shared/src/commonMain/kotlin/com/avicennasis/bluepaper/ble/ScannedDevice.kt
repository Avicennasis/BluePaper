package com.avicennasis.bluepaper.ble

/** How BluePaper reaches a printer. */
enum class DeviceTransport { BLUETOOTH, USB }

data class ScannedDevice(
    val name: String,
    /** Bluetooth address, or the serial port path (e.g. /dev/ttyACM0) for [DeviceTransport.USB]. */
    val address: String,
    val rssi: Int,
    val serviceUuids: List<String>,
    /** True when the OS already has this device paired/bonded. */
    val paired: Boolean = false,
    val transport: DeviceTransport = DeviceTransport.BLUETOOTH,
)
