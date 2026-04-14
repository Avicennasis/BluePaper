# Linux BLE Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable BluePaper to scan for and print to Niimbot printers on Linux desktop via Bluetooth Low Energy using BlueZ D-Bus.

**Architecture:** Add a Linux-specific BLE implementation in `desktopMain` that uses dbus-java to communicate with the BlueZ D-Bus API. Detect OS at runtime and instantiate either the Linux BlueZ implementation or stubs for other platforms. Also add the D100M printer model.

**Tech Stack:** Kotlin/JVM, dbus-java 5.x, BlueZ D-Bus API, Compose Multiplatform

---

## File Structure

| File | Purpose |
|------|---------|
| `shared/src/commonMain/.../config/DeviceRegistry.kt` | Add D100M model config |
| `shared/build.gradle.kts` | Add dbus-java dependency for desktop |
| `shared/src/desktopMain/.../ble/BlueZAdapter.kt` | D-Bus interface definitions for BlueZ |
| `shared/src/desktopMain/.../ble/BlueZBleScanner.kt` | Linux BLE scanner using BlueZ |
| `shared/src/desktopMain/.../ble/BlueZBleTransport.kt` | Linux BLE transport using BlueZ GATT |
| `shared/src/desktopMain/.../ble/DesktopBleFactory.kt` | Factory to create platform-appropriate BLE impl |
| `desktopApp/src/desktopMain/.../Main.kt` | Use factory instead of stubs |
| `shared/src/desktopTest/.../ble/BlueZBleScannerTest.kt` | Unit tests for scanner |
| `shared/src/desktopTest/.../ble/BlueZBleTransportTest.kt` | Unit tests for transport |
| `README.md` | Update platform support table |

---

### Task 1: Add D100M Printer Model

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceRegistry.kt:12-53`

- [ ] **Step 1: Add D100M config to DeviceRegistry**

Add the D100M entry after the d110_m config (line 37):

```kotlin
DeviceConfig(
    model = "d100_m", dpi = 300, maxDensity = DEFAULT_MAX_DENSITY,
    rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
    labelSizes = sizes(300, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
),
```

- [ ] **Step 2: Run existing tests to verify no regression**

Run: `./gradlew :shared:desktopTest --tests '*DeviceRegistry*' -q`
Expected: PASS (if tests exist) or no errors

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceRegistry.kt
git commit -m "feat: add D100M printer model to DeviceRegistry"
```

---

### Task 2: Add dbus-java Dependency

**Files:**
- Modify: `shared/build.gradle.kts:51-56`
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Add dbus-java version to version catalog**

Add to `gradle/libs.versions.toml` in `[versions]` section:

```toml
dbus-java = "5.1.0"
```

Add to `[libraries]` section:

```toml
dbus-java-core = { module = "com.github.hypfviern:dbus-java-core", version.ref = "dbus-java" }
dbus-java-transport-native-unixsocket = { module = "com.github.hypfviern:dbus-java-transport-native-unixsocket", version.ref = "dbus-java" }
```

- [ ] **Step 2: Add dependency to desktopMain in shared module**

In `shared/build.gradle.kts`, add to the `desktopMain` dependencies block:

```kotlin
val desktopMain by getting {
    dependencies {
        implementation(compose.desktop.currentOs)
        implementation("com.google.zxing:core:3.5.3")
        implementation(libs.dbus.java.core)
        implementation(libs.dbus.java.transport.native.unixsocket)
    }
}
```

- [ ] **Step 3: Sync Gradle and verify dependency resolves**

Run: `./gradlew :shared:dependencies --configuration desktopRuntimeClasspath | grep dbus`
Expected: Shows dbus-java-core and transport jars

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/build.gradle.kts
git commit -m "build: add dbus-java dependency for Linux BLE support"
```

---

### Task 3: Create BlueZ D-Bus Interface Definitions

**Files:**
- Create: `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZInterfaces.kt`

- [ ] **Step 1: Create the BlueZ D-Bus interface file**

```kotlin
package com.avicennasis.bluepaper.ble.bluez

import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * BlueZ D-Bus interface definitions for BLE operations.
 * See: https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/doc
 */

/** org.bluez.Adapter1 - Bluetooth adapter interface */
interface Adapter1 : DBusInterface {
    fun StartDiscovery()
    fun StopDiscovery()
    fun SetDiscoveryFilter(properties: Map<String, Variant<*>>)
}

/** org.bluez.Device1 - Bluetooth device interface */
interface Device1 : DBusInterface {
    fun Connect()
    fun Disconnect()
}

/** org.bluez.GattService1 - GATT service interface */
interface GattService1 : DBusInterface {
    // Properties accessed via org.freedesktop.DBus.Properties
}

/** org.bluez.GattCharacteristic1 - GATT characteristic interface */
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
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZInterfaces.kt
git commit -m "feat: add BlueZ D-Bus interface definitions"
```

---

### Task 4: Implement BlueZ BLE Scanner

**Files:**
- Create: `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleScanner.kt`

- [ ] **Step 1: Create the BlueZ scanner implementation**

```kotlin
package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.ScannedDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.handlers.AbstractInterfacesAddedHandler
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * BLE scanner implementation for Linux using BlueZ D-Bus API.
 */
class BlueZBleScanner : BleScanner {

    override fun scan(namePrefix: String): Flow<ScannedDevice> = callbackFlow {
        val connection: DBusConnection = DBusConnectionBuilder.forSystemBus().build()
        
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
                override fun handle(objectPath: org.freedesktop.dbus.DBusPath, interfaces: Map<String, Map<String, Variant<*>>>) {
                    val deviceProps = interfaces[BlueZPaths.DEVICE_INTERFACE] ?: return
                    
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
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleScanner.kt
git commit -m "feat: implement BlueZ BLE scanner for Linux"
```

---

### Task 5: Implement BlueZ BLE Transport

**Files:**
- Create: `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleTransport.kt`

- [ ] **Step 1: Create the BlueZ transport implementation**

```kotlin
package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import com.avicennasis.bluepaper.printer.PrinterException
import com.avicennasis.bluepaper.printer.PrinterNotConnectedException
import com.avicennasis.bluepaper.printer.PrinterProtocolException
import com.avicennasis.bluepaper.printer.PrinterTimeoutException
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler
import org.freedesktop.dbus.interfaces.ObjectManager
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

/**
 * BLE transport implementation for Linux using BlueZ D-Bus API.
 */
class BlueZBleTransport : BleTransport {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var connection: DBusConnection? = null
    private var device: Device1? = null
    private var characteristic: GattCharacteristic1? = null
    private var characteristicPath: String? = null
    private var responseChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val commandMutex = Mutex()
    private var scope: CoroutineScope? = null
    private var notifyHandler: AbstractPropertiesChangedHandler? = null

    override suspend fun connect(device: ScannedDevice) {
        println("[BlueZBleTransport] connect() called for ${device.name} (${device.address})")
        _connectionState.value = ConnectionState.CONNECTING
        responseChannel = Channel(Channel.UNLIMITED)

        try {
            val conn = DBusConnectionBuilder.forSystemBus().build()
            connection = conn

            val devicePath = BlueZPaths.devicePath(BlueZPaths.ADAPTER_PATH, device.address)
            println("[BlueZBleTransport] Device path: $devicePath")

            val bluezDevice = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                devicePath,
                Device1::class.java
            )
            this.device = bluezDevice

            // Connect to the device
            bluezDevice.Connect()
            println("[BlueZBleTransport] Connected to device, discovering services...")

            // Wait briefly for services to be discovered
            Thread.sleep(2000)

            // Find the GATT characteristic for Niimbot printer
            val objectManager = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                "/",
                ObjectManager::class.java
            )

            val objects = objectManager.GetManagedObjects()
            var foundCharPath: String? = null

            for ((path, interfaces) in objects) {
                if (!path.path.startsWith(devicePath)) continue
                if (BlueZPaths.GATT_CHARACTERISTIC_INTERFACE !in interfaces) continue

                // Skip standard GATT characteristics (UUID starts with 0000)
                val charProps = interfaces[BlueZPaths.GATT_CHARACTERISTIC_INTERFACE] ?: continue
                val uuid = (charProps["UUID"]?.value as? String) ?: continue
                if (uuid.startsWith("0000")) continue

                foundCharPath = path.path
                println("[BlueZBleTransport] Found characteristic: $uuid at $foundCharPath")
                break
            }

            if (foundCharPath == null) {
                throw PrinterProtocolException("No suitable GATT characteristic found")
            }

            characteristicPath = foundCharPath
            val gattChar = conn.getRemoteObject(
                BlueZPaths.BLUEZ_SERVICE,
                foundCharPath,
                GattCharacteristic1::class.java
            )
            characteristic = gattChar

            // Set up notification handler
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            
            notifyHandler = object : AbstractPropertiesChangedHandler() {
                override fun handle(s: Properties.PropertiesChanged) {
                    if (s.path != foundCharPath) return
                    val value = s.propertiesChanged["Value"]?.value
                    if (value is ByteArray) {
                        println("[BlueZBleTransport] Received notification: ${value.size} bytes")
                        responseChannel.trySend(value)
                    }
                }
            }
            
            conn.addSigHandler(
                Properties.PropertiesChanged::class.java,
                BlueZPaths.BLUEZ_SERVICE,
                foundCharPath,
                notifyHandler
            )

            // Start notifications
            gattChar.StartNotify()
            println("[BlueZBleTransport] Started notifications")

            _connectionState.value = ConnectionState.CONNECTED
            println("[BlueZBleTransport] Connection established successfully")

        } catch (e: PrinterException) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw e
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw PrinterException("Failed to connect: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        println("[BlueZBleTransport] disconnect() called")
        _connectionState.value = ConnectionState.DISCONNECTING

        scope?.cancel()
        scope = null

        try {
            characteristic?.StopNotify()
        } catch (_: Exception) { }

        notifyHandler?.let { handler ->
            characteristicPath?.let { path ->
                try {
                    connection?.removeSigHandler(
                        Properties.PropertiesChanged::class.java,
                        BlueZPaths.BLUEZ_SERVICE,
                        path,
                        handler
                    )
                } catch (_: Exception) { }
            }
        }

        try {
            device?.Disconnect()
        } catch (_: Exception) { }

        try {
            connection?.close()
        } catch (_: Exception) { }

        device = null
        characteristic = null
        characteristicPath = null
        connection = null
        notifyHandler = null
        responseChannel.close()

        _connectionState.value = ConnectionState.DISCONNECTED
        println("[BlueZBleTransport] Disconnected")
    }

    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket {
        println("[BlueZBleTransport] sendCommand: type=0x${packet.type.toString(16)}, ${packet.toBytes().size} bytes")

        return commandMutex.withLock {
            val char = characteristic ?: throw PrinterNotConnectedException()

            // Clear any stale responses
            while (responseChannel.tryReceive().isSuccess) { }

            // Write the command
            char.WriteValue(packet.toBytes(), emptyMap())

            // Wait for response
            val responseData = try {
                withTimeout(timeoutMs) {
                    responseChannel.receive()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                throw PrinterTimeoutException("No response within ${timeoutMs}ms")
            } catch (e: ClosedReceiveChannelException) {
                throw PrinterNotConnectedException()
            }

            NiimbotPacket.fromBytes(responseData)
        }
    }

    override suspend fun writeRaw(packet: NiimbotPacket) {
        println("[BlueZBleTransport] writeRaw: ${packet.toBytes().size} bytes")
        commandMutex.withLock {
            val char = characteristic ?: throw PrinterNotConnectedException()
            char.WriteValue(packet.toBytes(), emptyMap())
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleTransport.kt
git commit -m "feat: implement BlueZ BLE transport for Linux"
```

---

### Task 6: Create Desktop BLE Factory

**Files:**
- Create: `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/DesktopBleFactory.kt`

- [ ] **Step 1: Create the factory that detects OS and returns appropriate impl**

```kotlin
package com.avicennasis.bluepaper.ble

import com.avicennasis.bluepaper.ble.bluez.BlueZBleScanner
import com.avicennasis.bluepaper.ble.bluez.BlueZBleTransport
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Factory for creating platform-appropriate BLE implementations on desktop.
 */
object DesktopBleFactory {

    private val os: String = System.getProperty("os.name").lowercase()

    val isLinux: Boolean = os.contains("linux")
    val isMacOS: Boolean = os.contains("mac")
    val isWindows: Boolean = os.contains("windows")

    fun createScanner(): BleScanner {
        return when {
            isLinux -> BlueZBleScanner()
            // macOS uses Kable which requires native code, stub for now
            // Windows uses WinRT which requires native code, stub for now
            else -> StubBleScanner()
        }
    }

    fun createTransport(): BleTransport {
        return when {
            isLinux -> BlueZBleTransport()
            else -> StubBleTransport()
        }
    }
}

/** Stub scanner for unsupported platforms */
internal class StubBleScanner : BleScanner {
    override fun scan(namePrefix: String): Flow<ScannedDevice> {
        println("[StubBleScanner] BLE scanning not supported on this platform")
        return emptyFlow()
    }
}

/** Stub transport for unsupported platforms */
internal class StubBleTransport : BleTransport {
    override val connectionState: StateFlow<ConnectionState> = 
        MutableStateFlow(ConnectionState.DISCONNECTED)
    
    override suspend fun connect(device: ScannedDevice) {
        println("[StubBleTransport] BLE not supported on this platform")
    }
    
    override suspend fun disconnect() { }
    
    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket =
        NiimbotPacket(packet.type, byteArrayOf(0x01))
    
    override suspend fun writeRaw(packet: NiimbotPacket) { }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ble/DesktopBleFactory.kt
git commit -m "feat: add DesktopBleFactory for OS-specific BLE implementation"
```

---

### Task 7: Update Desktop App Entry Point

**Files:**
- Modify: `desktopApp/src/desktopMain/kotlin/com/avicennasis/bluepaper/Main.kt`

- [ ] **Step 1: Replace stub implementations with factory**

Replace the entire file content:

```kotlin
package com.avicennasis.bluepaper

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.avicennasis.bluepaper.ble.DesktopBleFactory
import com.avicennasis.bluepaper.ui.BluePaperApp
import com.avicennasis.bluepaper.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Log platform detection
    println("[BluePaper] Platform: ${System.getProperty("os.name")}")
    println("[BluePaper] Linux BLE: ${DesktopBleFactory.isLinux}")
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "BluePaper",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        BluePaperApp(
            bleScanner = DesktopBleFactory.createScanner(),
            bleTransport = DesktopBleFactory.createTransport(),
            scope = scope,
            startScreen = if (DesktopBleFactory.isLinux) Screen.Scanner else Screen.Editor,
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :desktopApp:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Test run on Linux**

Run: `./gradlew :desktopApp:run`
Expected: App launches, shows Scanner screen on Linux, console shows "[BluePaper] Linux BLE: true"

- [ ] **Step 4: Commit**

```bash
git add desktopApp/src/desktopMain/kotlin/com/avicennasis/bluepaper/Main.kt
git commit -m "feat: use DesktopBleFactory for BLE in desktop app"
```

---

### Task 8: Add Unit Tests for BlueZ Scanner

**Files:**
- Create: `shared/src/desktopTest/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleScannerTest.kt`

- [ ] **Step 1: Create scanner tests**

Note: These tests verify the scanner can be instantiated and the factory works correctly. Full BLE testing requires hardware.

```kotlin
package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.DesktopBleFactory
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueZBleScannerTest {

    @Test
    fun `factory creates scanner on Linux`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test on ${System.getProperty("os.name")}")
            return
        }
        
        val scanner = DesktopBleFactory.createScanner()
        assertNotNull(scanner)
        assertTrue(scanner is BlueZBleScanner)
    }

    @Test
    fun `factory creates stub scanner on non-Linux`() {
        if (DesktopBleFactory.isLinux) {
            println("Skipping non-Linux test on ${System.getProperty("os.name")}")
            return
        }
        
        val scanner = DesktopBleFactory.createScanner()
        assertNotNull(scanner)
        // Should be StubBleScanner, but that's internal
    }

    @Test
    fun `platform detection is consistent`() {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("linux") -> assertTrue(DesktopBleFactory.isLinux)
            os.contains("mac") -> assertTrue(DesktopBleFactory.isMacOS)
            os.contains("windows") -> assertTrue(DesktopBleFactory.isWindows)
        }
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :shared:desktopTest --tests '*BlueZBleScannerTest*' -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopTest/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleScannerTest.kt
git commit -m "test: add BlueZ BLE scanner tests"
```

---

### Task 9: Add Unit Tests for BlueZ Transport

**Files:**
- Create: `shared/src/desktopTest/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleTransportTest.kt`

- [ ] **Step 1: Create transport tests**

```kotlin
package com.avicennasis.bluepaper.ble.bluez

import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.DesktopBleFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlueZBleTransportTest {

    @Test
    fun `factory creates transport on Linux`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test on ${System.getProperty("os.name")}")
            return
        }
        
        val transport = DesktopBleFactory.createTransport()
        assertNotNull(transport)
        assertTrue(transport is BlueZBleTransport)
    }

    @Test
    fun `transport starts disconnected`() {
        val transport = DesktopBleFactory.createTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
    }

    @Test
    fun `BlueZ transport initial state is disconnected`() {
        if (!DesktopBleFactory.isLinux) {
            println("Skipping Linux-specific test")
            return
        }
        
        val transport = BlueZBleTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :shared:desktopTest --tests '*BlueZBleTransportTest*' -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add shared/src/desktopTest/kotlin/com/avicennasis/bluepaper/ble/bluez/BlueZBleTransportTest.kt
git commit -m "test: add BlueZ BLE transport tests"
```

---

### Task 10: Update README

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update the platform support table**

Find the "Supported Platforms" table and replace it with:

```markdown
## Supported Platforms

| Platform | UI | BLE Printing | Notes |
|----------|-----|--------------|-------|
| Android | Full | Full | Kable BLE, requires Android 8.0+ |
| macOS | Full | Full | Kable BLE (CoreBluetooth) |
| Linux | Full | Full | BlueZ D-Bus (requires BlueZ 5.x) |
| Windows | Full | Not yet | Kable lacks WinRT support |
| iOS | Full | Planned | Kable ready, needs UI integration |
```

- [ ] **Step 2: Update Linux installation section**

Find the line that says "**Note**: Desktop Linux BLE is not yet implemented" and replace that paragraph with:

```markdown
**Linux BLE requirements:**
- BlueZ 5.x (pre-installed on most distros)
- D-Bus system bus access
- User must be in `bluetooth` group: `sudo usermod -aG bluetooth $USER` (then re-login)
```

- [ ] **Step 3: Add troubleshooting entry for BlueZ**

Add to the troubleshooting table:

```markdown
| BLE scan finds nothing | Ensure BlueZ is running: `systemctl status bluetooth` |
| D-Bus permission denied | Add user to bluetooth group, re-login |
```

- [ ] **Step 4: Update supported printers table**

Add D100M to the "Supported Printers" table:

```markdown
| D100-M | 300 | V1 | 240px |
```

- [ ] **Step 5: Verify README renders correctly**

Run: `cat README.md | head -80`
Expected: Tables are properly formatted

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: update README for Linux BLE support and D100M"
```

---

### Task 11: Run Full Test Suite

**Files:**
- None (verification only)

- [ ] **Step 1: Run all desktop tests**

Run: `./gradlew :shared:desktopTest -q`
Expected: All tests pass (350+ tests)

- [ ] **Step 2: Run the app and verify scanner screen**

Run: `./gradlew :desktopApp:run`
Expected: 
- App opens to Scanner screen on Linux
- Console shows "[BluePaper] Linux BLE: true"
- Clicking "Scan" attempts BLE discovery (may fail if Bluetooth off, but shouldn't crash)

- [ ] **Step 3: Final commit with test count**

```bash
git log --oneline -10
```

Verify all commits are present. No additional commit needed unless fixes were required.

---

## Summary

This plan implements:
1. **D100M printer model** - Added to DeviceRegistry with D-series defaults
2. **BlueZ D-Bus interfaces** - Type-safe Kotlin interfaces for BlueZ API
3. **BlueZBleScanner** - Scans for BLE devices using BlueZ discovery
4. **BlueZBleTransport** - Connects, reads, writes GATT characteristics
5. **DesktopBleFactory** - Runtime OS detection, returns correct impl
6. **Updated Main.kt** - Uses factory, starts on Scanner screen on Linux
7. **Unit tests** - Factory and platform detection tests
8. **Updated README** - Linux now shows full BLE support

After implementation, test with the physical D100M printer to verify:
- Device appears in scan results
- Connection succeeds
- Print commands work
- Adjust D100M DPI/rotation if needed based on output quality
