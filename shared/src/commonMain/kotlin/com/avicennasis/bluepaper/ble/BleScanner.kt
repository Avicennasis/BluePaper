package com.avicennasis.bluepaper.ble

import kotlinx.coroutines.flow.Flow

interface BleScanner {
    fun scan(namePrefix: String): Flow<ScannedDevice>
}
