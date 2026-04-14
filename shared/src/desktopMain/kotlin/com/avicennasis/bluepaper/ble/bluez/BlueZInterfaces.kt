package com.avicennasis.bluepaper.ble.bluez

import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * BlueZ D-Bus interface definitions for BLE operations.
 * See: https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/doc
 */

/** org.bluez.Adapter1 - Bluetooth adapter interface */
@DBusInterfaceName("org.bluez.Adapter1")
interface Adapter1 : DBusInterface {
    fun StartDiscovery()
    fun StopDiscovery()
    fun SetDiscoveryFilter(properties: Map<String, Variant<*>>)
}

/** org.bluez.Device1 - Bluetooth device interface */
@DBusInterfaceName("org.bluez.Device1")
interface Device1 : DBusInterface {
    fun Connect()
    fun Disconnect()
}

/** org.bluez.GattService1 - GATT service interface */
@DBusInterfaceName("org.bluez.GattService1")
interface GattService1 : DBusInterface {
    // Properties accessed via org.freedesktop.DBus.Properties
}

/** org.bluez.GattCharacteristic1 - GATT characteristic interface */
@DBusInterfaceName("org.bluez.GattCharacteristic1")
interface GattCharacteristic1 : DBusInterface {
    fun ReadValue(options: Map<String, Variant<*>>): ByteArray
    fun WriteValue(value: ByteArray, options: Map<String, Variant<*>>)
    fun StartNotify()
    fun StopNotify()
}

/** Helper to read properties from BlueZ objects */
object BlueZProperties {
    fun <T> getProperty(props: Properties, iface: String, name: String): T? {
        return try {
            @Suppress("UNCHECKED_CAST")
            props.Get<Variant<T>>(iface, name)?.value
        } catch (_: Exception) {
            null
        }
    }

    fun getStringProperty(props: Properties, iface: String, name: String): String? =
        getProperty<String>(props, iface, name)

    fun getBooleanProperty(props: Properties, iface: String, name: String): Boolean =
        getProperty<Boolean>(props, iface, name) ?: false

    fun getShortProperty(props: Properties, iface: String, name: String): Short =
        getProperty<Short>(props, iface, name) ?: 0

    fun getStringListProperty(props: Properties, iface: String, name: String): List<String> =
        getProperty<List<String>>(props, iface, name) ?: emptyList()

    fun getByteArrayProperty(props: Properties, iface: String, name: String): ByteArray =
        getProperty<ByteArray>(props, iface, name) ?: byteArrayOf()
}

/** BlueZ object path constants */
object BlueZPaths {
    const val BLUEZ_SERVICE = "org.bluez"
    const val ADAPTER_PATH = "/org/bluez/hci0"
    const val ADAPTER_INTERFACE = "org.bluez.Adapter1"
    const val DEVICE_INTERFACE = "org.bluez.Device1"
    const val GATT_SERVICE_INTERFACE = "org.bluez.GattService1"
    const val GATT_CHARACTERISTIC_INTERFACE = "org.bluez.GattCharacteristic1"

    fun devicePath(adapterPath: String, address: String): String {
        val sanitized = address.uppercase().replace(":", "_")
        return "$adapterPath/dev_$sanitized"
    }
}
