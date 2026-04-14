package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.ScannedDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.handlers.AbstractInterfacesAddedHandler
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.types.Variant

/**
 * BLE scanner implementation for Linux using BlueZ D-Bus API.
 */
class BlueZBleScanner : BleScanner {

    override fun scan(namePrefix: String): Flow<ScannedDevice> = callbackFlow {
        val connection = DBusConnectionBuilder.forSystemBus().build()

        try {
            val adapter = connection.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                BlueZPaths.ADAPTER_PATH,
                Adapter1::class.java
            )

            // Set discovery filter for BLE devices
            val filter = mapOf<String, Variant<*>>(
                "Transport" to Variant("le"),
                "DuplicateData" to Variant(true),
            )
            adapter.SetDiscoveryFilter(filter)

            // Handler for new devices discovered
            val handler = object : AbstractInterfacesAddedHandler() {
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

                    println("[BlueZBleScanner] Found device: $name ($address) RSSI=$rssi")
                    trySend(device)
                }
            }

            // Also scan existing devices that were already discovered
            val objectManager = connection.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                "/",
                ObjectManager::class.java
            )

            val existingObjects = objectManager.GetManagedObjects()
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
                println("[BlueZBleScanner] Found existing device: $name ($address)")
                trySend(device)
            }

            connection.addSigHandler(ObjectManager.InterfacesAdded::class.java, handler)
            adapter.StartDiscovery()
            println("[BlueZBleScanner] Started discovery for prefix: $namePrefix")

            awaitClose {
                println("[BlueZBleScanner] Stopping discovery")
                try {
                    adapter.StopDiscovery()
                } catch (_: Exception) { }
                connection.removeSigHandler(ObjectManager.InterfacesAdded::class.java, handler)
                connection.close()
            }
        } catch (e: Exception) {
            println("[BlueZBleScanner] Error: ${e.message}")
            connection.close()
            throw e
        }
    }
}
