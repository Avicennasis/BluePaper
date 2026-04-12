# BluePaper Core Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the KMP project scaffold and implement the complete Niimbot binary protocol layer — packet encode/decode, command enums, response types, and device configuration models — as pure Kotlin in `commonMain` with full test coverage.

**Architecture:** All protocol code lives in `shared/src/commonMain/` with zero platform dependencies. The packet format is a custom binary protocol (header `0x55 0x55`, type, length, data, XOR checksum, footer `0xAA 0xAA`). Device configs define per-model DPI, density, rotation, label sizes, and V1/V2 protocol selection. This plan produces a fully tested protocol library that subsequent plans (BLE transport, image encoding, UI) will build on.

**Tech Stack:** Kotlin 2.1.10, Kotlin Multiplatform, Compose Multiplatform 1.7.3, Gradle 8.6, kotlin-test

**Subsequent plans (not covered here):**
- Plan 2: BLE Transport Layer (Kable on Android/iOS/macOS, platform-specific on Linux/Windows)
- Plan 3: Image Processing & Label Engine (monochrome conversion, bitmap encoding, offset arithmetic)
- Plan 4: Compose Multiplatform UI (label designer, device connection, print controls)

---

## File Structure

```
BluePaper/
├── build.gradle.kts                                    # Root build — plugins only
├── settings.gradle.kts                                 # Module includes + repos
├── gradle.properties                                   # KMP + Compose flags
├── gradle/
│   └── libs.versions.toml                              # Version catalog
├── shared/
│   ├── build.gradle.kts                                # KMP targets + dependencies
│   └── src/
│       ├── commonMain/kotlin/com/avicennasis/bluepaper/
│       │   ├── protocol/
│       │   │   ├── NiimbotPacket.kt                    # Packet encode/decode/checksum
│       │   │   ├── RequestCode.kt                      # Command codes sent to printer
│       │   │   ├── InfoEnum.kt                         # Sub-keys for GET_INFO command
│       │   │   └── ImageDataType.kt                    # Image row packet constant
│       │   ├── model/
│       │   │   ├── HeartbeatResponse.kt                # Heartbeat parse result
│       │   │   ├── PrintStatus.kt                      # Print progress parse result
│       │   │   ├── RFIDResponse.kt                     # RFID query parse result
│       │   │   └── PrinterInfo.kt                      # Aggregated device info
│       │   └── config/
│       │       ├── DeviceConfig.kt                     # Per-model printer config
│       │       ├── LabelSize.kt                        # Label dimensions (mm + px)
│       │       └── DeviceRegistry.kt                   # All 8 models + lookup
│       └── commonTest/kotlin/com/avicennasis/bluepaper/
│           ├── protocol/
│           │   └── NiimbotPacketTest.kt                # Packet round-trip, checksum, edge cases
│           ├── model/
│           │   ├── HeartbeatResponseTest.kt            # Heartbeat parsing per length variant
│           │   ├── PrintStatusTest.kt                  # Print status parsing
│           │   └── RFIDResponseTest.kt                 # RFID response parsing
│           └── config/
│               ├── DeviceConfigTest.kt                 # Model config validation
│               └── LabelSizeTest.kt                    # mm-to-pixel conversion
├── androidApp/
│   ├── build.gradle.kts                                # Android app module (minimal for now)
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/avicennasis/bluepaper/MainActivity.kt
├── desktopApp/
│   ├── build.gradle.kts                                # Desktop app module (minimal for now)
│   └── src/main/kotlin/com/avicennasis/bluepaper/Main.kt
└── .github/
    └── workflows/
        └── ci.yaml                                     # Test on push/PR
```

---

### Task 1: KMP Project Scaffolding

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `shared/build.gradle.kts`
- Create: `androidApp/build.gradle.kts`
- Create: `androidApp/src/main/AndroidManifest.xml`
- Create: `desktopApp/build.gradle.kts`

- [ ] **Step 1: Create version catalog**

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.1.10"
compose-multiplatform = "1.7.3"
agp = "8.7.3"
coroutines = "1.10.2"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

- [ ] **Step 2: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BluePaper"

include(":shared")
include(":androidApp")
include(":desktopApp")
```

- [ ] **Step 3: Create root build.gradle.kts**

```kotlin
// build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}
```

- [ ] **Step 4: Create gradle.properties**

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
kotlin.code.style=official
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create shared module build.gradle.kts**

```kotlin
// shared/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.avicennasis.bluepaper.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

- [ ] **Step 6: Create androidApp build.gradle.kts**

```kotlin
// androidApp/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.material3)
                implementation("androidx.activity:activity-compose:1.9.3")
            }
        }
    }
}

android {
    namespace = "com.avicennasis.bluepaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avicennasis.bluepaper"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

- [ ] **Step 7: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- androidApp/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />

    <application
        android:label="BluePaper"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Create placeholder MainActivity.kt**

```kotlin
// androidApp/src/main/kotlin/com/avicennasis/bluepaper/MainActivity.kt
package com.avicennasis.bluepaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("BluePaper")
        }
    }
}
```

- [ ] **Step 9: Create desktopApp build.gradle.kts**

```kotlin
// desktopApp/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.avicennasis.bluepaper.MainKt"

        nativeDistributions {
            packageName = "BluePaper"
            packageVersion = "0.1.0"
        }
    }
}
```

- [ ] **Step 10: Create placeholder desktop Main.kt**

```kotlin
// desktopApp/src/main/kotlin/com/avicennasis/bluepaper/Main.kt
package com.avicennasis.bluepaper

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "BluePaper") {
        Text("BluePaper")
    }
}
```

- [ ] **Step 11: Install Gradle wrapper and verify project compiles**

Run: `cd ~/github/BluePaper && gradle wrapper --gradle-version 8.6`
Then: `./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL

- [ ] **Step 12: Commit scaffold**

```bash
git add -A
git commit -m "feat: KMP project scaffold with shared/android/desktop modules"
```

---

### Task 2: NiimbotPacket — Encode/Decode/Checksum

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacket.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacketTest.kt`

- [ ] **Step 1: Write failing tests for packet construction and serialization**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacketTest.kt
package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class NiimbotPacketTest {

    @Test
    fun constructPacketWithTypeAndData() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        assertEquals(0x40, packet.type)
        assertContentEquals(byteArrayOf(0x01), packet.data)
    }

    @Test
    fun constructPacketWithEmptyData() {
        val packet = NiimbotPacket(type = 0xDC, data = byteArrayOf())
        assertEquals(0xDC.toByte().toInt() and 0xFF, packet.type)
        assertContentEquals(byteArrayOf(), packet.data)
    }

    @Test
    fun checksumIsXorOfTypeLengthAndData() {
        // type=0x40, len=1, data=[0x01]
        // checksum = 0x40 xor 0x01 xor 0x01 = 0x40
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        assertEquals(0x40, packet.checksum)
    }

    @Test
    fun checksumWithEmptyData() {
        // type=0xDC, len=0
        // checksum = 0xDC xor 0x00 = 0xDC
        val packet = NiimbotPacket(type = 0xDC, data = byteArrayOf())
        assertEquals(0xDC, packet.checksum)
    }

    @Test
    fun checksumWithMultipleDataBytes() {
        // type=0x13, len=4, data=[0x00, 0x64, 0x00, 0xF0]
        // checksum = 0x13 ^ 0x04 ^ 0x00 ^ 0x64 ^ 0x00 ^ 0xF0 = 0x83
        val data = byteArrayOf(0x00, 0x64, 0x00, 0xF0.toByte())
        val packet = NiimbotPacket(type = 0x13, data = data)
        val expected = 0x13 xor 0x04 xor 0x00 xor 0x64 xor 0x00 xor 0xF0
        assertEquals(expected and 0xFF, packet.checksum)
    }

    @Test
    fun toBytesProducesCorrectWireFormat() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        val bytes = packet.toBytes()
        // header(55 55) + type(40) + len(01) + data(01) + checksum(40) + footer(AA AA)
        val expected = byteArrayOf(
            0x55, 0x55,
            0x40,
            0x01,
            0x01,
            0x40,
            0xAA.toByte(), 0xAA.toByte()
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun toBytesEmptyDataPacket() {
        val packet = NiimbotPacket(type = 0x01, data = byteArrayOf())
        val bytes = packet.toBytes()
        // header(55 55) + type(01) + len(00) + checksum(01) + footer(AA AA)
        val expected = byteArrayOf(
            0x55, 0x55,
            0x01,
            0x00,
            0x01,
            0xAA.toByte(), 0xAA.toByte()
        )
        assertContentEquals(expected, bytes)
    }

    @Test
    fun minimumPacketSizeIs7Bytes() {
        val packet = NiimbotPacket(type = 0x01, data = byteArrayOf())
        assertEquals(7, packet.toBytes().size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: FAIL — `NiimbotPacket` does not exist

- [ ] **Step 3: Implement NiimbotPacket**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacket.kt
package com.avicennasis.bluepaper.protocol

class NiimbotPacket(
    val type: Int,
    val data: ByteArray,
) {
    init {
        require(type in 0..255) { "Type must be 0-255, got $type" }
        require(data.size <= 255) { "Data must be 0-255 bytes, got ${data.size}" }
    }

    val checksum: Int
        get() {
            var cs = type xor data.size
            for (b in data) {
                cs = cs xor (b.toInt() and 0xFF)
            }
            return cs and 0xFF
        }

    fun toBytes(): ByteArray {
        val result = ByteArray(data.size + 6)
        result[0] = HEADER
        result[1] = HEADER
        result[2] = type.toByte()
        result[3] = data.size.toByte()
        data.copyInto(result, 4)
        result[4 + data.size] = checksum.toByte()
        result[5 + data.size] = FOOTER
        result[6 + data.size] = FOOTER
        return result
    }

    /** Interpret data as a big-endian unsigned integer. */
    fun dataToInt(): Long {
        var value = 0L
        for (b in data) {
            value = (value shl 8) or (b.toLong() and 0xFF)
        }
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NiimbotPacket) return false
        return type == other.type && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * type + data.contentHashCode()

    override fun toString(): String =
        "NiimbotPacket(type=0x${type.toString(16).padStart(2, '0')}, data=[${data.size} bytes])"

    companion object {
        const val HEADER: Byte = 0x55
        const val FOOTER: Byte = 0xAA.toByte()

        fun fromBytes(raw: ByteArray): NiimbotPacket {
            require(raw.size >= 7) { "Packet too short: ${raw.size} bytes (minimum 7)" }
            require(raw[0] == HEADER && raw[1] == HEADER) {
                "Invalid header: expected 0x5555, got 0x${(raw[0].toInt() and 0xFF).toString(16)}${(raw[1].toInt() and 0xFF).toString(16)}"
            }

            val type = raw[2].toInt() and 0xFF
            val len = raw[3].toInt() and 0xFF
            val expectedEnd = 4 + len + 3 // data + checksum + 2 footer bytes

            require(raw.size >= expectedEnd) {
                "Packet truncated: need $expectedEnd bytes, have ${raw.size}"
            }
            require(raw[expectedEnd - 1] == FOOTER && raw[expectedEnd - 2] == FOOTER) {
                "Invalid footer"
            }

            val data = raw.copyOfRange(4, 4 + len)

            // Verify checksum
            var cs = type xor len
            for (b in data) {
                cs = cs xor (b.toInt() and 0xFF)
            }
            val actualChecksum = raw[expectedEnd - 3].toInt() and 0xFF
            require((cs and 0xFF) == actualChecksum) {
                "Checksum mismatch: expected 0x${(cs and 0xFF).toString(16)}, got 0x${actualChecksum.toString(16)}"
            }

            return NiimbotPacket(type, data)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS — all 9 tests green

- [ ] **Step 5: Write failing tests for fromBytes deserialization**

Add to `NiimbotPacketTest.kt`:

```kotlin
    @Test
    fun fromBytesRoundTrip() {
        val original = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01))
        val decoded = NiimbotPacket.fromBytes(original.toBytes())
        assertEquals(original.type, decoded.type)
        assertContentEquals(original.data, decoded.data)
    }

    @Test
    fun fromBytesEmptyDataRoundTrip() {
        val original = NiimbotPacket(type = 0xF3, data = byteArrayOf())
        val decoded = NiimbotPacket.fromBytes(original.toBytes())
        assertEquals(original.type, decoded.type)
        assertContentEquals(original.data, decoded.data)
    }

    @Test
    fun fromBytesWithTrailingBytesAccepted() {
        val wire = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01)).toBytes()
        val padded = wire + byteArrayOf(0xFF.toByte(), 0x00)
        val decoded = NiimbotPacket.fromBytes(padded)
        assertEquals(0x40, decoded.type)
        assertContentEquals(byteArrayOf(0x01), decoded.data)
    }

    @Test
    fun fromBytesRejectsTooShort() {
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(byteArrayOf(0x55, 0x55, 0x01))
        }
    }

    @Test
    fun fromBytesRejectsBadHeader() {
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(byteArrayOf(0xAA.toByte(), 0x55, 0x01, 0x00, 0x01, 0xAA.toByte(), 0xAA.toByte()))
        }
    }

    @Test
    fun fromBytesRejectsBadChecksum() {
        val wire = byteArrayOf(
            0x55, 0x55,
            0x40,
            0x01,
            0x01,
            0x00, // wrong checksum — should be 0x40
            0xAA.toByte(), 0xAA.toByte()
        )
        assertFailsWith<IllegalArgumentException> {
            NiimbotPacket.fromBytes(wire)
        }
    }

    @Test
    fun dataToIntSingleByte() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x03))
        assertEquals(3L, packet.dataToInt())
    }

    @Test
    fun dataToIntTwoBytes() {
        // 0x01 0x00 = 256
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf(0x01, 0x00))
        assertEquals(256L, packet.dataToInt())
    }

    @Test
    fun dataToIntEmpty() {
        val packet = NiimbotPacket(type = 0x40, data = byteArrayOf())
        assertEquals(0L, packet.dataToInt())
    }
```

- [ ] **Step 6: Run tests — they should already pass**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS — all 18 tests green (fromBytes and dataToInt were implemented in Step 3)

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacket.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/NiimbotPacketTest.kt
git commit -m "feat: NiimbotPacket encode/decode with XOR checksum and full test coverage"
```

---

### Task 3: Protocol Enums — RequestCode, InfoEnum, ImageDataType

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/RequestCode.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/InfoEnum.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/ImageDataType.kt`

- [ ] **Step 1: Create RequestCode enum**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/RequestCode.kt
package com.avicennasis.bluepaper.protocol

/** Command codes sent to the printer. The printer echoes the same code in its response. */
enum class RequestCode(val code: Int) {
    GET_INFO(0x40),
    GET_RFID(0x1A),
    HEARTBEAT(0xDC),
    SET_LABEL_TYPE(0x23),
    SET_LABEL_DENSITY(0x21),
    START_PRINT(0x01),
    END_PRINT(0xF3),
    START_PAGE_PRINT(0x03),
    END_PAGE_PRINT(0xE3),
    SET_DIMENSION(0x13),
    SET_QUANTITY(0x15),
    GET_PRINT_STATUS(0xA3),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun fromCode(code: Int): RequestCode? = byCode[code]
    }
}
```

- [ ] **Step 2: Create InfoEnum**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/InfoEnum.kt
package com.avicennasis.bluepaper.protocol

/** Sub-keys for the GET_INFO command. Sent as the single data byte in a GET_INFO packet. */
enum class InfoEnum(val key: Int) {
    DENSITY(1),
    PRINT_SPEED(2),
    LABEL_TYPE(3),
    LANGUAGE_TYPE(6),
    AUTO_SHUTDOWN_TIME(7),
    DEVICE_TYPE(8),
    SOFTWARE_VERSION(9),
    BATTERY(10),
    DEVICE_SERIAL(11),
    HARDWARE_VERSION(12),
    ;

    companion object {
        private val byKey = entries.associateBy { it.key }

        fun fromKey(key: Int): InfoEnum? = byKey[key]
    }
}
```

- [ ] **Step 3: Create ImageDataType constant**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/ImageDataType.kt
package com.avicennasis.bluepaper.protocol

/**
 * Image row packets use type 0x85.
 * These are fire-and-forget (no notification response expected).
 */
const val IMAGE_DATA_TYPE: Int = 0x85
```

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/RequestCode.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/InfoEnum.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/ImageDataType.kt
git commit -m "feat: protocol enums — RequestCode, InfoEnum, IMAGE_DATA_TYPE"
```

---

### Task 4: Response Types — HeartbeatResponse, PrintStatus, RFIDResponse

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/HeartbeatResponse.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/PrintStatus.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/RFIDResponse.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/PrinterInfo.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/HeartbeatResponseTest.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/PrintStatusTest.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/RFIDResponseTest.kt`

- [ ] **Step 1: Write failing tests for HeartbeatResponse parsing**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/HeartbeatResponseTest.kt
package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeartbeatResponseTest {

    @Test
    fun parse9ByteResponse() {
        val data = ByteArray(9).also { it[8] = 1 }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.closingState)
        assertNull(hb.powerLevel)
        assertNull(hb.paperState)
        assertNull(hb.rfidReadState)
    }

    @Test
    fun parse10ByteResponse() {
        val data = ByteArray(10).also {
            it[8] = 0
            it[9] = 85.toByte()
        }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(0, hb.closingState)
        assertEquals(85, hb.powerLevel)
        assertNull(hb.paperState)
    }

    @Test
    fun parse13ByteResponse() {
        val data = ByteArray(13).also {
            it[9] = 1
            it[10] = 75.toByte()
            it[11] = 2
            it[12] = 3
        }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.closingState)
        assertEquals(75, hb.powerLevel)
        assertEquals(2, hb.paperState)
        assertEquals(3, hb.rfidReadState)
    }

    @Test
    fun parse19ByteResponse() {
        val data = ByteArray(19).also {
            it[15] = 0
            it[16] = 100.toByte()
            it[17] = 1
            it[18] = 0
        }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(0, hb.closingState)
        assertEquals(100, hb.powerLevel)
        assertEquals(1, hb.paperState)
        assertEquals(0, hb.rfidReadState)
    }

    @Test
    fun parse20ByteResponse() {
        val data = ByteArray(20).also {
            it[18] = 1
            it[19] = 1
        }
        val hb = HeartbeatResponse.fromData(data)
        assertEquals(1, hb.paperState)
        assertEquals(1, hb.rfidReadState)
    }

    @Test
    fun unknownLengthReturnsAllNull() {
        val data = ByteArray(5)
        val hb = HeartbeatResponse.fromData(data)
        assertNull(hb.closingState)
        assertNull(hb.powerLevel)
        assertNull(hb.paperState)
        assertNull(hb.rfidReadState)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: FAIL — `HeartbeatResponse` does not exist

- [ ] **Step 3: Implement HeartbeatResponse**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/HeartbeatResponse.kt
package com.avicennasis.bluepaper.model

data class HeartbeatResponse(
    val closingState: Int?,
    val powerLevel: Int?,
    val paperState: Int?,
    val rfidReadState: Int?,
) {
    companion object {
        fun fromData(data: ByteArray): HeartbeatResponse {
            fun b(i: Int): Int = data[i].toInt() and 0xFF

            return when (data.size) {
                9 -> HeartbeatResponse(
                    closingState = b(8),
                    powerLevel = null,
                    paperState = null,
                    rfidReadState = null,
                )
                10 -> HeartbeatResponse(
                    closingState = b(8),
                    powerLevel = b(9),
                    paperState = null,
                    rfidReadState = null,
                )
                13 -> HeartbeatResponse(
                    closingState = b(9),
                    powerLevel = b(10),
                    paperState = b(11),
                    rfidReadState = b(12),
                )
                19 -> HeartbeatResponse(
                    closingState = b(15),
                    powerLevel = b(16),
                    paperState = b(17),
                    rfidReadState = b(18),
                )
                20 -> HeartbeatResponse(
                    closingState = null,
                    powerLevel = null,
                    paperState = b(18),
                    rfidReadState = b(19),
                )
                else -> HeartbeatResponse(null, null, null, null)
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS

- [ ] **Step 5: Write failing tests for PrintStatus**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/PrintStatusTest.kt
package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrintStatusTest {

    @Test
    fun parseNormalStatus() {
        // page=1 (0x00 0x01), progress1=50 (0x32), progress2=75 (0x4B)
        val data = byteArrayOf(0x00, 0x01, 0x32, 0x4B)
        val status = PrintStatus.fromData(data)
        assertEquals(1, status.page)
        assertEquals(50, status.progress1)
        assertEquals(75, status.progress2)
    }

    @Test
    fun parseHighPageCount() {
        // page=256 (0x01 0x00)
        val data = byteArrayOf(0x01, 0x00, 0x64, 0x64)
        val status = PrintStatus.fromData(data)
        assertEquals(256, status.page)
        assertEquals(100, status.progress1)
        assertEquals(100, status.progress2)
    }

    @Test
    fun rejectsTooShortData() {
        assertFailsWith<IllegalArgumentException> {
            PrintStatus.fromData(byteArrayOf(0x00, 0x01, 0x32))
        }
    }
}
```

- [ ] **Step 6: Implement PrintStatus**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/PrintStatus.kt
package com.avicennasis.bluepaper.model

data class PrintStatus(
    val page: Int,
    val progress1: Int,
    val progress2: Int,
) {
    companion object {
        fun fromData(data: ByteArray): PrintStatus {
            require(data.size >= 4) { "PrintStatus needs at least 4 bytes, got ${data.size}" }
            val page = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            val progress1 = data[2].toInt() and 0xFF
            val progress2 = data[3].toInt() and 0xFF
            return PrintStatus(page, progress1, progress2)
        }
    }
}
```

- [ ] **Step 7: Run tests to verify PrintStatus passes**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS

- [ ] **Step 8: Write failing tests for RFIDResponse**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/RFIDResponseTest.kt
package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RFIDResponseTest {

    @Test
    fun parseFullRfidResponse() {
        // uuid: 8 bytes (0x01..0x08)
        // barcode_len: 3
        // barcode: "ABC"
        // serial_len: 2
        // serial: "XY"
        // total_len: 100 (0x00 0x64)
        // used_len: 10 (0x00 0x0A)
        // type: 1
        val data = byteArrayOf(
            0x01,  // data[0] != 0, so not empty
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, // uuid (8 bytes)
            0x03,                                              // barcode_len
            0x41, 0x42, 0x43,                                  // "ABC"
            0x02,                                              // serial_len
            0x58, 0x59,                                        // "XY"
            0x00, 0x64,                                        // total_len = 100
            0x00, 0x0A,                                        // used_len = 10
            0x01,                                              // type = 1
        )
        val rfid = RFIDResponse.fromData(data)!!
        assertEquals("0102030405060708", rfid.uuid)
        assertEquals("ABC", rfid.barcode)
        assertEquals("XY", rfid.serial)
        assertEquals(100, rfid.totalLen)
        assertEquals(10, rfid.usedLen)
        assertEquals(1, rfid.type)
    }

    @Test
    fun emptyDataReturnsNull() {
        val data = byteArrayOf(0x00)
        assertNull(RFIDResponse.fromData(data))
    }

    @Test
    fun zeroLengthDataReturnsNull() {
        assertNull(RFIDResponse.fromData(byteArrayOf()))
    }
}
```

- [ ] **Step 9: Implement RFIDResponse**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/RFIDResponse.kt
package com.avicennasis.bluepaper.model

data class RFIDResponse(
    val uuid: String,
    val barcode: String,
    val serial: String,
    val totalLen: Int,
    val usedLen: Int,
    val type: Int,
) {
    companion object {
        fun fromData(data: ByteArray): RFIDResponse? {
            if (data.isEmpty() || (data[0].toInt() and 0xFF) == 0) return null

            // uuid: bytes [0..8) — 8 bytes, hex-encoded
            val uuid = data.sliceArray(0 until 8)
                .joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

            // barcode
            val barcodeLen = data[8].toInt() and 0xFF
            val barcode = data.decodeToString(9, 9 + barcodeLen)

            // serial
            val serialOffset = 9 + barcodeLen
            val serialLen = data[serialOffset].toInt() and 0xFF
            val serial = data.decodeToString(serialOffset + 1, serialOffset + 1 + serialLen)

            // trailer: total_len (uint16 BE), used_len (uint16 BE), type (uint8)
            val trailerOffset = serialOffset + 1 + serialLen
            val totalLen = ((data[trailerOffset].toInt() and 0xFF) shl 8) or
                (data[trailerOffset + 1].toInt() and 0xFF)
            val usedLen = ((data[trailerOffset + 2].toInt() and 0xFF) shl 8) or
                (data[trailerOffset + 3].toInt() and 0xFF)
            val type = data[trailerOffset + 4].toInt() and 0xFF

            return RFIDResponse(uuid, barcode, serial, totalLen, usedLen, type)
        }
    }
}
```

- [ ] **Step 10: Create PrinterInfo data class**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/PrinterInfo.kt
package com.avicennasis.bluepaper.model

data class PrinterInfo(
    val deviceSerial: String? = null,
    val softwareVersion: Double? = null,
    val hardwareVersion: Double? = null,
    val deviceType: Int? = null,
    val battery: Int? = null,
    val density: Int? = null,
    val printSpeed: Int? = null,
    val labelType: Int? = null,
)
```

- [ ] **Step 11: Run all tests**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS — all tests green

- [ ] **Step 12: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/model/ \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/model/
git commit -m "feat: response types — HeartbeatResponse, PrintStatus, RFIDResponse, PrinterInfo"
```

---

### Task 5: Device Configuration — Models, Label Sizes, Protocol Version

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/LabelSize.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceConfig.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceRegistry.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/config/LabelSizeTest.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/config/DeviceConfigTest.kt`

- [ ] **Step 1: Write failing tests for LabelSize mm-to-pixel conversion**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/config/LabelSizeTest.kt
package com.avicennasis.bluepaper.config

import kotlin.test.Test
import kotlin.test.assertEquals

class LabelSizeTest {

    @Test
    fun mmToPixelsAt203Dpi() {
        // 30mm at 203 DPI = (30 / 25.4) * 203 = 239.76 -> 239
        val size = LabelSize(widthMm = 30.0, heightMm = 15.0, dpi = 203)
        assertEquals(239, size.widthPx)
        assertEquals(119, size.heightPx)
    }

    @Test
    fun mmToPixelsAt300Dpi() {
        // 30mm at 300 DPI = (30 / 25.4) * 300 = 354.33 -> 354
        val size = LabelSize(widthMm = 30.0, heightMm = 14.0, dpi = 300)
        assertEquals(354, size.widthPx)
        assertEquals(165, size.heightPx)
    }

    @Test
    fun displayName() {
        val size = LabelSize(widthMm = 50.0, heightMm = 30.0, dpi = 203)
        assertEquals("50x30mm", size.displayName)
    }

    @Test
    fun fractionalMm() {
        val size = LabelSize(widthMm = 109.0, heightMm = 12.5, dpi = 203)
        assertEquals(870, size.widthPx)
        assertEquals(99, size.heightPx)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: FAIL — `LabelSize` does not exist

- [ ] **Step 3: Implement LabelSize**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/LabelSize.kt
package com.avicennasis.bluepaper.config

data class LabelSize(
    val widthMm: Double,
    val heightMm: Double,
    val dpi: Int,
) {
    val widthPx: Int get() = ((widthMm / 25.4) * dpi).toInt()
    val heightPx: Int get() = ((heightMm / 25.4) * dpi).toInt()

    val displayName: String
        get() {
            val w = if (widthMm == widthMm.toLong().toDouble()) widthMm.toLong().toString() else widthMm.toString()
            val h = if (heightMm == heightMm.toLong().toDouble()) heightMm.toLong().toString() else heightMm.toString()
            return "${w}x${h}mm"
        }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS

- [ ] **Step 5: Write failing tests for DeviceConfig and DeviceRegistry**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/config/DeviceConfigTest.kt
package com.avicennasis.bluepaper.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceConfigTest {

    @Test
    fun allEightModelsRegistered() {
        val models = listOf("d110", "d11", "d11_h", "d101", "d110_m", "b18", "b21", "b1")
        for (model in models) {
            assertNotNull(DeviceRegistry.get(model), "Missing model: $model")
        }
    }

    @Test
    fun unknownModelReturnsNull() {
        assertNull(DeviceRegistry.get("z99"))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        assertNotNull(DeviceRegistry.get("D110"))
        assertNotNull(DeviceRegistry.get("B21"))
    }

    @Test
    fun d110Config() {
        val cfg = DeviceRegistry.get("d110")!!
        assertEquals(203, cfg.dpi)
        assertEquals(3, cfg.maxDensity)
        assertEquals(-90, cfg.rotation)
        assertFalse(cfg.isV2)
        assertEquals(240, cfg.maxWidthPx)
        assertTrue(cfg.labelSizes.isNotEmpty())
    }

    @Test
    fun b21Config() {
        val cfg = DeviceRegistry.get("b21")!!
        assertEquals(203, cfg.dpi)
        assertEquals(5, cfg.maxDensity)
        assertEquals(0, cfg.rotation)
        assertTrue(cfg.isV2)
        assertEquals(384, cfg.maxWidthPx)
    }

    @Test
    fun d11hIsHighDpi() {
        val cfg = DeviceRegistry.get("d11_h")!!
        assertEquals(300, cfg.dpi)
    }

    @Test
    fun d110mIsHighDpi() {
        val cfg = DeviceRegistry.get("d110_m")!!
        assertEquals(300, cfg.dpi)
    }

    @Test
    fun v2ModelsAreOnlyBSeries() {
        val v2 = DeviceRegistry.all().filter { it.isV2 }.map { it.model }
        assertEquals(setOf("b1", "b18", "b21"), v2.toSet())
    }

    @Test
    fun dSeriesUseNeg90Rotation() {
        val dModels = listOf("d110", "d11", "d11_h", "d101", "d110_m")
        for (model in dModels) {
            assertEquals(-90, DeviceRegistry.get(model)!!.rotation, "Model $model rotation")
        }
    }

    @Test
    fun bSeriesUse0Rotation() {
        val bModels = listOf("b1", "b18", "b21")
        for (model in bModels) {
            assertEquals(0, DeviceRegistry.get(model)!!.rotation, "Model $model rotation")
        }
    }
}
```

- [ ] **Step 6: Implement DeviceConfig and DeviceRegistry**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceConfig.kt
package com.avicennasis.bluepaper.config

data class DeviceConfig(
    val model: String,
    val dpi: Int,
    val maxDensity: Int,
    val rotation: Int,
    val isV2: Boolean,
    val maxWidthPx: Int,
    val labelSizes: List<LabelSize>,
)
```

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/DeviceRegistry.kt
package com.avicennasis.bluepaper.config

object DeviceRegistry {

    private val V1_MAX_WIDTH = 240
    private val V2_MAX_WIDTH = 384
    private val DEFAULT_MAX_DENSITY = 3

    private fun sizes(dpi: Int, vararg dims: Pair<Double, Double>): List<LabelSize> =
        dims.map { (w, h) -> LabelSize(w, h, dpi) }

    private val configs: Map<String, DeviceConfig> = listOf(
        DeviceConfig(
            model = "d110", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d11", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d11_h", dpi = 300, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(300, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d101", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d110_m", dpi = 300, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(300, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "b18", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 40.0 to 14.0, 50.0 to 14.0, 120.0 to 14.0),
        ),
        DeviceConfig(
            model = "b21", dpi = 203, maxDensity = 5,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 50.0 to 30.0, 40.0 to 30.0, 50.0 to 15.0, 30.0 to 15.0),
        ),
        DeviceConfig(
            model = "b1", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 50.0 to 30.0, 50.0 to 15.0, 60.0 to 40.0, 40.0 to 30.0),
        ),
    ).associateBy { it.model }

    fun get(model: String): DeviceConfig? = configs[model.lowercase()]

    fun all(): List<DeviceConfig> = configs.values.toList()

    fun models(): List<String> = configs.keys.toList()
}
```

- [ ] **Step 7: Run all tests**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS — all tests green

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/config/ \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/config/
git commit -m "feat: device configuration — 8 printer models, label sizes, V1/V2 protocol flags"
```

---

### Task 6: Command Builder — Packet Construction Helpers

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilder.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilderTest.kt`

This encapsulates the exact byte payloads for each printer command, separate from the BLE transport layer.

- [ ] **Step 1: Write failing tests for command construction**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilderTest.kt
package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class CommandBuilderTest {

    @Test
    fun getInfoPacket() {
        val pkt = CommandBuilder.getInfo(InfoEnum.BATTERY)
        assertEquals(RequestCode.GET_INFO.code, pkt.type)
        assertContentEquals(byteArrayOf(10), pkt.data)
    }

    @Test
    fun getRfidPacket() {
        val pkt = CommandBuilder.getRfid()
        assertEquals(RequestCode.GET_RFID.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun heartbeatPacket() {
        val pkt = CommandBuilder.heartbeat()
        assertEquals(RequestCode.HEARTBEAT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setLabelTypePacket() {
        val pkt = CommandBuilder.setLabelType(1)
        assertEquals(RequestCode.SET_LABEL_TYPE.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setLabelTypeRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelType(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelType(4) }
    }

    @Test
    fun setLabelDensityPacket() {
        val pkt = CommandBuilder.setLabelDensity(3)
        assertEquals(RequestCode.SET_LABEL_DENSITY.code, pkt.type)
        assertContentEquals(byteArrayOf(0x03), pkt.data)
    }

    @Test
    fun setLabelDensityRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelDensity(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelDensity(6) }
    }

    @Test
    fun startPrintV1Packet() {
        val pkt = CommandBuilder.startPrint()
        assertEquals(RequestCode.START_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun startPrintV2Packet() {
        val pkt = CommandBuilder.startPrintV2(quantity = 3)
        assertEquals(RequestCode.START_PRINT.code, pkt.type)
        // [0x00][qty_hi=0x00][qty_lo=0x03][0x00][0x00][0x00][0x00]
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00), pkt.data)
    }

    @Test
    fun startPrintV2HighQuantity() {
        val pkt = CommandBuilder.startPrintV2(quantity = 500)
        // 500 = 0x01F4
        assertContentEquals(byteArrayOf(0x00, 0x01, 0xF4.toByte(), 0x00, 0x00, 0x00, 0x00), pkt.data)
    }

    @Test
    fun startPrintV2RejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.startPrintV2(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.startPrintV2(65536) }
    }

    @Test
    fun endPrintPacket() {
        val pkt = CommandBuilder.endPrint()
        assertEquals(RequestCode.END_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun startPagePrintPacket() {
        val pkt = CommandBuilder.startPagePrint()
        assertEquals(RequestCode.START_PAGE_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun endPagePrintPacket() {
        val pkt = CommandBuilder.endPagePrint()
        assertEquals(RequestCode.END_PAGE_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setDimensionV1Packet() {
        val pkt = CommandBuilder.setDimension(height = 100, width = 240)
        assertEquals(RequestCode.SET_DIMENSION.code, pkt.type)
        // pack(">HH", 100, 240) = [0x00 0x64 0x00 0xF0]
        assertContentEquals(byteArrayOf(0x00, 0x64, 0x00, 0xF0.toByte()), pkt.data)
    }

    @Test
    fun setDimensionV2Packet() {
        val pkt = CommandBuilder.setDimensionV2(height = 100, width = 384, copies = 2)
        assertEquals(RequestCode.SET_DIMENSION.code, pkt.type)
        // pack(">HHH", 100, 384, 2) = [0x00 0x64 0x01 0x80 0x00 0x02]
        assertContentEquals(
            byteArrayOf(0x00, 0x64, 0x01, 0x80.toByte(), 0x00, 0x02),
            pkt.data,
        )
    }

    @Test
    fun setQuantityPacket() {
        val pkt = CommandBuilder.setQuantity(5)
        assertEquals(RequestCode.SET_QUANTITY.code, pkt.type)
        // pack(">H", 5) = [0x00 0x05]
        assertContentEquals(byteArrayOf(0x00, 0x05), pkt.data)
    }

    @Test
    fun setQuantityRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setQuantity(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setQuantity(65536) }
    }

    @Test
    fun getPrintStatusPacket() {
        val pkt = CommandBuilder.getPrintStatus()
        assertEquals(RequestCode.GET_PRINT_STATUS.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun imageRowPacket() {
        val lineData = byteArrayOf(0xFF.toByte(), 0x00)
        val pkt = CommandBuilder.imageRow(y = 42, lineData = lineData)
        assertEquals(IMAGE_DATA_TYPE, pkt.type)
        // header: pack(">HBBBB", 42, 0, 0, 0, 1) = [0x00 0x2A 0x00 0x00 0x00 0x01]
        // + lineData [0xFF 0x00]
        val expectedData = byteArrayOf(0x00, 0x2A, 0x00, 0x00, 0x00, 0x01, 0xFF.toByte(), 0x00)
        assertContentEquals(expectedData, pkt.data)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: FAIL — `CommandBuilder` does not exist

- [ ] **Step 3: Implement CommandBuilder**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilder.kt
package com.avicennasis.bluepaper.protocol

/**
 * Constructs [NiimbotPacket]s for each printer command.
 * All multi-byte integers are big-endian (network byte order).
 */
object CommandBuilder {

    fun getInfo(key: InfoEnum): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(key.key.toByte()))

    fun getRfid(): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_RFID.code, byteArrayOf(0x01))

    fun heartbeat(): NiimbotPacket =
        NiimbotPacket(RequestCode.HEARTBEAT.code, byteArrayOf(0x01))

    fun setLabelType(n: Int): NiimbotPacket {
        require(n in 1..3) { "Label type must be 1-3, got $n" }
        return NiimbotPacket(RequestCode.SET_LABEL_TYPE.code, byteArrayOf(n.toByte()))
    }

    fun setLabelDensity(n: Int): NiimbotPacket {
        require(n in 1..5) { "Label density must be 1-5, got $n" }
        return NiimbotPacket(RequestCode.SET_LABEL_DENSITY.code, byteArrayOf(n.toByte()))
    }

    fun startPrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.START_PRINT.code, byteArrayOf(0x01))

    fun startPrintV2(quantity: Int): NiimbotPacket {
        require(quantity in 1..65535) { "Quantity must be 1-65535, got $quantity" }
        val data = ByteArray(7)
        data[0] = 0x00
        data[1] = (quantity shr 8).toByte()
        data[2] = (quantity and 0xFF).toByte()
        // bytes 3-6 are zero (already initialized)
        return NiimbotPacket(RequestCode.START_PRINT.code, data)
    }

    fun endPrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.END_PRINT.code, byteArrayOf(0x01))

    fun startPagePrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.START_PAGE_PRINT.code, byteArrayOf(0x01))

    fun endPagePrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.END_PAGE_PRINT.code, byteArrayOf(0x01))

    fun setDimension(height: Int, width: Int): NiimbotPacket {
        val data = ByteArray(4)
        data[0] = (height shr 8).toByte()
        data[1] = (height and 0xFF).toByte()
        data[2] = (width shr 8).toByte()
        data[3] = (width and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_DIMENSION.code, data)
    }

    fun setDimensionV2(height: Int, width: Int, copies: Int): NiimbotPacket {
        val data = ByteArray(6)
        data[0] = (height shr 8).toByte()
        data[1] = (height and 0xFF).toByte()
        data[2] = (width shr 8).toByte()
        data[3] = (width and 0xFF).toByte()
        data[4] = (copies shr 8).toByte()
        data[5] = (copies and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_DIMENSION.code, data)
    }

    fun setQuantity(n: Int): NiimbotPacket {
        require(n in 1..65535) { "Quantity must be 1-65535, got $n" }
        val data = ByteArray(2)
        data[0] = (n shr 8).toByte()
        data[1] = (n and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_QUANTITY.code, data)
    }

    fun getPrintStatus(): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_PRINT_STATUS.code, byteArrayOf(0x01))

    /**
     * Build an image row packet (type 0x85, fire-and-forget).
     *
     * @param y Row index (0-based)
     * @param lineData Packed 1-bit monochrome pixel data for this row
     */
    fun imageRow(y: Int, lineData: ByteArray): NiimbotPacket {
        val header = ByteArray(6)
        header[0] = (y shr 8).toByte()
        header[1] = (y and 0xFF).toByte()
        header[2] = 0x00 // count1 — always 0
        header[3] = 0x00 // count2 — always 0
        header[4] = 0x00 // count3 — always 0
        header[5] = 0x01 // flag — always 1
        return NiimbotPacket(IMAGE_DATA_TYPE, header + lineData)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ~/github/BluePaper && ./gradlew :shared:desktopTest`
Expected: PASS — all tests green

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilder.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/protocol/CommandBuilderTest.kt
git commit -m "feat: CommandBuilder — typed packet construction for all printer commands"
```

---

### Task 7: CI Pipeline

**Files:**
- Create: `.github/workflows/ci.yaml`

- [ ] **Step 1: Create CI workflow**

```yaml
# .github/workflows/ci.yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run shared tests (desktop target)
        run: ./gradlew :shared:desktopTest

      - name: Check compilation
        run: ./gradlew :shared:compileKotlinDesktop
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yaml
git commit -m "ci: add GitHub Actions workflow — desktop tests + compilation check"
```

---

### Task 8: README and .gitignore

**Files:**
- Create: `README.md`
- Create: `.gitignore`

- [ ] **Step 1: Create .gitignore**

```gitignore
# .gitignore
.gradle/
build/
.idea/
*.iml
local.properties
.DS_Store
*.hprof
```

- [ ] **Step 2: Create README**

```markdown
# BluePaper

Cross-platform Bluetooth label printer app built with Kotlin Multiplatform and Compose Multiplatform.

## Supported Platforms

- Android
- iOS
- Linux
- macOS
- Windows

## Supported Printers

| Model | DPI | Protocol |
|-------|-----|----------|
| D11 | 203 | V1 |
| D11-H | 300 | V1 |
| D101 | 203 | V1 |
| D110 | 203 | V1 |
| D110-M | 300 | V1 |
| B1 | 203 | V2 |
| B18 | 203 | V2 |
| B21 | 203 | V2 |

## Building

```bash
# Run tests
./gradlew :shared:desktopTest

# Build desktop app
./gradlew :desktopApp:run

# Build Android APK
./gradlew :androidApp:assembleDebug
```

## License

MIT
```

- [ ] **Step 3: Create LICENSE**

```
MIT License

Copyright (c) 2026 Avicennasis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 4: Commit**

```bash
git add README.md .gitignore LICENSE
git commit -m "docs: README, .gitignore, and MIT license"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] KMP project scaffold (Task 1)
- [x] Packet encode/decode/checksum (Task 2)
- [x] All command enums (Task 3)
- [x] All response types — Heartbeat, PrintStatus, RFID, PrinterInfo (Task 4)
- [x] All 8 device configs with label sizes (Task 5)
- [x] Command builder with exact byte payloads for all V1/V2 commands (Task 6)
- [x] CI pipeline (Task 7)
- [x] Repo hygiene — README, .gitignore, LICENSE (Task 8)
- [ ] BLE transport — deferred to Plan 2
- [ ] Image encoding — deferred to Plan 3
- [ ] Compose UI — deferred to Plan 4

**2. Placeholder scan:** No TBDs, TODOs, or "similar to" references found.

**3. Type consistency:**
- `NiimbotPacket(type: Int, data: ByteArray)` — consistent across Task 2, 6
- `RequestCode.code` — used consistently in CommandBuilder
- `InfoEnum.key` — used consistently in CommandBuilder.getInfo
- `HeartbeatResponse.fromData(data: ByteArray)` — matches protocol data format
- `PrintStatus.fromData(data: ByteArray)` — matches protocol data format
- `DeviceConfig.isV2` — used consistently in registry and tests
- `LabelSize(widthMm, heightMm, dpi)` — consistent in DeviceRegistry sizes() helper
