package com.avicennasis.bluepaper.ble

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    /** Discovers nearby devices whose name starts with [namePrefix] ("" = every named device). */
    fun scan(namePrefix: String): Flow<ScannedDevice>

    /** Devices the OS already has paired. Platforms that can't enumerate them return none. */
    suspend fun pairedDevices(): List<ScannedDevice> = emptyList()
}
