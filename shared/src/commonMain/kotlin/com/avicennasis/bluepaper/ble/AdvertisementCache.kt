package com.avicennasis.bluepaper.ble

/**
 * Abstraction for caching BLE advertisements observed during scanning,
 * so that the transport layer can retrieve them later when connecting.
 *
 * The [Any] type is used because the concrete advertisement type (e.g.
 * Kable's `Advertisement`) is platform-specific and not available in
 * common code.
 */
interface AdvertisementCache {
    fun cache(deviceId: String, advertisement: Any)
    fun get(deviceId: String): Any?
}
