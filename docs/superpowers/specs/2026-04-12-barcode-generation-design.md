# BluePaper v0.4.0 — QR/Barcode Generation Design Spec

**Date:** 2026-04-12
**Version:** 0.3.0 → 0.4.0
**Approach:** New BarcodeElement type + dual library strategy (qrose + ZXing)

## Overview

Add QR code and barcode generation as a first-class element type in the BluePaper label editor. Support 16 barcode formats across 1D linear, 2D matrix, and retail categories using qrose (Compose Multiplatform native) for common formats and ZXing (JVM) for 2D heavyweights.

## 1. BarcodeElement

### New LabelElement Variant

Third sealed class variant alongside TextElement and ImageElement:

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
) : LabelElement()
```

All fields are `val` — mutations via `copy()`, consistent with existing elements.

### BarcodeFormat Enum

```kotlin
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
}

enum class BarcodeLibrary { QROSE, ZXING }

enum class ErrorCorrection(val displayName: String) {
    L("Low (7%)"),
    M("Medium (15%)"),
    Q("Quartile (25%)"),
    H("High (30%)"),
}
```

16 formats with clear library ownership. Categories for UI grouping.

## 2. BarcodeRenderer

### Platform-Aware Rendering

```kotlin
// commonMain — interface
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

**desktopMain / androidMain actual:**
- qrose formats: use `QrCodePainter` / `BarcodePainter` → render into `ImageBitmap`
- ZXing formats: use `MultiFormatWriter` → `BitMatrix` → `ImageBitmap`
- Returns `null` for invalid data

**iosMain actual:**
- qrose formats: use qrose (works on iOS via KMP)
- ZXing formats: return `null` (`isFormatAvailable()` returns `false`)

### Render Caching

Cache rendered barcodes by key `(format, data, width, height, errorCorrection)`. Use a simple `MutableMap` with LRU eviction (max 20 entries). Only re-render when inputs change — barcode generation can be CPU-heavy for large Data Matrix / PDF417.

### BitMatrix → ImageBitmap Conversion

ZXing returns a `BitMatrix`. Convert to `ImageBitmap` via:

```kotlin
fun BitMatrix.toImageBitmap(): ImageBitmap {
    val width = this.width
    val height = this.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    val bitmap = ImageBitmap(width, height)
    bitmap.writePixels(pixels, width)
    return bitmap
}
```

## 3. BarcodeValidator

### Validation + Auto-Fix

```kotlin
object BarcodeValidator {
    fun validate(format: BarcodeFormat, data: String): ValidationResult
    fun autoFix(format: BarcodeFormat, data: String): String
}

data class ValidationResult(
    val isValid: Boolean,
    val fixedData: String? = null,
    val error: String? = null,
)
```

### Auto-Fix Rules

| Format | Rule |
|--------|------|
| EAN-13 | Pad to 12 digits with leading zeros, compute 13th check digit |
| EAN-8 | Pad to 7 digits with leading zeros, compute check digit |
| UPC-A | Pad to 11 digits with leading zeros, compute check digit |
| UPC-E | Pad to 7 digits with leading zeros, compute check digit |
| Code 39 | Uppercase input, strip invalid chars |
| ITF | Pad to even length with leading zero if odd |
| Codabar | Uppercase, ensure start/stop chars (A-D) |
| QR Code | Accept anything |
| PDF417 | Accept anything |
| Data Matrix | Accept anything |
| Aztec | Accept anything |
| MaxiCode | Accept anything |
| Code 128 | Accept any ASCII |
| Code 93 | Accept any ASCII |
| RSS-14 | Validate 14-digit GTIN |
| RSS Expanded | Validate GS1 AI format |

### Check Digit Computation

EAN/UPC check digits use the standard modulo-10 algorithm:
1. Sum odd-position digits × 1, even-position digits × 3
2. Check digit = (10 - (sum % 10)) % 10

## 4. UI Integration

### Left Panel — Add Barcode Button

The `ToolboxPanel` gets a third add button: "+ Barcode". Clicking opens the `BarcodeFormatPicker` dialog.

### BarcodeFormatPicker Dialog

Categorized grid layout:

```
┌─ Add Barcode ──────────────────────────────┐
│                                            │
│  2D Codes          Linear Codes   Retail   │
│  ─────────         ────────────   ──────   │
│  ○ QR Code         ○ Code 128    ○ EAN-13  │
│  ○ PDF417          ○ Code 39     ○ EAN-8   │
│  ○ Data Matrix     ○ Code 93     ○ UPC-A   │
│  ○ Aztec           ○ Codabar     ○ UPC-E   │
│  ○ MaxiCode        ○ ITF                   │
│  ○ RSS Expanded    ○ RSS-14                │
│                                            │
│  Data: [________________________]          │
│                                            │
│            [Cancel]  [Add]                 │
└────────────────────────────────────────────┘
```

Formats unavailable on current platform shown grayed out with "(desktop/Android only)" tooltip.

### Properties Panel — BarcodeElement

When a `BarcodeElement` is selected:

**Common fields:** X, Y, W, H, Rotation (same as other elements)

**Barcode-specific:**
- **Format:** Categorized dropdown (same groups as picker)
- **Data:** Text input field
- **Error Correction:** Dropdown (L/M/Q/H) — only visible when format is QR_CODE
- **Validation:** Green checkmark for valid, red warning with error message for invalid

Changing the format revalidates the data and re-renders. If the current data is invalid for the new format, auto-fix is attempted.

### Canvas Rendering

`LabelCanvas.drawBarcodeElement()`:
1. Call `BarcodeRenderer.render()` with element's format/data/size/errorCorrection
2. If result is non-null: draw the `ImageBitmap` at the element's position
3. If result is null (invalid data): draw a dashed rectangle with format name + "Invalid data" text

Barcode rendering is cached — the `remember` key is `(format, data, width, height, errorCorrection)`.

### Print Pipeline

`drawElementsForPrint()` renders barcodes at 1:1 label pixel scale. The monochrome encoder already handles 1-bit conversion — barcodes are already black-and-white, so they pass through cleanly.

## 5. Serialization

### SerializableLabelElement Changes

Add 3 new nullable fields:

```kotlin
@Serializable
data class SerializableLabelElement(
    // ... existing fields ...
    val barcodeData: String? = null,
    val barcodeFormat: String? = null,
    val errorCorrection: String? = null,
)
```

### Conversion Functions

```kotlin
// In toSerializable()
is LabelElement.BarcodeElement -> SerializableLabelElement(
    type = "barcode", id = id, x = x, y = y, width = width, height = height,
    rotation = rotation,
    barcodeData = data,
    barcodeFormat = format.name,
    errorCorrection = errorCorrection.name,
)

// In toLabelElement()
"barcode" -> LabelElement.BarcodeElement(
    id = id, x = x, y = y, width = width, height = height, rotation = rotation,
    data = barcodeData ?: "",
    format = barcodeFormat?.let { BarcodeFormat.valueOf(it) } ?: BarcodeFormat.QR_CODE,
    errorCorrection = errorCorrection?.let { ErrorCorrection.valueOf(it) } ?: ErrorCorrection.M,
)
```

## 6. Dependencies

### shared/build.gradle.kts

```kotlin
commonMain {
    dependencies {
        // existing...
        implementation("io.github.alexzhirkevich:qrose:1.1.0")
    }
}

val desktopMain by getting {
    dependencies {
        // existing...
        implementation("com.google.zxing:core:3.5.3")
    }
}

val androidMain by getting {
    dependencies {
        implementation("com.google.zxing:core:3.5.3")
    }
}
```

qrose is KMP-native (all platforms). ZXing is JVM-only (desktop + Android).

## 7. File Changes Summary

### New Files
- `ui/editor/BarcodeFormat.kt` — BarcodeFormat enum, ErrorCorrection enum, BarcodeLibrary enum
- `ui/editor/BarcodeRenderer.kt` — expect declaration
- `ui/editor/BarcodeRenderer.desktop.kt` — actual (qrose + ZXing)
- `ui/editor/BarcodeRenderer.android.kt` — actual (qrose + ZXing)
- `ui/editor/BarcodeRenderer.ios.kt` — actual (qrose only)
- `ui/editor/BarcodeValidator.kt` — validation + auto-fix logic
- `ui/editor/BarcodeFormatPicker.kt` — categorized format picker dialog
- `ui/editor/BarcodeProperties.kt` — barcode-specific properties panel section

### Modified Files
- `ui/editor/LabelElement.kt` — add BarcodeElement variant
- `ui/editor/LabelElement.kt` — add barcode fields to SerializableLabelElement
- `ui/editor/EditorState.kt` — add addBarcodeElement(), barcode mutation methods
- `ui/editor/LabelCanvas.kt` — add drawBarcodeElement()
- `ui/editor/ToolboxPanel.kt` — add "+ Barcode" button
- `ui/editor/PropertiesPanel.kt` — add BarcodeElement branch
- `ui/editor/UndoManager.kt` — add BarcodeElement to deepCopy when clause
- `shared/build.gradle.kts` — add qrose + ZXing dependencies

### New Test Files
- `ui/editor/BarcodeValidatorTest.kt` — validation rules, auto-fix, check digits
- `ui/editor/BarcodeFormatTest.kt` — enum properties, category grouping
- `ui/editor/BarcodeSerializationTest.kt` — roundtrip serialization

## 8. Testing Strategy

### Unit Tests
- BarcodeValidator: check digit computation for all EAN/UPC formats, auto-fix padding, Code 39 uppercasing, ITF even-length padding
- BarcodeFormat: enum properties (category, is2D, library), grouping
- Serialization: BarcodeElement roundtrip through SerializableLabelElement

### Integration Tests
- BarcodeRenderer: render QR code → verify non-null ImageBitmap with expected dimensions
- BarcodeRenderer: render with invalid data → verify null return
- Full pipeline: BarcodeElement → LabelCanvas → MonochromeEncoder → verify 1-bit output

## 9. Out of Scope

- Barcode scanning / reading (this is generation only)
- Custom barcode colors (barcodes must be black-on-white for thermal printing)
- Barcode-specific templates (can be added to template system later)
- Logo embedding in QR codes (qrose supports this but adds complexity)
