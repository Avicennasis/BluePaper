package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.ScannedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.handlers.AbstractInterfacesAddedHandler
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.types.Variant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BLE scanner implementation for Linux using BlueZ D-Bus API.
 * Uses a shared connection to avoid D-Bus connection conflicts.
 */
class BlueZBleScanner : BleScanner {

    companion object {
        private val isScanning = AtomicBoolean(false)
    }

    /** Paired devices from BlueZ's object tree — includes printers already connected to the
     *  PC, which stop advertising and so never show up in discovery. */
    override suspend fun pairedDevices(): List<ScannedDevice> = withContext(Dispatchers.IO) {
        val objectManager = BlueZConnection.get().getRemoteObject(
            BlueZPaths.BLUEZ_SERVICE,
            "/",
            ObjectManager::class.java
        )
        objectManager.GetManagedObjects().mapNotNull { (path, interfaces) ->
            if (!path.path.startsWith(BlueZPaths.ADAPTER_PATH + "/dev_")) return@mapNotNull null
            val deviceProps = interfaces[BlueZPaths.DEVICE_INTERFACE] ?: return@mapNotNull null
            BlueZDevices.toScannedDevice(deviceProps)?.takeIf { it.paired }
        }.also { println("[BlueZBleScanner] Paired devices: ${it.map { d -> d.name }}") }
    }

    override fun scan(namePrefix: String): Flow<ScannedDevice> = callbackFlow {
        // Prevent multiple simultaneous scans
        if (!isScanning.compareAndSet(false, true)) {
            println("[BlueZBleScanner] Scan already in progress, skipping")
            close()
            return@callbackFlow
        }

        println("[BlueZBleScanner] Starting scan for prefix: $namePrefix")

        val connection = withContext(Dispatchers.IO) { BlueZConnection.get() }
        var handler: AbstractInterfacesAddedHandler? = null
        var adapter: Adapter1? = null

        try {
            adapter = connection.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                BlueZPaths.ADAPTER_PATH,
                Adapter1::class.java
            )
            println("[BlueZBleScanner] Got adapter object")

            // First, get existing devices
            val objectManager = connection.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                "/",
                ObjectManager::class.java
            )
            println("[BlueZBleScanner] Got object manager")

            val existingObjects = withContext(Dispatchers.IO) {
                objectManager.GetManagedObjects()
            }
            println("[BlueZBleScanner] Got ${existingObjects.size} managed objects")

            for ((path, interfaces) in existingObjects) {
                if (!path.path.startsWith(BlueZPaths.ADAPTER_PATH + "/dev_")) continue
                val deviceProps = interfaces[BlueZPaths.DEVICE_INTERFACE] ?: continue
                val device = BlueZDevices.toScannedDevice(deviceProps) ?: continue
                if (!device.name.lowercase().startsWith(namePrefix.lowercase())) continue

                println("[BlueZBleScanner] Found existing device: ${device.name} (${device.address}) UUIDs=${device.serviceUuids.size}")
                trySend(device)
            }

            // Set up handler for new devices
            handler = object : AbstractInterfacesAddedHandler() {
                override fun handle(signal: ObjectManager.InterfacesAdded) {
                    val interfaces = signal.interfaces ?: return
                    @Suppress("UNCHECKED_CAST")
                    val deviceProps = (interfaces[BlueZPaths.DEVICE_INTERFACE]
                        as? Map<String, Variant<*>>) ?: return
                    val device = BlueZDevices.toScannedDevice(deviceProps) ?: return
                    if (!device.name.lowercase().startsWith(namePrefix.lowercase())) return

                    println("[BlueZBleScanner] Found new device: ${device.name} (${device.address}) RSSI=${device.rssi}")
                    trySend(device)
                }
            }

            connection.addSigHandler(ObjectManager.InterfacesAdded::class.java, handler)

            // Start discovery
            withContext(Dispatchers.IO) {
                adapter.StartDiscovery()
            }
            println("[BlueZBleScanner] Discovery started")

            awaitClose {
                println("[BlueZBleScanner] Stopping discovery")
                isScanning.set(false)
                try {
                    adapter?.StopDiscovery()
                } catch (e: Exception) {
                    println("[BlueZBleScanner] Error stopping discovery: ${e.message}")
                }
                handler?.let {
                    try {
                        connection.removeSigHandler(ObjectManager.InterfacesAdded::class.java, it)
                    } catch (_: Exception) {}
                }
                // Don't close the shared connection
            }
        } catch (e: Exception) {
            println("[BlueZBleScanner] Error: ${e.message}")
            e.printStackTrace()
            isScanning.set(false)
            throw e
        }
    }
}
