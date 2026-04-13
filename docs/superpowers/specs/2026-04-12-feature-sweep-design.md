# BluePaper v0.6.0 — Feature Sweep Design Spec

**Date:** 2026-04-12
**Version:** 0.5.0 → 0.6.0
**Scope:** 6 backlog features — image serialization, custom templates, z-ordering, bold/italic, width clamping, responsive layout

## Overview

Sweep all remaining feature backlog items into a single release. Each feature is independent and can be implemented in parallel.

## 1. Image Serialization in .bpl

### Problem
ImageElement's bitmap is lost on save/load — only transforms are persisted.

### Solution
Downscale bitmap to fit the current label pixel dimensions, encode as PNG, base64-encode into the .bpl JSON.

### New SerializableLabelElement field
```kotlin
val imageData: String? = null,  // base64-encoded PNG bytes
```

### Downscale Logic
Scale the bitmap so its largest dimension fits within `max(labelWidthPx, labelHeightPx)` of the currently selected label size. If already smaller, no scaling.

### Platform-Specific PNG Encoding (expect/actual)
```kotlin
// commonMain
expect object ImageEncoder {
    fun encode(bitmap: ImageBitmap, maxDimension: Int): String  // returns base64
    fun decode(base64: String): ImageBitmap?
}
```

**Desktop actual:** `ImageBitmap` → AWT `BufferedImage` → scale with `getScaledInstance` → `ImageIO.write(png)` → `Base64.getEncoder().encodeToString()`

**Android actual:** `ImageBitmap` → `android.graphics.Bitmap` → `Bitmap.createScaledBitmap` → `compress(PNG)` → `Base64.encodeToString()`

**iOS actual:** Return empty string / null (stubs)

### Integration
- `toSerializable()`: if `ImageElement` has a bitmap, call `ImageEncoder.encode(bitmap, maxDimension)` → set `imageData`
- `toLabelElement()`: if `imageData` is non-null, call `ImageEncoder.decode(imageData)` → set `bitmap`
- `EditorState.toDesign()` passes the current label's max pixel dimension for downscaling

## 2. Custom User-Saved Templates

### Save Flow
1. User clicks "Save as Template" in toolbar
2. Dialog: enter name + description
3. Current elements converted to proportional coordinates (x/labelWidth, y/labelHeight)
4. Saved as `LabelTemplate` JSON to platform storage

### Storage (expect/actual)
```kotlin
expect object TemplateStorage {
    fun save(template: LabelTemplate)
    fun loadAll(): List<LabelTemplate>
    fun delete(name: String)
}
```

**Desktop:** `~/.bluepaper/templates/{slug}.json`
**Android:** `context.filesDir/templates/{slug}.json`
**iOS:** stub (empty list)

### Proportional Conversion
```kotlin
fun EditorState.toTemplate(name: String, description: String): LabelTemplate {
    val w = selectedLabelSize.value.widthPx
    val h = selectedLabelSize.value.heightPx
    val templateElements = elements.value.map { el ->
        TemplateElement(
            type = when (el) { is TextElement -> "text"; is ImageElement -> "image"; is BarcodeElement -> "barcode" },
            xFraction = el.x / w,
            yFraction = el.y / h,
            widthFraction = el.width / w,
            heightFraction = el.height / h,
            text = (el as? LabelElement.TextElement)?.text,
            fontSize = (el as? LabelElement.TextElement)?.fontSize,
            fontFamily = (el as? LabelElement.TextElement)?.fontFamily,
        )
    }
    return LabelTemplate(name, description, templateElements)
}
```

### UI Changes
- **TopToolbar:** "Save Template" button (between Templates and Grid toggle)
- **SaveTemplateDialog:** Name + description text fields, Save/Cancel buttons
- **TemplatePickerDialog:** New "Saved" section at bottom showing user templates with delete buttons
- **TemplateStorage.init(context)** called from MainActivity on Android (like ThemePreferences)

## 3. Z-Ordering

### EditorState Methods
```kotlin
fun bringToFront(id: String) {
    saveUndoSnapshot()
    val el = _elements.value.find { it.id == id } ?: return
    _elements.value = _elements.value.filter { it.id != id } + el
}

fun sendToBack(id: String) {
    saveUndoSnapshot()
    val el = _elements.value.find { it.id == id } ?: return
    _elements.value = listOf(el) + _elements.value.filter { it.id != id }
}

fun moveUp(id: String) {
    saveUndoSnapshot()
    val list = _elements.value.toMutableList()
    val idx = list.indexOfFirst { it.id == id }
    if (idx < 0 || idx >= list.lastIndex) return
    list[idx] = list[idx + 1].also { list[idx + 1] = list[idx] }
    _elements.value = list
}

fun moveDown(id: String) {
    saveUndoSnapshot()
    val list = _elements.value.toMutableList()
    val idx = list.indexOfFirst { it.id == id }
    if (idx <= 0) return
    list[idx] = list[idx - 1].also { list[idx - 1] = list[idx] }
    _elements.value = list
}
```

### UI
Properties panel (common section, below position/size): Row of 4 small buttons:
- "↑" (move up) — disabled if already at top
- "↓" (move down) — disabled if already at bottom
- "⤒" (bring to front) — disabled if already at top
- "⤓" (send to back) — disabled if already at bottom

## 4. Bold/Italic Font Weights

### TextElement Changes
```kotlin
data class TextElement(
    // ...existing fields...
    val fontWeight: Int = 400,      // 400=normal, 700=bold
    val fontStyle: String = "normal",  // "normal" or "italic"
)
```

### SerializableLabelElement Changes
```kotlin
val fontWeight: Int? = null,
val fontStyle: String? = null,
```

### FontRegistry Extension Point
```kotlin
object FontRegistry {
    // New method — checks for registered real variant, falls back to synthetic
    fun get(key: String, weight: Int = 400, style: String = "normal"): FontFamily

    // Hook for future font addon packages
    fun registerVariant(key: String, weight: Int, style: String, family: FontFamily)
}
```

When no real variant is registered, the returned `FontFamily` is `FontFamily.Default` (or the registered regular font) — Compose applies synthetic weight/style via `TextStyle(fontWeight, fontStyle)`.

### Canvas Rendering
```kotlin
val textLayout = textMeasurer.measure(
    text = el.text,
    style = TextStyle(
        fontSize = ...,
        color = Color.Black,
        fontFamily = FontRegistry.get(el.fontFamily, el.fontWeight, el.fontStyle),
        fontWeight = FontWeight(el.fontWeight),
        fontStyle = if (el.fontStyle == "italic") FontStyle.Italic else FontStyle.Normal,
    ),
    ...
)
```

### Properties Panel
Two toggle buttons next to the FontPicker:
- **B** (bold toggle) — toggles fontWeight between 400 and 700
- ***I*** (italic toggle) — toggles fontStyle between "normal" and "italic"

## 5. Width Clamping Validation

### Clamp Function
```kotlin
fun clampToLabel(
    element: LabelElement,
    labelWidth: Int,
    labelHeight: Int,
): LabelElement {
    val x = element.x.coerceIn(0f, (labelWidth - element.width).coerceAtLeast(0f))
    val y = element.y.coerceIn(0f, (labelHeight - element.height).coerceAtLeast(0f))
    val w = element.width.coerceIn(MIN_ELEMENT_SIZE, labelWidth.toFloat())
    val h = element.height.coerceIn(MIN_ELEMENT_SIZE, labelHeight.toFloat())

    return when (element) {
        is LabelElement.TextElement -> element.copy(x = x, y = y, width = w, height = h)
        is LabelElement.ImageElement -> element.copy(x = x, y = y, width = w, height = h)
        is LabelElement.BarcodeElement -> element.copy(x = x, y = y, width = w, height = h)
    }
}
```

### Integration Points
- `moveElement()` — clamp after position update
- `resizeElement()` — clamp after resize
- `setElementPosition()` — clamp after numeric input
- `selectLabelSize()` — re-clamp all elements when label size changes
- Drag-to-move in EditorScreen — clamp result

## 6. Responsive Layout (< 800dp)

### Compact Mode
When `maxWidth < 800.dp`:

**Left panel → Icon strip (48dp):**
```kotlin
@Composable
fun CompactToolbox(
    onAddText: () -> Unit,
    onAddImage: (ImageBitmap) -> Unit,
    onAddBarcode: () -> Unit,
)
```
Three `IconButton`s stacked vertically: "T", image icon, barcode icon.

**Right panel → ModalBottomSheet:**
Shows `PropertiesPanel` content inside a `ModalBottomSheet` that opens when an element is selected and closes on deselect.

**Center canvas → fills remaining width**

### Implementation
Wrap the `Row` in `EditorScreen` with `BoxWithConstraints`:
```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    if (maxWidth < 800.dp) {
        CompactLayout(...)
    } else {
        FullLayout(...)  // existing three-panel Row
    }
}
```

Extract the current `Row` content into a `FullLayout` composable and create a `CompactLayout` composable.

## 7. File Changes Summary

### New Files
| File | Responsibility |
|------|---------------|
| `ImageEncoder.kt` (expect) | Base64 PNG encode/decode interface |
| `ImageEncoder.desktop.kt` | AWT implementation |
| `ImageEncoder.android.kt` | Android Bitmap implementation |
| `ImageEncoder.ios.kt` | Stub |
| `TemplateStorage.kt` (expect) | User template persistence interface |
| `TemplateStorage.desktop.kt` | File-based (~/.bluepaper/templates/) |
| `TemplateStorage.android.kt` | App files dir |
| `TemplateStorage.ios.kt` | Stub |
| `SaveTemplateDialog.kt` | Name/description input dialog |
| `CompactToolbox.kt` | Icon-only toolbox for narrow screens |

### Modified Files
| File | Changes |
|------|---------|
| `LabelElement.kt` | TextElement: fontWeight, fontStyle. SerializableLabelElement: imageData, fontWeight, fontStyle |
| `EditorState.kt` | Z-ordering methods, clamp integration, toTemplate(), image encode on save |
| `LabelCanvas.kt` | FontWeight/FontStyle in text rendering |
| `FontRegistry.kt` | get(key, weight, style), registerVariant() |
| `PropertiesPanel.kt` | Bold/italic toggles, z-ordering buttons |
| `CanvasInteraction.kt` | clampToLabel() function |
| `EditorScreen.kt` | BoxWithConstraints, compact/full layout split, save template button |
| `TopToolbar.kt` | "Save Template" button |
| `TemplatePickerDialog.kt` | "Saved" section with user templates + delete |
| `ToolboxPanel.kt` | Minor — no change if CompactToolbox is separate |

### New Test Files
| File | Tests |
|------|-------|
| `ImageEncoderTest.kt` | Base64 roundtrip (desktop only — needs ImageBitmap) |
| `ZOrderingTest.kt` | bringToFront, sendToBack, moveUp, moveDown |
| `WidthClampingTest.kt` | Clamp logic, edge cases, label size change |
| `TemplateStorageTest.kt` | Save/load/delete (desktop only) |

## 8. Out of Scope
- Real font variant .ttf bundles (designed hook for future addon)
- Font addon package marketplace
- iOS platform stubs beyond current state
- Desktop BLE
