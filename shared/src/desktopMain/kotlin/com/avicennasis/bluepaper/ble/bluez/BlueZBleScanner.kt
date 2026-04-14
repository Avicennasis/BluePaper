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

                val name = (deviceProps["Name"]?.value as? String) ?: continue
                if (!name.lowercase().startsWith(namePrefix.lowercase())) continue

                val address = (deviceProps["Address"]?.value as? String) ?: continue
                val rssi = (deviceProps["RSSI"]?.value as? Short)?.toInt() ?: -100

                @Suppress("UNCHECKED_CAST")
                val uuids = (deviceProps["UUIDs"]?.value as? List<String>) ?: emptyList()

                val device = ScannedDevice(
                    name = name,
                    address = address,
                    rssi = rssi,
                    serviceUuids = uuids,
                )
                println("[BlueZBleScanner] Found existing device: $name ($address) UUIDs=${uuids.size}")
                trySend(device)
            }

            // Set up handler for new devices
            handler = object : AbstractInterfacesAddedHandler() {
                override fun handle(signal: ObjectManager.InterfacesAdded) {
                    val interfaces = signal.interfaces ?: return
                    @Suppress("UNCHECKED_CAST")
                    val deviceProps = (interfaces[BlueZPaths.DEVICE_INTERFACE]
                        as? Map<String, Variant<*>>) ?: return

                    val name = (deviceProps["Name"]?.value as? String) ?: return
                    if (!name.lowercase().startsWith(namePrefix.lowercase())) return

                    val address = (deviceProps["Address"]?.value as? String) ?: return
                    val rssi = (deviceProps["RSSI"]?.value as? Short)?.toInt() ?: -100

                    @Suppress("UNCHECKED_CAST")
                    val uuids = (deviceProps["UUIDs"]?.value as? List<String>) ?: emptyList()

                    val device = ScannedDevice(
                        name = name,
                        address = address,
                        rssi = rssi,
                        serviceUuids = uuids,
                    )

                    println("[BlueZBleScanner] Found new device: $name ($address) RSSI=$rssi")
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
