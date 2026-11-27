package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.ble.serial.UsbPrinterFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Bluetooth discovery plus printers on USB, which are listed alongside paired Bluetooth printers. */
class DesktopScanner(
    private val bluetooth: BleScanner,
    private val usb: UsbPrinterFinder,
) : BleScanner {

    override fun scan(namePrefix: String): Flow<ScannedDevice> = bluetooth.scan(namePrefix)

    override suspend fun pairedDevices(): List<ScannedDevice> {
        val bluetoothResult = runCatching { bluetooth.pairedDevices() }
        val usbPrinters = withContext(Dispatchers.IO) {
            runCatching { usb.find() }.getOrElse {
                println("[DesktopScanner] USB lookup failed: ${it.message}")
                emptyList()
            }
        }
        val paired = bluetoothResult.getOrElse { error ->
            if (usbPrinters.isEmpty()) throw error // nothing to show; let the UI report it
            println("[DesktopScanner] Bluetooth lookup failed: ${error.message}")
            emptyList()
        }
        return paired + usbPrinters
    }
}
