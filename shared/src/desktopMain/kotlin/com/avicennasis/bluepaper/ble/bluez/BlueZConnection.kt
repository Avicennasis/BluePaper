package com.avicennasis.bluepaper.ble.bluez

import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder

/**
 * Singleton D-Bus connection manager for BlueZ.
 * All BlueZ operations must use this shared connection to avoid conflicts.
 */
object BlueZConnection {
    private var connection: DBusConnection? = null
    private val lock = Any()

    fun get(): DBusConnection {
        synchronized(lock) {
            var conn = connection
            if (conn == null || !conn.isConnected) {
                println("[BlueZConnection] Creating new D-Bus connection...")
                conn = DBusConnectionBuilder.forSystemBus().build()
                connection = conn
                println("[BlueZConnection] D-Bus connection created")
            }
            return conn
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                connection?.close()
            } catch (_: Exception) { }
            connection = null
            println("[BlueZConnection] Connection closed")
        }
    }
}
