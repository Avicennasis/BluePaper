package com.avicennasis.bluepaper.ui.scanner

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.config.DeviceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class ScannerState(
    private val scanner: BleScanner,
    private val transport: BleTransport,
    private val scope: CoroutineScope,
) {
    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    val connectionState: StateFlow<ConnectionState> = transport.connectionState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var scanJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        scanJob?.cancel()
        _isScanning.value = true
        _devices.value = emptyList()
        _error.value = null

        scanJob = scope.launch {
            try {
                val prefixes = DeviceRegistry.scanPrefixes()
                prefixes.map { scanner.scan(it) }.merge().collect { device ->
                    val current = _devices.value
                    if (current.none { it.address == device.address }) {
                        _devices.value = current + device
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    fun connectTo(device: ScannedDevice) {
        scope.launch {
            try {
                _error.value = null
                transport.connect(device)
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun disconnect() {
        scope.launch {
            transport.disconnect()
        }
    }
}
