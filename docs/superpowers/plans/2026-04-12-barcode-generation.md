# BluePaper v0.4.0 Barcode Generation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add QR code and barcode generation with 16 formats, 13 data standards, and structured data entry to the BluePaper label editor.

**Architecture:** New `BarcodeElement` sealed class variant + `BarcodeFormat`/`DataStandard` enums + `DataEncoder` interface with 13 concrete encoders + dual-library rendering (qrose KMP + ZXing JVM) + categorized picker UI + properties panel with structured forms.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, qrose 1.1.0 (KMP barcode lib), ZXing core 3.5.3 (JVM barcode lib), kotlinx.serialization.

**Build/Test commands:**
```bash
ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest
ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop
ANDROID_HOME=~/Android/Sdk ./gradlew :desktopApp:run
```

**Base path:** `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/`
**Test base:** `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/`

---

## File Plan

### New Files — Core Types (commonMain)
| File | Responsibility |
|------|---------------|
| `ui/editor/BarcodeFormat.kt` | `BarcodeFormat` enum (16 formats), `BarcodeLibrary` enum, `ErrorCorrection` enum |
| `ui/editor/DataStandard.kt` | `DataStandard` enum (13 standards), `DataField` data class, `FieldType` enum |
| `ui/editor/DataEncoder.kt` | `DataEncoder` interface + `DataEncoderRegistry` object |

### New Files — Data Encoders (commonMain, one per encoder)
| File | Responsibility |
|------|---------------|
| `ui/editor/encoders/VCardEncoder.kt` | vCard (RFC 6350) encode/decode |
| `ui/editor/encoders/UrlEncoder.kt` | URL pass-through + validation |
| `ui/editor/encoders/WifiEncoder.kt` | WiFi WIFI:T:...;S:...;P:...; |
| `ui/editor/encoders/MeCardEncoder.kt` | MeCard MECARD:N:...;TEL:...; |
| `ui/editor/encoders/SmsEncoder.kt` | SMS smsto:number:message |
| `ui/editor/encoders/EmailEncoder.kt` | Email mailto: with params |
| `ui/editor/encoders/PhoneEncoder.kt` | Phone tel: URI |
| `ui/editor/encoders/GeoEncoder.kt` | Geo geo:lat,lon |
| `ui/editor/encoders/AamvaEncoder.kt` | AAMVA ANSI 636 for PDF417 |
| `ui/editor/encoders/Gs1128Encoder.kt` | GS1-128 Application Identifiers |
| `ui/editor/encoders/Gs1DataMatrixEncoder.kt` | GS1 DataMatrix AIs |
| `ui/editor/encoders/HibcEncoder.kt` | HIBC +LIC/PCN format |

### New Files — Validation + Rendering
| File | Responsibility |
|------|---------------|
| `ui/editor/BarcodeValidator.kt` | Format validation, auto-fix, check digit computation |
| `ui/editor/BarcodeRenderer.kt` | expect object declaration |
| `desktopMain/.../BarcodeRenderer.desktop.kt` | actual: qrose + ZXing rendering |
| `androidMain/.../BarcodeRenderer.android.kt` | actual: qrose + ZXing rendering |
| `iosMain/.../BarcodeRenderer.ios.kt` | actual: qrose only |

### New Files — UI
| File | Responsibility |
|------|---------------|
| `ui/editor/BarcodeFormatPicker.kt` | Categorized format+standard picker dialog |
| `ui/editor/BarcodeProperties.kt` | Barcode-specific properties panel section |
| `ui/editor/DataStandardForm.kt` | Structured field form for data standards |

### Modified Files
| File | Changes |
|------|---------|
| `ui/editor/LabelElement.kt` | Add BarcodeElement + serialization fields |
| `ui/editor/EditorState.kt` | Add barcode CRUD methods |
| `ui/editor/LabelCanvas.kt` | Add drawBarcodeElement() |
| `ui/editor/UndoManager.kt` | Add BarcodeElement to deepCopy |
| `ui/editor/ToolboxPanel.kt` | Add "+ Barcode" button |
| `ui/editor/PropertiesPanel.kt` | Add BarcodeElement branch |
| `shared/build.gradle.kts` | Add qrose + ZXing dependencies |

### New Test Files
| File | Tests |
|------|-------|
| `ui/editor/BarcodeFormatTest.kt` | Enum properties, categories, library mapping |
| `ui/editor/BarcodeValidatorTest.kt` | Check digits, auto-fix, validation rules |
| `ui/editor/BarcodeSerializationTest.kt` | Roundtrip serialization for BarcodeElement |
| `ui/editor/encoders/VCardEncoderTest.kt` | vCard encode/decode roundtrip |
| `ui/editor/encoders/WifiEncoderTest.kt` | WiFi encode/decode |
| `ui/editor/encoders/MeCardEncoderTest.kt` | MeCard encode/decode |
| `ui/editor/encoders/SmsEncoderTest.kt` | SMS encode/decode |
| `ui/editor/encoders/EmailEncoderTest.kt` | Email encode/decode |
| `ui/editor/encoders/GeoEncoderTest.kt` | Geo encode/decode |
| `ui/editor/encoders/AamvaEncoderTest.kt` | AAMVA encode/decode |
| `ui/editor/encoders/Gs1EncoderTest.kt` | GS1-128 + GS1 DataMatrix encode/decode |
| `ui/editor/encoders/HibcEncoderTest.kt` | HIBC encode/decode |

---

## Task 1: Build Configuration — Add Dependencies

**Files:**
- Modify: `shared/build.gradle.kts`

- [ ] **Step 1: Add qrose and ZXing dependencies**

Edit `shared/build.gradle.kts`. In the `sourceSets` block, add qrose to commonMain and ZXing to desktopMain and androidMain:

```kotlin
val commonMain by getting {
    dependencies {
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kable.core)
        implementation("io.github.alexzhirkevich:qrose:1.1.0")
    }
}

val desktopMain by getting {
    dependencies {
        implementation(compose.desktop.currentOs)
        implementation("com.google.zxing:core:3.5.3")
    }
}

val androidMain by getting {
    dependencies {
        implementation("com.google.zxing:core:3.5.3")
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL (dependencies resolve).

- [ ] **Step 3: Commit**

```bash
git add shared/build.gradle.kts
git commit -m "build: add qrose 1.1.0 and ZXing 3.5.3 dependencies"
```

---

## Task 2: Core Types — BarcodeFormat + ErrorCorrection

**Files:**
- Create: `ui/editor/BarcodeFormat.kt`
- Test: `ui/editor/BarcodeFormatTest.kt`

- [ ] **Step 1: Write BarcodeFormat tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormatTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BarcodeFormatTest {

    @Test
    fun totalFormatCount() {
        assertEquals(16, BarcodeFormat.entries.size)
    }

    @Test
    fun qrCodeIs2D() {
        assertTrue(BarcodeFormat.QR_CODE.is2D)
        assertEquals("2D Codes", BarcodeFormat.QR_CODE.category)
        assertEquals(BarcodeLibrary.QROSE, BarcodeFormat.QR_CODE.library)
    }

    @Test
    fun code128Is1D() {
        assertFalse(BarcodeFormat.CODE_128.is2D)
        assertEquals("Linear Codes", BarcodeFormat.CODE_128.category)
        assertEquals(BarcodeLibrary.QROSE, BarcodeFormat.CODE_128.library)
    }

    @Test
    fun pdf417IsZxing() {
        assertEquals(BarcodeLibrary.ZXING, BarcodeFormat.PDF_417.library)
        assertTrue(BarcodeFormat.PDF_417.is2D)
    }

    @Test
    fun categoriesAreThree() {
        val categories = BarcodeFormat.entries.map { it.category }.distinct()
        assertEquals(3, categories.size)
        assertTrue(categories.contains("2D Codes"))
        assertTrue(categories.contains("Linear Codes"))
        assertTrue(categories.contains("Retail"))
    }

    @Test
    fun retailFormatsAreQrose() {
        val retail = BarcodeFormat.entries.filter { it.category == "Retail" }
        assertEquals(4, retail.size)
        assertTrue(retail.all { it.library == BarcodeLibrary.QROSE })
    }

    @Test
    fun errorCorrectionLevels() {
        assertEquals(4, ErrorCorrection.entries.size)
        assertEquals("Low (7%)", ErrorCorrection.L.displayName)
        assertEquals("High (30%)", ErrorCorrection.H.displayName)
    }
}
```

- [ ] **Step 2: Implement BarcodeFormat.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormat.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

enum class BarcodeLibrary { QROSE, ZXING }

enum class ErrorCorrection(val displayName: String) {
    L("Low (7%)"),
    M("Medium (15%)"),
    Q("Quartile (25%)"),
    H("High (30%)"),
}

enum class BarcodeFormat(
    val displayName: String,
    val category: String,
    val is2D: Boolean,
    val library: BarcodeLibrary,
) {
    // 2D Codes
    QR_CODE("QR Code", "2D Codes", true, BarcodeLibrary.QROSE),
    PDF_417("PDF417", "2D Codes", true, BarcodeLibrary.ZXING),
    DATA_MATRIX("Data Matrix", "2D Codes", true, BarcodeLibrary.ZXING),
    AZTEC("Aztec", "2D Codes", true, BarcodeLibrary.ZXING),
    MAXICODE("MaxiCode", "2D Codes", true, BarcodeLibrary.ZXING),
    RSS_EXPANDED("RSS Expanded", "2D Codes", true, BarcodeLibrary.ZXING),

    // Linear Codes
    CODE_128("Code 128", "Linear Codes", false, BarcodeLibrary.QROSE),
    CODE_39("Code 39", "Linear Codes", false, BarcodeLibrary.QROSE),
    CODE_93("Code 93", "Linear Codes", false, BarcodeLibrary.QROSE),
    CODABAR("Codabar", "Linear Codes", false, BarcodeLibrary.QROSE),
    ITF("ITF", "Linear Codes", false, BarcodeLibrary.QROSE),
    RSS_14("RSS-14", "Linear Codes", false, BarcodeLibrary.ZXING),

    // Retail
    EAN_13("EAN-13", "Retail", false, BarcodeLibrary.QROSE),
    EAN_8("EAN-8", "Retail", false, BarcodeLibrary.QROSE),
    UPC_A("UPC-A", "Retail", false, BarcodeLibrary.QROSE),
    UPC_E("UPC-E", "Retail", false, BarcodeLibrary.QROSE),
    ;

    companion object {
        fun byCategory(): Map<String, List<BarcodeFormat>> =
            entries.groupBy { it.category }
    }
}
```

- [ ] **Step 3: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.BarcodeFormatTest" -q`
Expected: All 7 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormat.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormatTest.kt
git commit -m "feat: BarcodeFormat enum with 16 formats + ErrorCorrection"
```

---

## Task 3: Core Types — DataStandard + DataEncoder Interface

**Files:**
- Create: `ui/editor/DataStandard.kt`
- Create: `ui/editor/DataEncoder.kt`

- [ ] **Step 1: Create DataStandard.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataStandard.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

enum class FieldType { TEXT, NUMBER, EMAIL, PHONE, URL }

data class DataField(
    val key: String,
    val label: String,
    val required: Boolean = false,
    val hint: String = "",
    val fieldType: FieldType = FieldType.TEXT,
)

enum class DataStandard(
    val displayName: String,
    val description: String,
    val applicableFormats: Set<BarcodeFormat>,
) {
    RAW_TEXT("Raw Text", "Plain text data", BarcodeFormat.entries.toSet()),
    VCARD("vCard", "Contact card", setOf(BarcodeFormat.QR_CODE)),
    URL("URL", "Web link", setOf(BarcodeFormat.QR_CODE)),
    WIFI("WiFi", "Network credentials", setOf(BarcodeFormat.QR_CODE)),
    MECARD("MeCard", "Simplified contact", setOf(BarcodeFormat.QR_CODE)),
    SMS("SMS", "Text message", setOf(BarcodeFormat.QR_CODE)),
    EMAIL("Email", "Email message", setOf(BarcodeFormat.QR_CODE)),
    PHONE("Phone", "Phone number", setOf(BarcodeFormat.QR_CODE)),
    GEO("Geo Location", "GPS coordinates", setOf(BarcodeFormat.QR_CODE)),
    AAMVA("AAMVA", "Driver's license / ID", setOf(BarcodeFormat.PDF_417)),
    GS1_128("GS1-128", "Supply chain AIs", setOf(BarcodeFormat.CODE_128)),
    GS1_DATAMATRIX("GS1 DataMatrix", "Product ID AIs", setOf(BarcodeFormat.DATA_MATRIX)),
    HIBC("HIBC", "Health Industry Bar Code", setOf(BarcodeFormat.DATA_MATRIX, BarcodeFormat.CODE_128)),
    ;

    companion object {
        fun forFormat(format: BarcodeFormat): List<DataStandard> =
            entries.filter { format in it.applicableFormats }
    }
}
```

- [ ] **Step 2: Create DataEncoder.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

interface DataEncoder {
    val standard: DataStandard
    fun fields(): List<DataField>
    fun encode(fields: Map<String, String>): String
    fun decode(data: String): Map<String, String>
}

object DataEncoderRegistry {
    private val encoders = mutableMapOf<DataStandard, DataEncoder>()

    fun register(encoder: DataEncoder) {
        encoders[encoder.standard] = encoder
    }

    fun get(standard: DataStandard): DataEncoder? = encoders[standard]

    fun encode(standard: DataStandard, fields: Map<String, String>): String =
        encoders[standard]?.encode(fields) ?: fields.values.joinToString(" ")

    fun decode(standard: DataStandard, data: String): Map<String, String> =
        encoders[standard]?.decode(data) ?: mapOf("data" to data)

    fun fieldsFor(standard: DataStandard): List<DataField> =
        encoders[standard]?.fields() ?: listOf(DataField("data", "Data", required = true))
}
```

- [ ] **Step 3: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataStandard.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataEncoder.kt
git commit -m "feat: DataStandard enum (13 standards) + DataEncoder interface"
```

---

## Task 4: BarcodeValidator + Check Digits

**Files:**
- Create: `ui/editor/BarcodeValidator.kt`
- Test: `ui/editor/BarcodeValidatorTest.kt`

- [ ] **Step 1: Write BarcodeValidator tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeValidatorTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BarcodeValidatorTest {

    @Test
    fun ean13CheckDigit() {
        // "590123412345" → check digit 7 → "5901234123457"
        val result = BarcodeValidator.autoFix(BarcodeFormat.EAN_13, "590123412345")
        assertEquals("5901234123457", result)
    }

    @Test
    fun ean13PadsShortInput() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.EAN_13, "123")
        assertEquals(13, result.length)
        assertTrue(result.startsWith("000000000123"))
    }

    @Test
    fun ean8CheckDigit() {
        // "1234567" → check digit 0 → "12345670"
        val result = BarcodeValidator.autoFix(BarcodeFormat.EAN_8, "1234567")
        assertEquals("12345670", result)
    }

    @Test
    fun upcAPadsAndChecks() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.UPC_A, "03600029145")
        assertEquals(12, result.length)
        assertEquals("036000291452", result)
    }

    @Test
    fun code39Uppercases() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.CODE_39, "hello world")
        assertEquals("HELLO WORLD", result)
    }

    @Test
    fun itfPadsToEvenLength() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.ITF, "123")
        assertEquals("0123", result)
    }

    @Test
    fun itfEvenLengthUnchanged() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.ITF, "1234")
        assertEquals("1234", result)
    }

    @Test
    fun qrCodeAcceptsAnything() {
        val result = BarcodeValidator.validate(BarcodeFormat.QR_CODE, "anything goes! 🎉")
        assertTrue(result.isValid)
    }

    @Test
    fun ean13RejectsNonNumeric() {
        val result = BarcodeValidator.validate(BarcodeFormat.EAN_13, "abcdefghijklm")
        assertFalse(result.isValid)
        assertTrue(result.error != null)
    }

    @Test
    fun code39RejectsInvalidChars() {
        val result = BarcodeValidator.validate(BarcodeFormat.CODE_39, "hello@world")
        assertFalse(result.isValid)
    }

    @Test
    fun codabarValidation() {
        val result = BarcodeValidator.autoFix(BarcodeFormat.CODABAR, "12345")
        assertTrue(result.startsWith("A") && result.endsWith("A"))
    }

    @Test
    fun emptyDataIsInvalid() {
        val result = BarcodeValidator.validate(BarcodeFormat.CODE_128, "")
        assertFalse(result.isValid)
    }
}
```

- [ ] **Step 2: Implement BarcodeValidator.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeValidator.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

data class ValidationResult(
    val isValid: Boolean,
    val fixedData: String? = null,
    val error: String? = null,
)

object BarcodeValidator {

    fun validate(format: BarcodeFormat, data: String): ValidationResult {
        if (data.isEmpty()) return ValidationResult(false, error = "Data cannot be empty")

        return when (format) {
            BarcodeFormat.EAN_13 -> validateNumeric(data, "EAN-13")
            BarcodeFormat.EAN_8 -> validateNumeric(data, "EAN-8")
            BarcodeFormat.UPC_A -> validateNumeric(data, "UPC-A")
            BarcodeFormat.UPC_E -> validateNumeric(data, "UPC-E")
            BarcodeFormat.ITF -> validateNumeric(data, "ITF")
            BarcodeFormat.RSS_14 -> validateNumeric(data, "RSS-14")
            BarcodeFormat.CODE_39 -> validateCode39(data)
            BarcodeFormat.CODABAR -> validateCodabar(data)
            else -> ValidationResult(true)
        }
    }

    fun autoFix(format: BarcodeFormat, data: String): String = when (format) {
        BarcodeFormat.EAN_13 -> fixEan13(data)
        BarcodeFormat.EAN_8 -> fixEan8(data)
        BarcodeFormat.UPC_A -> fixUpcA(data)
        BarcodeFormat.UPC_E -> fixUpcE(data)
        BarcodeFormat.CODE_39 -> data.uppercase().filter { it in CODE_39_CHARS }
        BarcodeFormat.ITF -> fixItf(data.filter { it.isDigit() })
        BarcodeFormat.CODABAR -> fixCodabar(data)
        else -> data
    }

    // --- Check digit (mod-10 / Luhn for EAN/UPC) ---

    fun computeCheckDigit(digits: String): Int {
        var sum = 0
        for (i in digits.indices) {
            val d = digits[i] - '0'
            sum += if (i % 2 == 0) d else d * 3
        }
        return (10 - (sum % 10)) % 10
    }

    // --- Format-specific fixers ---

    private fun fixEan13(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(12, '0').take(12)
        return padded + computeCheckDigit(padded)
    }

    private fun fixEan8(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(7, '0').take(7)
        return padded + computeCheckDigit(padded)
    }

    private fun fixUpcA(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(11, '0').take(11)
        return padded + computeCheckDigit(padded)
    }

    private fun fixUpcE(data: String): String {
        val digits = data.filter { it.isDigit() }
        val padded = digits.padStart(7, '0').take(7)
        return padded + computeCheckDigit(padded)
    }

    private fun fixItf(data: String): String =
        if (data.length % 2 != 0) "0$data" else data

    private fun fixCodabar(data: String): String {
        val upper = data.uppercase()
        val body = upper.filter { it in CODABAR_CHARS }
        val starts = setOf('A', 'B', 'C', 'D')
        val hasStart = body.isNotEmpty() && body.first() in starts
        val hasStop = body.isNotEmpty() && body.last() in starts
        return when {
            hasStart && hasStop -> body
            hasStart -> body + "A"
            hasStop -> "A" + body
            else -> "A${body}A"
        }
    }

    // --- Validators ---

    private fun validateNumeric(data: String, formatName: String): ValidationResult =
        if (data.all { it.isDigit() }) ValidationResult(true)
        else ValidationResult(false, error = "$formatName requires numeric data only")

    private fun validateCode39(data: String): ValidationResult {
        val upper = data.uppercase()
        return if (upper.all { it in CODE_39_CHARS }) ValidationResult(true)
        else ValidationResult(false, error = "Code 39 allows: A-Z 0-9 - . $ / + % SPACE")
    }

    private fun validateCodabar(data: String): ValidationResult {
        val upper = data.uppercase()
        return if (upper.all { it in CODABAR_CHARS }) ValidationResult(true)
        else ValidationResult(false, error = "Codabar allows: 0-9 - $ : / . + A B C D")
    }

    private val CODE_39_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-. $/+%".toSet()
    private val CODABAR_CHARS = "0123456789-\$:/.+ABCD".toSet()
}
```

- [ ] **Step 3: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.BarcodeValidatorTest" -q`
Expected: All 12 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeValidator.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeValidatorTest.kt
git commit -m "feat: BarcodeValidator with check digits and auto-fix rules"
```

---

## Task 5: Data Encoders — vCard

**Files:**
- Create: `ui/editor/encoders/VCardEncoder.kt`
- Test: `ui/editor/encoders/VCardEncoderTest.kt`

- [ ] **Step 1: Write tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/VCardEncoderTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataStandard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VCardEncoderTest {

    private val encoder = VCardEncoder()

    @Test
    fun standardIsVcard() {
        assertEquals(DataStandard.VCARD, encoder.standard)
    }

    @Test
    fun fieldsContainRequiredFields() {
        val fields = encoder.fields()
        val required = fields.filter { it.required }.map { it.key }
        assertTrue("lastName" in required)
        assertTrue("phone" in required)
        assertTrue("email" in required)
    }

    @Test
    fun encodeBasicContact() {
        val fields = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "phone" to "+1234567890",
            "email" to "john@example.com",
        )
        val result = encoder.encode(fields)
        assertTrue(result.contains("BEGIN:VCARD"))
        assertTrue(result.contains("VERSION:3.0"))
        assertTrue(result.contains("N:Doe;John"))
        assertTrue(result.contains("FN:John Doe"))
        assertTrue(result.contains("TEL:+1234567890"))
        assertTrue(result.contains("EMAIL:john@example.com"))
        assertTrue(result.contains("END:VCARD"))
    }

    @Test
    fun encodeWithOrganization() {
        val fields = mapOf(
            "firstName" to "Jane",
            "lastName" to "Smith",
            "organization" to "Acme Corp",
            "phone" to "+9876543210",
            "email" to "jane@acme.com",
        )
        val result = encoder.encode(fields)
        assertTrue(result.contains("ORG:Acme Corp"))
    }

    @Test
    fun decodeRoundTrip() {
        val original = mapOf(
            "firstName" to "John",
            "lastName" to "Doe",
            "phone" to "+1234567890",
            "email" to "john@example.com",
        )
        val encoded = encoder.encode(original)
        val decoded = encoder.decode(encoded)
        assertEquals("John", decoded["firstName"])
        assertEquals("Doe", decoded["lastName"])
        assertEquals("+1234567890", decoded["phone"])
        assertEquals("john@example.com", decoded["email"])
    }
}
```

- [ ] **Step 2: Implement VCardEncoder.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/VCardEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class VCardEncoder : DataEncoder {

    override val standard = DataStandard.VCARD

    override fun fields() = listOf(
        DataField("firstName", "First Name", hint = "John"),
        DataField("lastName", "Last Name", required = true, hint = "Doe"),
        DataField("organization", "Organization", hint = "Acme Corp"),
        DataField("title", "Title", hint = "Engineer"),
        DataField("phone", "Phone", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
        DataField("email", "Email", required = true, hint = "user@example.com", fieldType = FieldType.EMAIL),
        DataField("street", "Street", hint = "123 Main St"),
        DataField("city", "City", hint = "Springfield"),
        DataField("state", "State", hint = "IL"),
        DataField("zip", "Zip", hint = "62704"),
        DataField("country", "Country", hint = "US"),
        DataField("url", "URL", hint = "https://example.com", fieldType = FieldType.URL),
        DataField("note", "Note"),
    )

    override fun encode(fields: Map<String, String>): String {
        val first = fields["firstName"] ?: ""
        val last = fields["lastName"] ?: ""
        val lines = mutableListOf(
            "BEGIN:VCARD",
            "VERSION:3.0",
            "N:$last;$first",
            "FN:$first $last".trim(),
        )
        fields["organization"]?.takeIf { it.isNotEmpty() }?.let { lines.add("ORG:$it") }
        fields["title"]?.takeIf { it.isNotEmpty() }?.let { lines.add("TITLE:$it") }
        fields["phone"]?.takeIf { it.isNotEmpty() }?.let { lines.add("TEL:$it") }
        fields["email"]?.takeIf { it.isNotEmpty() }?.let { lines.add("EMAIL:$it") }

        val addr = listOf("street", "city", "state", "zip", "country")
            .mapNotNull { fields[it]?.takeIf { v -> v.isNotEmpty() } }
        if (addr.isNotEmpty()) {
            val street = fields["street"] ?: ""
            val city = fields["city"] ?: ""
            val state = fields["state"] ?: ""
            val zip = fields["zip"] ?: ""
            val country = fields["country"] ?: ""
            lines.add("ADR:;;$street;$city;$state;$zip;$country")
        }

        fields["url"]?.takeIf { it.isNotEmpty() }?.let { lines.add("URL:$it") }
        fields["note"]?.takeIf { it.isNotEmpty() }?.let { lines.add("NOTE:$it") }
        lines.add("END:VCARD")
        return lines.joinToString("\n")
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = data.lines()
        for (line in lines) {
            when {
                line.startsWith("N:") -> {
                    val parts = line.removePrefix("N:").split(";")
                    if (parts.size >= 2) {
                        result["lastName"] = parts[0]
                        result["firstName"] = parts[1]
                    }
                }
                line.startsWith("ORG:") -> result["organization"] = line.removePrefix("ORG:")
                line.startsWith("TITLE:") -> result["title"] = line.removePrefix("TITLE:")
                line.startsWith("TEL:") -> result["phone"] = line.removePrefix("TEL:")
                line.startsWith("EMAIL:") -> result["email"] = line.removePrefix("EMAIL:")
                line.startsWith("ADR:") -> {
                    val parts = line.removePrefix("ADR:").split(";")
                    if (parts.size >= 7) {
                        result["street"] = parts[2]
                        result["city"] = parts[3]
                        result["state"] = parts[4]
                        result["zip"] = parts[5]
                        result["country"] = parts[6]
                    }
                }
                line.startsWith("URL:") -> result["url"] = line.removePrefix("URL:")
                line.startsWith("NOTE:") -> result["note"] = line.removePrefix("NOTE:")
            }
        }
        return result
    }
}
```

- [ ] **Step 3: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.encoders.VCardEncoderTest" -q`
Expected: All 5 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/VCardEncoder.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/VCardEncoderTest.kt
git commit -m "feat: vCard data encoder with RFC 6350 format"
```

---

## Task 6: Data Encoders — WiFi + MeCard + URL

**Files:**
- Create: `ui/editor/encoders/WifiEncoder.kt`, `MeCardEncoder.kt`, `UrlEncoder.kt`
- Test: `ui/editor/encoders/WifiEncoderTest.kt`, `MeCardEncoderTest.kt`

These are simple string-format encoders that follow similar patterns. Grouped to reduce task overhead.

- [ ] **Step 1: Implement and test all three**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/WifiEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class WifiEncoder : DataEncoder {
    override val standard = DataStandard.WIFI
    override fun fields() = listOf(
        DataField("ssid", "SSID (Network Name)", required = true, hint = "MyNetwork"),
        DataField("password", "Password", hint = "MyPassword"),
        DataField("encryption", "Encryption", required = true, hint = "WPA"),
    )
    override fun encode(fields: Map<String, String>): String {
        val t = fields["encryption"] ?: "WPA"
        val s = fields["ssid"] ?: ""
        val p = fields["password"] ?: ""
        return "WIFI:T:$t;S:$s;P:$p;;"
    }
    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val body = data.removePrefix("WIFI:").removeSuffix(";;").removeSuffix(";")
        for (part in body.split(";")) {
            val (k, v) = part.split(":", limit = 2).takeIf { it.size == 2 } ?: continue
            when (k) { "T" -> result["encryption"] = v; "S" -> result["ssid"] = v; "P" -> result["password"] = v }
        }
        return result
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/MeCardEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class MeCardEncoder : DataEncoder {
    override val standard = DataStandard.MECARD
    override fun fields() = listOf(
        DataField("name", "Name", required = true, hint = "Doe,John"),
        DataField("phone", "Phone", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
        DataField("email", "Email", hint = "user@example.com", fieldType = FieldType.EMAIL),
        DataField("url", "URL", hint = "https://example.com", fieldType = FieldType.URL),
        DataField("address", "Address", hint = "123 Main St"),
        DataField("note", "Note"),
    )
    override fun encode(fields: Map<String, String>): String {
        val parts = mutableListOf("MECARD:")
        fields["name"]?.takeIf { it.isNotEmpty() }?.let { parts.add("N:$it;") }
        fields["phone"]?.takeIf { it.isNotEmpty() }?.let { parts.add("TEL:$it;") }
        fields["email"]?.takeIf { it.isNotEmpty() }?.let { parts.add("EMAIL:$it;") }
        fields["url"]?.takeIf { it.isNotEmpty() }?.let { parts.add("URL:$it;") }
        fields["address"]?.takeIf { it.isNotEmpty() }?.let { parts.add("ADR:$it;") }
        fields["note"]?.takeIf { it.isNotEmpty() }?.let { parts.add("NOTE:$it;") }
        parts.add(";")
        return parts.joinToString("")
    }
    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val body = data.removePrefix("MECARD:").removeSuffix(";;").removeSuffix(";")
        for (part in body.split(";")) {
            if (!part.contains(":")) continue
            val (k, v) = part.split(":", limit = 2)
            when (k) { "N" -> result["name"] = v; "TEL" -> result["phone"] = v; "EMAIL" -> result["email"] = v; "URL" -> result["url"] = v; "ADR" -> result["address"] = v; "NOTE" -> result["note"] = v }
        }
        return result
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/UrlEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class UrlEncoder : DataEncoder {
    override val standard = DataStandard.URL
    override fun fields() = listOf(
        DataField("url", "URL", required = true, hint = "https://example.com", fieldType = FieldType.URL),
    )
    override fun encode(fields: Map<String, String>): String = fields["url"] ?: ""
    override fun decode(data: String) = mapOf("url" to data)
}
```

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/WifiEncoderTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class WifiEncoderTest {
    private val encoder = WifiEncoder()

    @Test
    fun encodeWifi() {
        val result = encoder.encode(mapOf("ssid" to "MyNet", "password" to "secret", "encryption" to "WPA"))
        assertEquals("WIFI:T:WPA;S:MyNet;P:secret;;", result)
    }

    @Test
    fun decodeWifi() {
        val decoded = encoder.decode("WIFI:T:WPA;S:MyNet;P:secret;;")
        assertEquals("MyNet", decoded["ssid"])
        assertEquals("secret", decoded["password"])
        assertEquals("WPA", decoded["encryption"])
    }
}
```

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/MeCardEncoderTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeCardEncoderTest {
    private val encoder = MeCardEncoder()

    @Test
    fun encodeMeCard() {
        val result = encoder.encode(mapOf("name" to "Doe,John", "phone" to "+1234567890", "email" to "j@e.com"))
        assertTrue(result.startsWith("MECARD:"))
        assertTrue(result.contains("N:Doe,John"))
        assertTrue(result.contains("TEL:+1234567890"))
    }

    @Test
    fun decodeMeCard() {
        val decoded = encoder.decode("MECARD:N:Doe,John;TEL:+1234567890;EMAIL:j@e.com;;")
        assertEquals("Doe,John", decoded["name"])
        assertEquals("+1234567890", decoded["phone"])
    }
}
```

- [ ] **Step 2: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.encoders.*" -q`
Expected: All PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/ \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/
git commit -m "feat: WiFi, MeCard, URL data encoders"
```

---

## Task 7: Data Encoders — SMS + Email + Phone + Geo

**Files:**
- Create: `ui/editor/encoders/SmsEncoder.kt`, `EmailEncoder.kt`, `PhoneEncoder.kt`, `GeoEncoder.kt`
- Test: `ui/editor/encoders/SmsEncoderTest.kt`, `EmailEncoderTest.kt`, `GeoEncoderTest.kt`

- [ ] **Step 1: Implement all four encoders**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/SmsEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class SmsEncoder : DataEncoder {
    override val standard = DataStandard.SMS
    override fun fields() = listOf(
        DataField("phone", "Phone Number", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
        DataField("message", "Message", hint = "Hello!"),
    )
    override fun encode(fields: Map<String, String>): String {
        val phone = fields["phone"] ?: ""
        val msg = fields["message"] ?: ""
        return "smsto:$phone:$msg"
    }
    override fun decode(data: String): Map<String, String> {
        val body = data.removePrefix("smsto:")
        val parts = body.split(":", limit = 2)
        return mapOf("phone" to parts.getOrElse(0) { "" }, "message" to parts.getOrElse(1) { "" })
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/EmailEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class EmailEncoder : DataEncoder {
    override val standard = DataStandard.EMAIL
    override fun fields() = listOf(
        DataField("address", "Email Address", required = true, hint = "user@example.com", fieldType = FieldType.EMAIL),
        DataField("subject", "Subject", hint = "Hello"),
        DataField("body", "Body", hint = "Message text"),
    )
    override fun encode(fields: Map<String, String>): String {
        val addr = fields["address"] ?: ""
        val params = mutableListOf<String>()
        fields["subject"]?.takeIf { it.isNotEmpty() }?.let { params.add("subject=$it") }
        fields["body"]?.takeIf { it.isNotEmpty() }?.let { params.add("body=$it") }
        return if (params.isEmpty()) "mailto:$addr" else "mailto:$addr?${params.joinToString("&")}"
    }
    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val body = data.removePrefix("mailto:")
        val parts = body.split("?", limit = 2)
        result["address"] = parts[0]
        if (parts.size > 1) {
            for (param in parts[1].split("&")) {
                val (k, v) = param.split("=", limit = 2).takeIf { it.size == 2 } ?: continue
                result[k] = v
            }
        }
        return result
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/PhoneEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class PhoneEncoder : DataEncoder {
    override val standard = DataStandard.PHONE
    override fun fields() = listOf(
        DataField("number", "Phone Number", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
    )
    override fun encode(fields: Map<String, String>) = "tel:${fields["number"] ?: ""}"
    override fun decode(data: String) = mapOf("number" to data.removePrefix("tel:"))
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/GeoEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class GeoEncoder : DataEncoder {
    override val standard = DataStandard.GEO
    override fun fields() = listOf(
        DataField("latitude", "Latitude", required = true, hint = "40.7128", fieldType = FieldType.NUMBER),
        DataField("longitude", "Longitude", required = true, hint = "-74.0060", fieldType = FieldType.NUMBER),
    )
    override fun encode(fields: Map<String, String>): String {
        val lat = fields["latitude"] ?: "0"
        val lon = fields["longitude"] ?: "0"
        return "geo:$lat,$lon"
    }
    override fun decode(data: String): Map<String, String> {
        val coords = data.removePrefix("geo:").split(",", limit = 2)
        return mapOf("latitude" to coords.getOrElse(0) { "0" }, "longitude" to coords.getOrElse(1) { "0" })
    }
}
```

Create test files:

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/SmsEncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class SmsEncoderTest {
    private val encoder = SmsEncoder()

    @Test
    fun encodeSms() {
        assertEquals("smsto:+1234567890:Hello!", encoder.encode(mapOf("phone" to "+1234567890", "message" to "Hello!")))
    }

    @Test
    fun decodeSms() {
        val decoded = encoder.decode("smsto:+1234567890:Hello!")
        assertEquals("+1234567890", decoded["phone"])
        assertEquals("Hello!", decoded["message"])
    }
}
```

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/EmailEncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmailEncoderTest {
    private val encoder = EmailEncoder()

    @Test
    fun encodeWithSubject() {
        val result = encoder.encode(mapOf("address" to "a@b.com", "subject" to "Hi", "body" to "Hello"))
        assertEquals("mailto:a@b.com?subject=Hi&body=Hello", result)
    }

    @Test
    fun encodeAddressOnly() {
        assertEquals("mailto:a@b.com", encoder.encode(mapOf("address" to "a@b.com")))
    }

    @Test
    fun decodeRoundTrip() {
        val decoded = encoder.decode("mailto:a@b.com?subject=Hi&body=Hello")
        assertEquals("a@b.com", decoded["address"])
        assertEquals("Hi", decoded["subject"])
    }
}
```

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/GeoEncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class GeoEncoderTest {
    private val encoder = GeoEncoder()

    @Test
    fun encodeGeo() {
        assertEquals("geo:40.7128,-74.0060", encoder.encode(mapOf("latitude" to "40.7128", "longitude" to "-74.0060")))
    }

    @Test
    fun decodeGeo() {
        val decoded = encoder.decode("geo:40.7128,-74.0060")
        assertEquals("40.7128", decoded["latitude"])
        assertEquals("-74.0060", decoded["longitude"])
    }
}
```

- [ ] **Step 2: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.encoders.*" -q`
Expected: All PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/ \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/
git commit -m "feat: SMS, Email, Phone, Geo data encoders"
```

---

## Task 8: Data Encoders — AAMVA + GS1-128 + GS1 DataMatrix + HIBC

**Files:**
- Create: `ui/editor/encoders/AamvaEncoder.kt`, `Gs1128Encoder.kt`, `Gs1DataMatrixEncoder.kt`, `HibcEncoder.kt`
- Test: `ui/editor/encoders/AamvaEncoderTest.kt`, `Gs1EncoderTest.kt`, `HibcEncoderTest.kt`

- [ ] **Step 1: Implement AAMVA encoder**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/AamvaEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class AamvaEncoder : DataEncoder {
    override val standard = DataStandard.AAMVA
    override fun fields() = listOf(
        DataField("firstName", "First Name", required = true),
        DataField("lastName", "Last Name", required = true),
        DataField("dob", "Date of Birth", required = true, hint = "MMDDYYYY"),
        DataField("licenseNumber", "License Number", required = true),
        DataField("street", "Street"),
        DataField("city", "City", required = true),
        DataField("state", "State", required = true, hint = "PA"),
        DataField("zip", "Zip Code", required = true),
        DataField("country", "Country", hint = "USA"),
        DataField("sex", "Sex", hint = "1=Male, 2=Female"),
        DataField("height", "Height", hint = "510 (5'10\")"),
        DataField("weight", "Weight", hint = "180"),
        DataField("eyeColor", "Eye Color", hint = "BRO"),
        DataField("hairColor", "Hair Color", hint = "BLK"),
        DataField("expirationDate", "Expiration Date", hint = "MMDDYYYY"),
    )

    override fun encode(fields: Map<String, String>): String {
        val sb = StringBuilder()
        // AAMVA header (simplified CDS v10 format)
        sb.append("@\n")
        sb.append("\u001e\r") // record separator + CR
        sb.append("ANSI 636000100002DL00410278ZP03180008\n")
        sb.append("DL") // subfile designator
        // Mandatory fields
        fields["lastName"]?.let { sb.append("DCS$it\n") }
        fields["firstName"]?.let { sb.append("DAC$it\n") }
        fields["dob"]?.let { sb.append("DBB$it\n") }
        fields["licenseNumber"]?.let { sb.append("DAQ$it\n") }
        fields["street"]?.let { if (it.isNotEmpty()) sb.append("DAG$it\n") }
        fields["city"]?.let { sb.append("DAI$it\n") }
        fields["state"]?.let { sb.append("DAJ$it\n") }
        fields["zip"]?.let { sb.append("DAK$it\n") }
        fields["country"]?.let { if (it.isNotEmpty()) sb.append("DCG$it\n") }
        fields["sex"]?.let { if (it.isNotEmpty()) sb.append("DBC$it\n") }
        fields["height"]?.let { if (it.isNotEmpty()) sb.append("DAU$it\n") }
        fields["weight"]?.let { if (it.isNotEmpty()) sb.append("DAW$it\n") }
        fields["eyeColor"]?.let { if (it.isNotEmpty()) sb.append("DAY$it\n") }
        fields["hairColor"]?.let { if (it.isNotEmpty()) sb.append("DAZ$it\n") }
        fields["expirationDate"]?.let { if (it.isNotEmpty()) sb.append("DBA$it\n") }
        return sb.toString()
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val fieldMap = mapOf(
            "DCS" to "lastName", "DAC" to "firstName", "DBB" to "dob",
            "DAQ" to "licenseNumber", "DAG" to "street", "DAI" to "city",
            "DAJ" to "state", "DAK" to "zip", "DCG" to "country",
            "DBC" to "sex", "DAU" to "height", "DAW" to "weight",
            "DAY" to "eyeColor", "DAZ" to "hairColor", "DBA" to "expirationDate",
        )
        for (line in data.lines()) {
            if (line.length < 3) continue
            val tag = line.take(3)
            fieldMap[tag]?.let { result[it] = line.drop(3) }
        }
        return result
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/Gs1128Encoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class Gs1128Encoder : DataEncoder {
    override val standard = DataStandard.GS1_128
    override fun fields() = listOf(
        DataField("gtin", "GTIN (AI 01)", hint = "09521234543213", fieldType = FieldType.NUMBER),
        DataField("batchLot", "Batch/Lot (AI 10)", hint = "ABC123"),
        DataField("serial", "Serial (AI 21)", hint = "SN001"),
        DataField("useBy", "Use By (AI 17)", hint = "260401 (YYMMDD)"),
        DataField("count", "Count (AI 30)", hint = "100", fieldType = FieldType.NUMBER),
    )

    override fun encode(fields: Map<String, String>): String {
        val parts = mutableListOf<String>()
        fields["gtin"]?.takeIf { it.isNotEmpty() }?.let { parts.add("(01)$it") }
        fields["useBy"]?.takeIf { it.isNotEmpty() }?.let { parts.add("(17)$it") }
        fields["batchLot"]?.takeIf { it.isNotEmpty() }?.let { parts.add("(10)$it") }
        fields["serial"]?.takeIf { it.isNotEmpty() }?.let { parts.add("(21)$it") }
        fields["count"]?.takeIf { it.isNotEmpty() }?.let { parts.add("(30)$it") }
        return parts.joinToString("")
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val aiMap = mapOf("01" to "gtin", "10" to "batchLot", "17" to "useBy", "21" to "serial", "30" to "count")
        val regex = Regex("""\((\d{2})\)([^(]*)""")
        for (match in regex.findAll(data)) {
            val ai = match.groupValues[1]
            val value = match.groupValues[2]
            aiMap[ai]?.let { result[it] = value }
        }
        return result
    }
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/Gs1DataMatrixEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class Gs1DataMatrixEncoder : DataEncoder {
    override val standard = DataStandard.GS1_DATAMATRIX
    // Same AI structure as GS1-128
    private val gs1 = Gs1128Encoder()
    override fun fields() = gs1.fields()
    override fun encode(fields: Map<String, String>) = gs1.encode(fields)
    override fun decode(data: String) = gs1.decode(data)
}
```

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/HibcEncoder.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.*

class HibcEncoder : DataEncoder {
    override val standard = DataStandard.HIBC
    override fun fields() = listOf(
        DataField("lic", "Labeler ID Code (LIC)", required = true, hint = "A123"),
        DataField("pcn", "Product/Catalog Number", required = true, hint = "PRODUCT01"),
        DataField("uom", "Unit of Measure", hint = "0"),
        DataField("quantity", "Quantity", hint = "1", fieldType = FieldType.NUMBER),
    )

    override fun encode(fields: Map<String, String>): String {
        val lic = fields["lic"] ?: ""
        val pcn = fields["pcn"] ?: ""
        val uom = fields["uom"]?.takeIf { it.isNotEmpty() } ?: "0"
        val qty = fields["quantity"]?.takeIf { it.isNotEmpty() }
        val data = "+$lic$pcn$uom"
        return if (qty != null) "$data/$qty" else data
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val body = data.removePrefix("+")
        val parts = body.split("/", limit = 2)
        if (parts[0].length >= 4) {
            result["lic"] = parts[0].take(4)
            val rest = parts[0].drop(4)
            if (rest.isNotEmpty()) {
                result["uom"] = rest.last().toString()
                result["pcn"] = rest.dropLast(1)
            }
        }
        if (parts.size > 1) result["quantity"] = parts[1]
        return result
    }
}
```

Create test files:

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/AamvaEncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AamvaEncoderTest {
    private val encoder = AamvaEncoder()

    @Test
    fun encodeContainsMandatoryFields() {
        val fields = mapOf("firstName" to "John", "lastName" to "Doe", "dob" to "01151990", "licenseNumber" to "D12345", "city" to "Pittsburgh", "state" to "PA", "zip" to "15213")
        val result = encoder.encode(fields)
        assertTrue(result.contains("DCSDoe"))
        assertTrue(result.contains("DACJohn"))
        assertTrue(result.contains("DBB01151990"))
        assertTrue(result.contains("DAQD12345"))
    }

    @Test
    fun decodeRoundTrip() {
        val fields = mapOf("firstName" to "John", "lastName" to "Doe", "dob" to "01151990", "licenseNumber" to "D12345", "city" to "Pittsburgh", "state" to "PA", "zip" to "15213")
        val encoded = encoder.encode(fields)
        val decoded = encoder.decode(encoded)
        assertEquals("John", decoded["firstName"])
        assertEquals("Doe", decoded["lastName"])
        assertEquals("PA", decoded["state"])
    }
}
```

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/Gs1EncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals

class Gs1EncoderTest {
    private val encoder = Gs1128Encoder()
    private val dmEncoder = Gs1DataMatrixEncoder()

    @Test
    fun encodeGs1128() {
        val result = encoder.encode(mapOf("gtin" to "09521234543213", "batchLot" to "ABC123", "useBy" to "260401"))
        assertEquals("(01)09521234543213(17)260401(10)ABC123", result)
    }

    @Test
    fun decodeGs1128() {
        val decoded = encoder.decode("(01)09521234543213(17)260401(10)ABC123")
        assertEquals("09521234543213", decoded["gtin"])
        assertEquals("ABC123", decoded["batchLot"])
        assertEquals("260401", decoded["useBy"])
    }

    @Test
    fun gs1DataMatrixSameAsGs1128() {
        val fields = mapOf("gtin" to "1234", "serial" to "SN01")
        assertEquals(encoder.encode(fields), dmEncoder.encode(fields))
    }
}
```

`shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/HibcEncoderTest.kt`:
```kotlin
package com.avicennasis.bluepaper.ui.editor.encoders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HibcEncoderTest {
    private val encoder = HibcEncoder()

    @Test
    fun encodeHibc() {
        val result = encoder.encode(mapOf("lic" to "A123", "pcn" to "PRODUCT01", "uom" to "0"))
        assertTrue(result.startsWith("+"))
        assertTrue(result.contains("A123"))
        assertTrue(result.contains("PRODUCT01"))
    }

    @Test
    fun encodeWithQuantity() {
        val result = encoder.encode(mapOf("lic" to "A123", "pcn" to "P01", "uom" to "0", "quantity" to "50"))
        assertTrue(result.contains("/50"))
    }
}
```

- [ ] **Step 2: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.encoders.*" -q`
Expected: All PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/ \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/encoders/
git commit -m "feat: AAMVA, GS1-128, GS1 DataMatrix, HIBC data encoders"
```

---

## Task 9: Register All Encoders

**Files:**
- Modify: `ui/editor/DataEncoder.kt`

- [ ] **Step 1: Add init block to register all encoders**

Add to `DataEncoderRegistry` in `DataEncoder.kt`:

```kotlin
object DataEncoderRegistry {
    private val encoders = mutableMapOf<DataStandard, DataEncoder>()

    init {
        register(com.avicennasis.bluepaper.ui.editor.encoders.VCardEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.UrlEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.WifiEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.MeCardEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.SmsEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.EmailEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.PhoneEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.GeoEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.AamvaEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.Gs1128Encoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.Gs1DataMatrixEncoder())
        register(com.avicennasis.bluepaper.ui.editor.encoders.HibcEncoder())
    }

    fun register(encoder: DataEncoder) { encoders[encoder.standard] = encoder }
    fun get(standard: DataStandard): DataEncoder? = encoders[standard]
    fun encode(standard: DataStandard, fields: Map<String, String>): String =
        encoders[standard]?.encode(fields) ?: fields.values.joinToString(" ")
    fun decode(standard: DataStandard, data: String): Map<String, String> =
        encoders[standard]?.decode(data) ?: mapOf("data" to data)
    fun fieldsFor(standard: DataStandard): List<DataField> =
        encoders[standard]?.fields() ?: listOf(DataField("data", "Data", required = true))
}
```

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All PASS.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataEncoder.kt
git commit -m "feat: register all 12 data encoders in DataEncoderRegistry"
```

---

## Task 10: LabelElement.kt — Add BarcodeElement + Serialization

**Files:**
- Modify: `ui/editor/LabelElement.kt`
- Test: `ui/editor/BarcodeSerializationTest.kt`

- [ ] **Step 1: Write serialization tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeSerializationTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BarcodeSerializationTest {

    @Test
    fun barcodeElementDefaults() {
        val el = LabelElement.BarcodeElement(id = "b1")
        assertEquals(100f, el.width)
        assertEquals(100f, el.height)
        assertEquals("", el.data)
        assertEquals(BarcodeFormat.QR_CODE, el.format)
        assertEquals(ErrorCorrection.M, el.errorCorrection)
        assertEquals(DataStandard.RAW_TEXT, el.dataStandard)
        assertEquals(emptyMap(), el.structuredData)
    }

    @Test
    fun barcodeSerializableRoundTrip() {
        val el = LabelElement.BarcodeElement(
            id = "b1", x = 10f, y = 20f, width = 150f, height = 150f,
            data = "https://example.com", format = BarcodeFormat.QR_CODE,
            errorCorrection = ErrorCorrection.H,
            dataStandard = DataStandard.URL,
            structuredData = mapOf("url" to "https://example.com"),
        )
        val serializable = el.toSerializable()
        assertEquals("barcode", serializable.type)
        assertEquals("https://example.com", serializable.barcodeData)
        assertEquals("QR_CODE", serializable.barcodeFormat)
        assertEquals("H", serializable.errorCorrection)
        assertEquals("URL", serializable.dataStandard)
        assertEquals(mapOf("url" to "https://example.com"), serializable.structuredData)

        val restored = serializable.toLabelElement()
        assertIs<LabelElement.BarcodeElement>(restored)
        assertEquals(el.data, (restored as LabelElement.BarcodeElement).data)
        assertEquals(el.format, restored.format)
        assertEquals(el.errorCorrection, restored.errorCorrection)
        assertEquals(el.dataStandard, restored.dataStandard)
        assertEquals(el.structuredData, restored.structuredData)
    }

    @Test
    fun barcodeCode128Serialization() {
        val el = LabelElement.BarcodeElement(
            id = "b2", data = "SHIPPING-123", format = BarcodeFormat.CODE_128,
            dataStandard = DataStandard.RAW_TEXT,
        )
        val s = el.toSerializable()
        assertEquals("CODE_128", s.barcodeFormat)
        val restored = s.toLabelElement() as LabelElement.BarcodeElement
        assertEquals(BarcodeFormat.CODE_128, restored.format)
        assertEquals(DataStandard.RAW_TEXT, restored.dataStandard)
    }
}
```

- [ ] **Step 2: Update LabelElement.kt**

Add `BarcodeElement` to the sealed class, add barcode fields to `SerializableLabelElement`, and update conversion functions. The full updated file:

Add after `ImageElement`:
```kotlin
    data class BarcodeElement(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 100f,
        override val height: Float = 100f,
        override val rotation: Float = 0f,
        val data: String = "",
        val format: BarcodeFormat = BarcodeFormat.QR_CODE,
        val errorCorrection: ErrorCorrection = ErrorCorrection.M,
        val dataStandard: DataStandard = DataStandard.RAW_TEXT,
        val structuredData: Map<String, String> = emptyMap(),
    ) : LabelElement()
```

Add to `SerializableLabelElement`:
```kotlin
    val barcodeData: String? = null,
    val barcodeFormat: String? = null,
    val errorCorrection: String? = null,
    val dataStandard: String? = null,
    val structuredData: Map<String, String>? = null,
```

Add to `toSerializable()`:
```kotlin
    is LabelElement.BarcodeElement -> SerializableLabelElement(
        type = "barcode", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, barcodeData = data, barcodeFormat = format.name,
        errorCorrection = errorCorrection.name, dataStandard = dataStandard.name,
        structuredData = structuredData.takeIf { it.isNotEmpty() },
    )
```

Add to `toLabelElement()`:
```kotlin
    "barcode" -> LabelElement.BarcodeElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        data = barcodeData ?: "",
        format = barcodeFormat?.let { runCatching { BarcodeFormat.valueOf(it) }.getOrNull() } ?: BarcodeFormat.QR_CODE,
        errorCorrection = errorCorrection?.let { runCatching { ErrorCorrection.valueOf(it) }.getOrNull() } ?: ErrorCorrection.M,
        dataStandard = dataStandard?.let { runCatching { DataStandard.valueOf(it) }.getOrNull() } ?: DataStandard.RAW_TEXT,
        structuredData = structuredData ?: emptyMap(),
    )
```

- [ ] **Step 3: Update UndoManager.kt deepCopy**

Add BarcodeElement branch to the `when` in `deepCopy`:
```kotlin
is LabelElement.BarcodeElement -> element.copy()
```

- [ ] **Step 4: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelElement.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/UndoManager.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeSerializationTest.kt
git commit -m "feat: BarcodeElement in LabelElement + serialization + undo support"
```

---

## Task 11: BarcodeRenderer — expect/actual

**Files:**
- Create: `ui/editor/BarcodeRenderer.kt` (expect)
- Create: `desktopMain/.../BarcodeRenderer.desktop.kt` (actual)
- Create: `androidMain/.../BarcodeRenderer.android.kt` (actual)
- Create: `iosMain/.../BarcodeRenderer.ios.kt` (actual)

- [ ] **Step 1: Create expect declaration**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

expect object BarcodeRenderer {
    fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection = ErrorCorrection.M,
    ): ImageBitmap?

    fun isFormatAvailable(format: BarcodeFormat): Boolean
}
```

- [ ] **Step 2: Create desktop actual**

Create `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.desktop.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.BarcodeFormat as ZxingFormat
import io.github.alexzhirkevich.qrose.toQrCodeBitmap
import java.awt.image.BufferedImage

private val renderCache = LinkedHashMap<String, ImageBitmap>(20, 0.75f, true)

actual object BarcodeRenderer {

    actual fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? {
        if (data.isEmpty() || width <= 0 || height <= 0) return null

        val cacheKey = "${format.name}|$data|$width|$height|${errorCorrection.name}"
        renderCache[cacheKey]?.let { return it }

        val bitmap = try {
            when (format.library) {
                BarcodeLibrary.QROSE -> renderQrose(format, data, width, height, errorCorrection)
                BarcodeLibrary.ZXING -> renderZxing(format, data, width, height, errorCorrection)
            }
        } catch (_: Exception) {
            null
        }

        if (bitmap != null) {
            if (renderCache.size >= 20) renderCache.remove(renderCache.keys.first())
            renderCache[cacheKey] = bitmap
        }
        return bitmap
    }

    actual fun isFormatAvailable(format: BarcodeFormat): Boolean = true

    private fun renderQrose(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? {
        return when (format) {
            BarcodeFormat.QR_CODE -> {
                val ecl = when (errorCorrection) {
                    ErrorCorrection.L -> io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.Low
                    ErrorCorrection.M -> io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.Medium
                    ErrorCorrection.Q -> io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.MediumHigh
                    ErrorCorrection.H -> io.github.alexzhirkevich.qrose.options.QrErrorCorrectionLevel.High
                }
                val bmp = data.toQrCodeBitmap(width, errorCorrectionLevel = ecl)
                bmp
            }
            else -> {
                // For non-QR qrose formats, fall back to ZXing on desktop
                renderZxing(format, data, width, height, errorCorrection)
            }
        }
    }

    private fun renderZxing(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? {
        val zxingFormat = when (format) {
            BarcodeFormat.QR_CODE -> ZxingFormat.QR_CODE
            BarcodeFormat.PDF_417 -> ZxingFormat.PDF_417
            BarcodeFormat.DATA_MATRIX -> ZxingFormat.DATA_MATRIX
            BarcodeFormat.AZTEC -> ZxingFormat.AZTEC
            BarcodeFormat.MAXICODE -> ZxingFormat.MAXICODE
            BarcodeFormat.RSS_EXPANDED -> ZxingFormat.RSS_EXPANDED
            BarcodeFormat.CODE_128 -> ZxingFormat.CODE_128
            BarcodeFormat.CODE_39 -> ZxingFormat.CODE_39
            BarcodeFormat.CODE_93 -> ZxingFormat.CODE_93
            BarcodeFormat.CODABAR -> ZxingFormat.CODABAR
            BarcodeFormat.ITF -> ZxingFormat.ITF
            BarcodeFormat.RSS_14 -> ZxingFormat.RSS_14
            BarcodeFormat.EAN_13 -> ZxingFormat.EAN_13
            BarcodeFormat.EAN_8 -> ZxingFormat.EAN_8
            BarcodeFormat.UPC_A -> ZxingFormat.UPC_A
            BarcodeFormat.UPC_E -> ZxingFormat.UPC_E
        }

        val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
        if (format == BarcodeFormat.QR_CODE) {
            hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = when (errorCorrection) {
                ErrorCorrection.L -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
                ErrorCorrection.M -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                ErrorCorrection.Q -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q
                ErrorCorrection.H -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            }
        }

        val matrix: BitMatrix = MultiFormatWriter().encode(data, zxingFormat, width, height, hints)
        return matrix.toComposeImageBitmap()
    }

    private fun BitMatrix.toComposeImageBitmap(): ImageBitmap {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                img.setRGB(x, y, if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return img.toComposeImageBitmap()
    }
}
```

- [ ] **Step 3: Create Android actual**

Create `shared/src/androidMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.android.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.BarcodeFormat as ZxingFormat

actual object BarcodeRenderer {
    actual fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? {
        if (data.isEmpty() || width <= 0 || height <= 0) return null
        return try {
            val zxingFormat = mapFormat(format) ?: return null
            val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
            if (format == BarcodeFormat.QR_CODE) {
                hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = when (errorCorrection) {
                    ErrorCorrection.L -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
                    ErrorCorrection.M -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                    ErrorCorrection.Q -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q
                    ErrorCorrection.H -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                }
            }
            val matrix = MultiFormatWriter().encode(data, zxingFormat, width, height, hints)
            matrix.toImageBitmap()
        } catch (_: Exception) { null }
    }

    actual fun isFormatAvailable(format: BarcodeFormat): Boolean = true

    private fun mapFormat(format: BarcodeFormat): ZxingFormat? = when (format) {
        BarcodeFormat.QR_CODE -> ZxingFormat.QR_CODE
        BarcodeFormat.PDF_417 -> ZxingFormat.PDF_417
        BarcodeFormat.DATA_MATRIX -> ZxingFormat.DATA_MATRIX
        BarcodeFormat.AZTEC -> ZxingFormat.AZTEC
        BarcodeFormat.MAXICODE -> ZxingFormat.MAXICODE
        BarcodeFormat.RSS_EXPANDED -> ZxingFormat.RSS_EXPANDED
        BarcodeFormat.CODE_128 -> ZxingFormat.CODE_128
        BarcodeFormat.CODE_39 -> ZxingFormat.CODE_39
        BarcodeFormat.CODE_93 -> ZxingFormat.CODE_93
        BarcodeFormat.CODABAR -> ZxingFormat.CODABAR
        BarcodeFormat.ITF -> ZxingFormat.ITF
        BarcodeFormat.RSS_14 -> ZxingFormat.RSS_14
        BarcodeFormat.EAN_13 -> ZxingFormat.EAN_13
        BarcodeFormat.EAN_8 -> ZxingFormat.EAN_8
        BarcodeFormat.UPC_A -> ZxingFormat.UPC_A
        BarcodeFormat.UPC_E -> ZxingFormat.UPC_E
    }

    private fun BitMatrix.toImageBitmap(): ImageBitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp.asImageBitmap()
    }
}
```

- [ ] **Step 4: Create iOS actual (stub)**

Create `shared/src/iosMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.ios.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap

actual object BarcodeRenderer {
    actual fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? = null // TODO: qrose iOS rendering

    actual fun isFormatAvailable(format: BarcodeFormat): Boolean =
        format.library == BarcodeLibrary.QROSE
}
```

- [ ] **Step 5: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.kt \
       shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.desktop.kt \
       shared/src/androidMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.android.kt \
       shared/src/iosMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeRenderer.ios.kt
git commit -m "feat: BarcodeRenderer — expect/actual with qrose + ZXing rendering"
```

---

## Task 12: EditorState — Barcode Methods

**Files:**
- Modify: `ui/editor/EditorState.kt`

- [ ] **Step 1: Add barcode CRUD methods to EditorState**

Add these methods to `EditorState`:

```kotlin
    fun addBarcodeElement(format: BarcodeFormat, data: String, dataStandard: DataStandard = DataStandard.RAW_TEXT, structuredData: Map<String, String> = emptyMap()) {
        saveUndoSnapshot()
        val size = if (format.is2D) 100f else 200f
        val height = if (format.is2D) 100f else 60f
        val el = LabelElement.BarcodeElement(
            id = newId("barcode"),
            width = size,
            height = height,
            data = data,
            format = format,
            dataStandard = dataStandard,
            structuredData = structuredData,
        )
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun setBarcodeData(id: String, data: String) {
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(data = data) else el
        }
    }

    fun setBarcodeDataDone(id: String) { saveUndoSnapshot() }

    fun setBarcodeFormat(id: String, format: BarcodeFormat) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) {
                val autoFixed = BarcodeValidator.autoFix(format, el.data)
                el.copy(format = format, data = autoFixed)
            } else el
        }
    }

    fun setBarcodeErrorCorrection(id: String, ec: ErrorCorrection) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(errorCorrection = ec) else el
        }
    }

    fun setBarcodeDataStandard(id: String, standard: DataStandard) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(dataStandard = standard, structuredData = emptyMap(), data = "") else el
        }
    }

    fun setBarcodeStructuredData(id: String, fields: Map<String, String>) {
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) {
                val encoded = DataEncoderRegistry.encode(el.dataStandard, fields)
                el.copy(structuredData = fields, data = encoded)
            } else el
        }
    }

    fun setBarcodeStructuredDataDone(id: String) { saveUndoSnapshot() }
```

- [ ] **Step 2: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorState.kt
git commit -m "feat: EditorState barcode CRUD — add, format, data, standard, structured data"
```

---

## Task 13: LabelCanvas — Draw Barcode Elements

**Files:**
- Modify: `ui/editor/LabelCanvas.kt`

- [ ] **Step 1: Add drawBarcodeElement to LabelCanvas**

Add a new private function and update `drawElement`:

In `drawElement`, add:
```kotlin
is LabelElement.BarcodeElement -> drawBarcodeElement(element, scaleFactor, textMeasurer)
```

New function:
```kotlin
private fun DrawScope.drawBarcodeElement(
    el: LabelElement.BarcodeElement,
    scaleFactor: Float,
    textMeasurer: TextMeasurer,
) {
    val screenX = el.x * scaleFactor
    val screenY = el.y * scaleFactor
    val screenW = el.width * scaleFactor
    val screenH = el.height * scaleFactor

    val bitmap = BarcodeRenderer.render(
        format = el.format,
        data = el.data,
        width = el.width.toInt().coerceAtLeast(1),
        height = el.height.toInt().coerceAtLeast(1),
        errorCorrection = el.errorCorrection,
    )

    if (bitmap != null) {
        drawImage(
            image = bitmap,
            dstOffset = androidx.compose.ui.unit.IntOffset(screenX.toInt(), screenY.toInt()),
            dstSize = IntSize(screenW.toInt(), screenH.toInt()),
        )
    } else {
        // Invalid data placeholder
        drawRect(
            Color.LightGray,
            topLeft = Offset(screenX, screenY),
            size = Size(screenW, screenH),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))),
        )
        val label = "${el.format.displayName}\n${if (el.data.isEmpty()) "No data" else "Invalid data"}"
        val layout = textMeasurer.measure(
            text = label,
            style = TextStyle(fontSize = (10f * scaleFactor).sp, color = Color.Gray),
            constraints = Constraints(maxWidth = screenW.toInt().coerceAtLeast(1)),
        )
        drawText(layout, topLeft = Offset(screenX + 4f, screenY + 4f))
    }
}
```

Also update `drawElementsForPrint` to handle BarcodeElement:
```kotlin
fun drawElementsForPrint(scope: DrawScope, elements: List<LabelElement>, textMeasurer: TextMeasurer) {
    scope.drawRect(Color.White)
    for (element in elements) {
        scope.drawElement(element, scaleFactor = 1f, textMeasurer)
    }
}
```
This already works since `drawElement` dispatches to `drawBarcodeElement`.

- [ ] **Step 2: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelCanvas.kt
git commit -m "feat: LabelCanvas draws BarcodeElement with rendered bitmap or placeholder"
```

---

## Task 14: UI — BarcodeFormatPicker Dialog

**Files:**
- Create: `ui/editor/BarcodeFormatPicker.kt`

- [ ] **Step 1: Create the picker dialog**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormatPicker.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BarcodeFormatPicker(
    onAdd: (BarcodeFormat, String, DataStandard, Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(BarcodeFormat.QR_CODE) }
    var selectedStandard by remember { mutableStateOf(DataStandard.RAW_TEXT) }
    var rawData by remember { mutableStateOf("") }
    var structuredFields by remember { mutableStateOf(mapOf<String, String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Barcode") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Format selection by category
                for ((category, formats) in BarcodeFormat.byCategory()) {
                    Text(category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    formats.forEach { format ->
                        val available = BarcodeRenderer.isFormatAvailable(format)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = {
                                    selectedFormat = format
                                    selectedStandard = DataStandard.RAW_TEXT
                                    structuredFields = emptyMap()
                                },
                                enabled = available,
                            )
                            Text(
                                text = format.displayName + if (!available) " (unavailable)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Data standard selection
                val standards = DataStandard.forFormat(selectedFormat)
                if (standards.size > 1) {
                    Text("Data Standard", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    standards.forEach { std ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selectedStandard == std,
                                onClick = {
                                    selectedStandard = std
                                    structuredFields = emptyMap()
                                    rawData = ""
                                },
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(std.displayName, style = MaterialTheme.typography.bodySmall)
                                Text(std.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Data entry
                if (selectedStandard == DataStandard.RAW_TEXT) {
                    OutlinedTextField(
                        value = rawData,
                        onValueChange = { rawData = it },
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                    )
                } else {
                    DataStandardForm(
                        standard = selectedStandard,
                        fields = structuredFields,
                        onFieldChange = { key, value ->
                            structuredFields = structuredFields + (key to value)
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val data = if (selectedStandard == DataStandard.RAW_TEXT) rawData
                else DataEncoderRegistry.encode(selectedStandard, structuredFields)
                onAdd(selectedFormat, data, selectedStandard, structuredFields)
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 2: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: Will fail because `DataStandardForm` doesn't exist yet. That's Task 15.

- [ ] **Step 3: Commit (partial — will compile after Task 15)**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeFormatPicker.kt
git commit -m "feat: BarcodeFormatPicker dialog with categorized format + standard selection"
```

---

## Task 15: UI — DataStandardForm + BarcodeProperties

**Files:**
- Create: `ui/editor/DataStandardForm.kt`
- Create: `ui/editor/BarcodeProperties.kt`

- [ ] **Step 1: Create DataStandardForm**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataStandardForm.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DataStandardForm(
    standard: DataStandard,
    fields: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldDefs = DataEncoderRegistry.fieldsFor(standard)

    Column(modifier = modifier) {
        for (field in fieldDefs) {
            val value = fields[field.key] ?: ""
            OutlinedTextField(
                value = value,
                onValueChange = { onFieldChange(field.key, it) },
                label = {
                    Text(
                        field.label + if (field.required) " *" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                placeholder = { if (field.hint.isNotEmpty()) Text(field.hint, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (field.fieldType) {
                        FieldType.NUMBER -> KeyboardType.Number
                        FieldType.EMAIL -> KeyboardType.Email
                        FieldType.PHONE -> KeyboardType.Phone
                        FieldType.URL -> KeyboardType.Uri
                        FieldType.TEXT -> KeyboardType.Text
                    },
                ),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
```

- [ ] **Step 2: Create BarcodeProperties**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeProperties.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeProperties(
    element: LabelElement.BarcodeElement,
    onFormatChange: (String, BarcodeFormat) -> Unit,
    onDataChange: (String, String) -> Unit,
    onDataChangeDone: (String) -> Unit,
    onErrorCorrectionChange: (String, ErrorCorrection) -> Unit,
    onDataStandardChange: (String, DataStandard) -> Unit,
    onStructuredDataChange: (String, Map<String, String>) -> Unit,
    onStructuredDataChangeDone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Barcode", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        // Format dropdown
        var formatExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = it }) {
            OutlinedTextField(
                value = element.format.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Format") },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                for ((category, formats) in BarcodeFormat.byCategory()) {
                    Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                    formats.forEach { fmt ->
                        val available = BarcodeRenderer.isFormatAvailable(fmt)
                        DropdownMenuItem(
                            text = { Text(fmt.displayName) },
                            onClick = { onFormatChange(element.id, fmt); formatExpanded = false },
                            enabled = available,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Data standard dropdown
        val standards = DataStandard.forFormat(element.format)
        if (standards.size > 1) {
            var stdExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = stdExpanded, onExpandedChange = { stdExpanded = it }) {
                OutlinedTextField(
                    value = element.dataStandard.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data Standard") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stdExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = stdExpanded, onDismissRequest = { stdExpanded = false }) {
                    standards.forEach { std ->
                        DropdownMenuItem(
                            text = { Text("${std.displayName} — ${std.description}") },
                            onClick = { onDataStandardChange(element.id, std); stdExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Data entry
        if (element.dataStandard == DataStandard.RAW_TEXT) {
            OutlinedTextField(
                value = element.data,
                onValueChange = { onDataChange(element.id, it) },
                label = { Text("Data") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 5,
            )
        } else {
            DataStandardForm(
                standard = element.dataStandard,
                fields = element.structuredData,
                onFieldChange = { key, value ->
                    val updated = element.structuredData + (key to value)
                    onStructuredDataChange(element.id, updated)
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        // Error correction (QR only)
        if (element.format == BarcodeFormat.QR_CODE) {
            var ecExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = ecExpanded, onExpandedChange = { ecExpanded = it }) {
                OutlinedTextField(
                    value = element.errorCorrection.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Error Correction") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ecExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = ecExpanded, onDismissRequest = { ecExpanded = false }) {
                    ErrorCorrection.entries.forEach { ec ->
                        DropdownMenuItem(
                            text = { Text(ec.displayName) },
                            onClick = { onErrorCorrectionChange(element.id, ec); ecExpanded = false },
                        )
                    }
                }
            }
        }

        // Validation status
        Spacer(Modifier.height(8.dp))
        val validation = BarcodeValidator.validate(element.format, element.data)
        if (element.data.isNotEmpty()) {
            Text(
                text = if (validation.isValid) "Valid" else validation.error ?: "Invalid data",
                style = MaterialTheme.typography.labelSmall,
                color = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/DataStandardForm.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/BarcodeProperties.kt
git commit -m "feat: DataStandardForm + BarcodeProperties composables"
```

---

## Task 16: Integration — ToolboxPanel + PropertiesPanel + EditorScreen

**Files:**
- Modify: `ui/editor/ToolboxPanel.kt`
- Modify: `ui/editor/PropertiesPanel.kt`
- Modify: `ui/editor/EditorScreen.kt`

- [ ] **Step 1: Add "+ Barcode" button to ToolboxPanel**

In `ToolboxPanel.kt`, add an `onAddBarcode` callback parameter and a button:

Add parameter: `onAddBarcode: () -> Unit,`

Add button after the "+ Text" / Image row:
```kotlin
OutlinedButton(onClick = onAddBarcode, modifier = Modifier.fillMaxWidth()) {
    Text("+ Barcode")
}
```

- [ ] **Step 2: Add BarcodeElement branch to PropertiesPanel**

In `PropertiesPanel.kt`, add `BarcodeElement` callbacks to the parameter list and a `when` branch:

Add parameters:
```kotlin
onBarcodeFormatChange: (String, BarcodeFormat) -> Unit,
onBarcodeDataChange: (String, String) -> Unit,
onBarcodeDataChangeDone: (String) -> Unit,
onBarcodeErrorCorrectionChange: (String, ErrorCorrection) -> Unit,
onBarcodeDataStandardChange: (String, DataStandard) -> Unit,
onBarcodeStructuredDataChange: (String, Map<String, String>) -> Unit,
onBarcodeStructuredDataChangeDone: (String) -> Unit,
```

Add `when` branch after `ImageElement`:
```kotlin
is LabelElement.BarcodeElement -> {
    BarcodeProperties(
        element = selectedElement,
        onFormatChange = onBarcodeFormatChange,
        onDataChange = onBarcodeDataChange,
        onDataChangeDone = onBarcodeDataChangeDone,
        onErrorCorrectionChange = onBarcodeErrorCorrectionChange,
        onDataStandardChange = onBarcodeDataStandardChange,
        onStructuredDataChange = onBarcodeStructuredDataChange,
        onStructuredDataChangeDone = onBarcodeStructuredDataChangeDone,
    )
}
```

- [ ] **Step 3: Wire everything in EditorScreen**

In `EditorScreen.kt`:

Add state for barcode picker dialog:
```kotlin
var showBarcodePicker by remember { mutableStateOf(false) }
```

Wire ToolboxPanel's `onAddBarcode`:
```kotlin
onAddBarcode = { showBarcodePicker = true },
```

Wire PropertiesPanel's barcode callbacks:
```kotlin
onBarcodeFormatChange = { id, fmt -> state.setBarcodeFormat(id, fmt) },
onBarcodeDataChange = { id, data -> state.setBarcodeData(id, data) },
onBarcodeDataChangeDone = { id -> state.setBarcodeDataDone(id) },
onBarcodeErrorCorrectionChange = { id, ec -> state.setBarcodeErrorCorrection(id, ec) },
onBarcodeDataStandardChange = { id, std -> state.setBarcodeDataStandard(id, std) },
onBarcodeStructuredDataChange = { id, fields -> state.setBarcodeStructuredData(id, fields) },
onBarcodeStructuredDataChangeDone = { id -> state.setBarcodeStructuredDataDone(id) },
```

Add barcode picker dialog:
```kotlin
if (showBarcodePicker) {
    BarcodeFormatPicker(
        onAdd = { format, data, standard, structuredData ->
            state.addBarcodeElement(format, data, standard, structuredData)
            showBarcodePicker = false
        },
        onDismiss = { showBarcodePicker = false },
    )
}
```

- [ ] **Step 4: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All PASS.

- [ ] **Step 5: Run the desktop app**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :desktopApp:run`
Expected: App launches. "+ Barcode" button visible in left panel. Clicking opens the format picker. Adding a QR code shows it on the canvas.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ToolboxPanel.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PropertiesPanel.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt
git commit -m "feat: barcode integration — ToolboxPanel, PropertiesPanel, EditorScreen wiring"
```

---

## Task 17: Version Bump + TODO Update

**Files:**
- Modify: `README.md`
- Modify: `TODO.md`

- [ ] **Step 1: Update README and TODO**

Add barcode features to README. Mark QR/barcode item as complete in TODO. Add data standards to feature list.

- [ ] **Step 2: Run all tests one final time**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All PASS.

- [ ] **Step 3: Commit**

```bash
git add README.md TODO.md
git commit -m "chore: bump to v0.4.0 — barcode generation complete"
```

---

## Dependency Graph

```
Task 1 (build config) ──────────────────────────────────────────────┐
                                                                    │
Task 2 (BarcodeFormat) ─────┬──→ Task 4 (BarcodeValidator)          │
                            ├──→ Task 10 (LabelElement update)      │
                            ├──→ Task 11 (BarcodeRenderer)          │
                            └──→ Task 14 (BarcodeFormatPicker)      │
                                                                    │
Task 3 (DataStandard) ──────┬──→ Task 5 (vCard encoder)            │
                            ├──→ Task 6 (WiFi+MeCard+URL)          │
                            ├──→ Task 7 (SMS+Email+Phone+Geo)      │
                            ├──→ Task 8 (AAMVA+GS1+HIBC)           │
                            └──→ Task 9 (register all)              │
                                      │                             │
Task 10 ─────────────────────────────→├──→ Task 12 (EditorState)    │
Task 11 ─────────────────────────────→├──→ Task 13 (LabelCanvas)    │
Task 14 + Task 15 ──────────────────→├──→ Task 16 (Integration)    │
                                      │                             │
                                      └──→ Task 17 (Version bump)  │
```

**Parallel groups for /burn:**
- **Group A (no deps):** Tasks 1, 2, 3
- **Group B (after 2+3):** Tasks 4, 5, 6, 7, 8 — all independent
- **Group C (after B):** Tasks 9, 10, 11
- **Group D (after C):** Tasks 12, 13, 14, 15
- **Group E (after D):** Task 16
- **Group F (after E):** Task 17
