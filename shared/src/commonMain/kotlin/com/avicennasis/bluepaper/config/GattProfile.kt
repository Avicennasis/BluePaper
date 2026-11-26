package com.avicennasis.bluepaper.config

/**
 * Explicit GATT endpoints for printers that don't follow Niimbot's characteristic layout.
 *
 * When a [DeviceConfig] carries a non-null [GattProfile], the transport connects to and writes
 * on these UUIDs instead of auto-discovering the Niimbot characteristic.
 */
data class GattProfile(
    val serviceUuid: String,
    val writeCharUuid: String,
    val notifyCharUuid: String? = null,
    /** Phomemo D30 requires write-with-response on its ff02 characteristic. */
    val writeWithResponse: Boolean = false,
)
