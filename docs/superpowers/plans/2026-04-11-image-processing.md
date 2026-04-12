# BluePaper Image Processing — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the monochrome image encoding pipeline that converts ARGB pixel data into packed 1-bit bitmap rows suitable for Niimbot printer packets — the bridge between "a label design on screen" and "bytes the printer can receive."

**Architecture:** `MonochromeEncoder` in `commonMain` is a pure-Kotlin algorithm class with zero platform dependencies. It takes an IntArray of ARGB pixels + dimensions, converts to grayscale, inverts, thresholds to 1-bit, applies offsets, packs bits into bytes (MSB first, 8 pixels/byte), and returns a `List<ByteArray>` — one entry per scan line. This feeds directly into `PrinterClient.print()`. Compose `ImageBitmap.readPixels()` provides the ARGB input cross-platform. A `LabelRenderer` composable uses `CanvasDrawScope` to render text labels onto an `ImageBitmap`, then feeds pixels through `MonochromeEncoder`.

**Tech Stack:** Pure Kotlin (MonochromeEncoder), Compose Multiplatform `ImageBitmap` + `CanvasDrawScope` + `TextMeasurer` (LabelRenderer)

**Depends on:** Plan 1 (CommandBuilder.imageRow) and Plan 2 (PrinterClient.print)

---

## File Structure

```
shared/src/
  commonMain/kotlin/com/avicennasis/bluepaper/
    image/
      MonochromeEncoder.kt          # ARGB pixels → packed 1-bit rows (pure Kotlin)
      LabelRenderer.kt              # Compose: render text label → ImageBitmap → pixel extraction
  commonTest/kotlin/com/avicennasis/bluepaper/
    image/
      MonochromeEncoderTest.kt      # Full TDD: grayscale, invert, threshold, bit-pack, offsets
```

---

### Task 1: MonochromeEncoder — Core Algorithm (TDD)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoder.kt`
- Test: `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoderTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoderTest.kt
package com.avicennasis.bluepaper.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals

class MonochromeEncoderTest {

    // Helper: create ARGB pixel from gray value (opaque)
    private fun gray(value: Int): Int = (0xFF shl 24) or (value shl 16) or (value shl 8) or value

    // Helper: create ARGB pixel from RGB
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun allBlackPixelsProduceAllOnes() {
        // 8 black pixels (gray=0) → after invert+threshold → all 1-bits → 0xFF
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertEquals(1, rows.size)
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[0])
    }

    @Test
    fun allWhitePixelsProduceAllZeros() {
        // 8 white pixels (gray=255) → after invert+threshold → all 0-bits → 0x00
        val pixels = IntArray(8) { gray(255) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertEquals(1, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0])
    }

    @Test
    fun alternatingBlackWhite() {
        // Pattern: black(0), white(255), black(0), white(255), black(0), white(255), black(0), white(255)
        // After invert: 255, 0, 255, 0, 255, 0, 255, 0
        // After threshold: 1, 0, 1, 0, 1, 0, 1, 0
        // MSB first: 10101010 = 0xAA
        val pixels = IntArray(8) { i -> if (i % 2 == 0) gray(0) else gray(255) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        assertContentEquals(byteArrayOf(0xAA.toByte()), rows[0])
    }

    @Test
    fun widthNotMultipleOf8PadsWithZeros() {
        // 10 black pixels → ceil(10/8) = 2 bytes
        // First byte: 8 black → 0xFF
        // Second byte: 2 black + 6 padding zeros → 11000000 = 0xC0
        val pixels = IntArray(10) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 10, height = 1)
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(0xFF.toByte(), rows[0][0])
        assertEquals(0xC0.toByte(), rows[0][1])
    }

    @Test
    fun multipleRows() {
        // 2 rows of 8 pixels each
        // Row 0: all black → 0xFF
        // Row 1: all white → 0x00
        val pixels = IntArray(16)
        for (i in 0 until 8) pixels[i] = gray(0)     // row 0: black
        for (i in 8 until 16) pixels[i] = gray(255)   // row 1: white
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 2)
        assertEquals(2, rows.size)
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[0])
        assertContentEquals(byteArrayOf(0x00), rows[1])
    }

    @Test
    fun grayscaleConversionFromColor() {
        // Pure red (255,0,0): gray = 0.299*255 + 0.587*0 + 0.114*0 = 76.245 → 76
        // Invert: 255-76 = 179, threshold >= 128 → 1 (ink)
        // Pure blue (0,0,255): gray = 0.299*0 + 0.587*0 + 0.114*255 = 29.07 → 29
        // Invert: 255-29 = 226, threshold >= 128 → 1 (ink)
        // Pure green (0,255,0): gray = 0.299*0 + 0.587*255 + 0.114*0 = 149.685 → 149
        // Invert: 255-149 = 106, threshold >= 128 → 0 (no ink)
        // White (255,255,255): gray = 255, invert: 0, threshold → 0
        // 4 pixels in a row: red(1), blue(1), green(0), white(0) → 1100_0000 = 0xC0 (padded to 8 bits)
        val pixels = intArrayOf(rgb(255, 0, 0), rgb(0, 0, 255), rgb(0, 255, 0), gray(255))
        val rows = MonochromeEncoder.encode(pixels, width = 4, height = 1)
        assertEquals(1, rows.size)
        assertEquals(0xC0.toByte(), rows[0][0])
    }

    @Test
    fun threshold128IsExact() {
        // Gray 127 → invert: 128 → threshold: >= 128 → 1 (ink)
        // Gray 128 → invert: 127 → threshold: < 128 → 0 (no ink)
        val pixels = intArrayOf(gray(127), gray(128), gray(127), gray(128), gray(127), gray(128), gray(127), gray(128))
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1)
        // 1, 0, 1, 0, 1, 0, 1, 0 → 0xAA
        assertContentEquals(byteArrayOf(0xAA.toByte()), rows[0])
    }

    @Test
    fun bytesPerRowCalculation() {
        assertEquals(1, MonochromeEncoder.bytesPerRow(1))
        assertEquals(1, MonochromeEncoder.bytesPerRow(8))
        assertEquals(2, MonochromeEncoder.bytesPerRow(9))
        assertEquals(2, MonochromeEncoder.bytesPerRow(16))
        assertEquals(30, MonochromeEncoder.bytesPerRow(240))
        assertEquals(48, MonochromeEncoder.bytesPerRow(384))
    }

    @Test
    fun emptyImageReturnsEmptyList() {
        val rows = MonochromeEncoder.encode(IntArray(0), width = 0, height = 0)
        assertEquals(0, rows.size)
    }

    @Test
    fun singlePixelBlack() {
        val pixels = intArrayOf(gray(0))
        val rows = MonochromeEncoder.encode(pixels, width = 1, height = 1)
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size)
        // 1 bit set + 7 padding → 10000000 = 0x80
        assertEquals(0x80.toByte(), rows[0][0])
    }

    @Test
    fun sixteenPixelWidth() {
        // 16 black pixels = 2 full bytes of 0xFF
        val pixels = IntArray(16) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 16, height = 1)
        assertEquals(2, rows[0].size)
        assertEquals(0xFF.toByte(), rows[0][0])
        assertEquals(0xFF.toByte(), rows[0][1])
    }

    @Test
    fun positiveHorizontalOffset() {
        // 8 black pixels, horizontal offset +4
        // Adds 4 white pixels on the left → total width 12
        // Byte 0: 0000_1111 = 0x0F (4 white offset + 4 black)
        // Byte 1: 1111_0000 = 0xF0 (4 black + 4 padding)
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, horizontalOffset = 4)
        assertEquals(1, rows.size)
        assertEquals(2, rows[0].size) // ceil(12/8) = 2
        assertEquals(0x0F.toByte(), rows[0][0])
        assertEquals(0xF0.toByte(), rows[0][1])
    }

    @Test
    fun negativeHorizontalOffset() {
        // 8 black pixels, horizontal offset -2
        // Crops 2 pixels from left → 6 pixels remain
        // 111111_00 = 0xFC
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, horizontalOffset = -2)
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].size) // ceil(6/8) = 1
        assertEquals(0xFC.toByte(), rows[0][0])
    }

    @Test
    fun positiveVerticalOffset() {
        // 1 row of 8 black pixels, vertical offset +2
        // Adds 2 blank rows at top → 3 total rows
        val pixels = IntArray(8) { gray(0) }
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 1, verticalOffset = 2)
        assertEquals(3, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0]) // blank
        assertContentEquals(byteArrayOf(0x00), rows[1]) // blank
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[2]) // data
    }

    @Test
    fun negativeVerticalOffset() {
        // 3 rows of 8 pixels, vertical offset -1
        // Crops 1 row from top → 2 rows remain
        val pixels = IntArray(24)
        for (i in 0 until 8) pixels[i] = gray(0)       // row 0: black (cropped)
        for (i in 8 until 16) pixels[i] = gray(255)     // row 1: white
        for (i in 16 until 24) pixels[i] = gray(0)      // row 2: black
        val rows = MonochromeEncoder.encode(pixels, width = 8, height = 3, verticalOffset = -1)
        assertEquals(2, rows.size)
        assertContentEquals(byteArrayOf(0x00), rows[0])           // was row 1 (white)
        assertContentEquals(byteArrayOf(0xFF.toByte()), rows[1])  // was row 2 (black)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest`
Expected: FAIL — `MonochromeEncoder` does not exist

- [ ] **Step 3: Implement MonochromeEncoder**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoder.kt
package com.avicennasis.bluepaper.image

import kotlin.math.ceil

/**
 * Converts ARGB pixel data to packed 1-bit monochrome bitmap rows for Niimbot printers.
 *
 * Pipeline: ARGB → grayscale → invert → threshold at 128 → pack 8 pixels per byte (MSB first).
 *
 * The inversion step is critical: dark pixels (low grayscale) become 1-bits (ink printed),
 * white pixels (high grayscale) become 0-bits (no ink). This matches the printer's protocol
 * where 1 = deposit ink, 0 = skip.
 */
object MonochromeEncoder {

    /** Number of packed bytes needed per row for the given pixel width. */
    fun bytesPerRow(width: Int): Int = ceil(width.toDouble() / 8.0).toInt()

    /**
     * Encode ARGB pixels to packed 1-bit monochrome rows.
     *
     * @param pixels ARGB pixel data, row-major (length = width * height).
     *   Each Int is packed ARGB: (A shl 24) or (R shl 16) or (G shl 8) or B.
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @param horizontalOffset Positive = add blank pixels on left, negative = crop from left
     * @param verticalOffset Positive = add blank rows on top, negative = crop from top
     * @return List of ByteArrays, one per output row. Each ByteArray is [bytesPerRow] bytes
     *   of packed 1-bit data, MSB first, zero-padded if width is not a multiple of 8.
     */
    fun encode(
        pixels: IntArray,
        width: Int,
        height: Int,
        horizontalOffset: Int = 0,
        verticalOffset: Int = 0,
    ): List<ByteArray> {
        if (width <= 0 || height <= 0) return emptyList()

        // Apply vertical offset
        val srcStartRow: Int
        val blankRowsTop: Int
        val effectiveHeight: Int

        if (verticalOffset >= 0) {
            srcStartRow = 0
            blankRowsTop = verticalOffset
            effectiveHeight = height + verticalOffset
        } else {
            srcStartRow = -verticalOffset
            blankRowsTop = 0
            effectiveHeight = height + verticalOffset // smaller
        }

        if (effectiveHeight <= 0) return emptyList()

        // Apply horizontal offset
        val srcStartCol: Int
        val blankColsLeft: Int
        val effectiveWidth: Int

        if (horizontalOffset >= 0) {
            srcStartCol = 0
            blankColsLeft = horizontalOffset
            effectiveWidth = width + horizontalOffset
        } else {
            srcStartCol = -horizontalOffset
            blankColsLeft = 0
            effectiveWidth = width + horizontalOffset // smaller
        }

        if (effectiveWidth <= 0) return emptyList()

        val bpr = bytesPerRow(effectiveWidth)
        val rows = mutableListOf<ByteArray>()

        for (outY in 0 until effectiveHeight) {
            val packed = ByteArray(bpr)

            // Blank rows from vertical offset
            if (outY < blankRowsTop) {
                rows.add(packed) // all zeros = white
                continue
            }

            val srcY = srcStartRow + (outY - blankRowsTop)
            if (srcY >= height) {
                rows.add(packed) // past source data
                continue
            }

            for (outX in 0 until effectiveWidth) {
                // Determine source pixel or blank
                val bit: Int
                if (outX < blankColsLeft) {
                    bit = 0 // blank offset pixel (white = no ink)
                } else {
                    val srcX = srcStartCol + (outX - blankColsLeft)
                    if (srcX >= width) {
                        bit = 0 // past source data
                    } else {
                        val argb = pixels[srcY * width + srcX]
                        val r = (argb shr 16) and 0xFF
                        val g = (argb shr 8) and 0xFF
                        val b = argb and 0xFF

                        // Grayscale: ITU-R BT.601 luma
                        val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                        // Invert + threshold: dark pixels become 1 (ink)
                        val inverted = 255 - gray
                        bit = if (inverted >= 128) 1 else 0
                    }
                }

                if (bit == 1) {
                    val byteIndex = outX / 8
                    val bitIndex = 7 - (outX % 8) // MSB first
                    packed[byteIndex] = (packed[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }

            rows.add(packed)
        }

        return rows
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest`
Expected: PASS — 93 existing + 15 new = 108 tests

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoder.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/image/MonochromeEncoderTest.kt
git commit -m "feat: MonochromeEncoder — ARGB to 1-bit monochrome with offsets, full test coverage"
```

---

### Task 2: LabelRenderer — Compose Canvas to Monochrome Pipeline

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/LabelRenderer.kt`

This class bridges Compose UI rendering to the printer. It creates an `ImageBitmap`, draws label content using `CanvasDrawScope`, extracts pixels via `readPixels()`, and feeds them through `MonochromeEncoder`.

- [ ] **Step 1: Create LabelRenderer**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/LabelRenderer.kt
package com.avicennasis.bluepaper.image

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

/**
 * Renders label content to monochrome bitmap rows for printing.
 *
 * Usage:
 * ```
 * val rows = LabelRenderer.render(width = 240, height = 100) { drawScope ->
 *     drawScope.drawRect(Color.White) // background
 *     drawScope.drawText(textMeasurer, "Hello", ...)
 * }
 * // rows is List<ByteArray> ready for PrinterClient.print()
 * ```
 */
object LabelRenderer {

    /**
     * Render label content and encode to monochrome bitmap rows.
     *
     * @param width Label width in pixels
     * @param height Label height in pixels
     * @param horizontalOffset Pixel offset applied after rendering
     * @param verticalOffset Pixel offset applied after rendering
     * @param draw Lambda that draws the label content onto a DrawScope.
     *   The DrawScope size matches [width] x [height].
     *   Draw with Color.Black for ink, Color.White for no ink.
     * @return List of packed 1-bit monochrome row data, ready for PrinterClient.print()
     */
    fun render(
        width: Int,
        height: Int,
        horizontalOffset: Int = 0,
        verticalOffset: Int = 0,
        draw: (DrawScope) -> Unit,
    ): List<ByteArray> {
        // Create bitmap and draw
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val scope = CanvasDrawScope()
        scope.draw(
            density = Density(1f),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
        ) {
            draw(this)
        }

        // Extract ARGB pixels
        val pixels = IntArray(width * height)
        bitmap.readPixels(pixels, 0, 0, width, height)

        // Encode to monochrome
        return MonochromeEncoder.encode(pixels, width, height, horizontalOffset, verticalOffset)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/LabelRenderer.kt
git commit -m "feat: LabelRenderer — Compose Canvas to monochrome pipeline via ImageBitmap.readPixels"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] ARGB → grayscale conversion (ITU-R BT.601 luma: 0.299R + 0.587G + 0.114B)
- [x] Inversion (dark pixels → high values → 1-bits)
- [x] Threshold at 128
- [x] Bit packing (MSB first, 8 pixels/byte, zero-padded)
- [x] Horizontal offset (positive = pad left, negative = crop left)
- [x] Vertical offset (positive = pad top, negative = crop top)
- [x] bytesPerRow utility
- [x] Edge cases (empty image, single pixel, non-byte-aligned width)
- [x] LabelRenderer — Compose Canvas → ImageBitmap → readPixels → MonochromeEncoder
- [x] Full test coverage (15 tests for MonochromeEncoder)

**2. Placeholder scan:** No TBDs, TODOs found.

**3. Type consistency:**
- `MonochromeEncoder.encode(pixels: IntArray, width: Int, height: Int, ...): List<ByteArray>` — consistent with PrinterClient.print(imageRows: List<ByteArray>)
- `LabelRenderer.render()` returns `List<ByteArray>` — same type
