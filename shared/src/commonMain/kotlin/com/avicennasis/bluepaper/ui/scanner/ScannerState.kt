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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
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
        // Paired printers stay listed — they may not advertise, so a scan can't re-find them.
        _devices.update { current -> current.filter { it.paired } }
        _error.value = null

        scanJob = scope.launch {
            try {
                // ONE discovery session filtered here. BlueZ allows a single session per
                // client (concurrent per-prefix scans got only the first prefix's results)
                // and Android throttles scanners started in quick succession.
                scanner.scan("")
                    .filter { DeviceRegistry.isSupportedPrinterName(it.name) }
                    .collect { addOrMerge(it) }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }

    /** Lists printers the OS already has paired, without needing a scan. */
    fun loadPairedDevices() {
        scope.launch {
            try {
                scanner.pairedDevices()
                    .filter { DeviceRegistry.isSupportedPrinterName(it.name) }
                    .forEach { addOrMerge(it.copy(paired = true)) }
            } catch (e: Exception) {
                _error.value = "Could not list paired printers: ${e.message}"
            }
        }
    }

    private fun addOrMerge(device: ScannedDevice) {
        _devices.update { current ->
            val index = current.indexOfFirst { it.address == device.address }
            if (index < 0) {
                current + device
            } else {
                val existing = current[index]
                current.toMutableList().also { it[index] = existing.copy(paired = existing.paired || device.paired) }
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
