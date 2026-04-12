# BluePaper v0.3.0 — UI Overhaul Design Spec

**Date:** 2026-04-12
**Version:** 0.2.0 → 0.3.0
**Approach:** Incremental refactor with LabelElement model from Approach B

## Overview

Redesign the BluePaper editor from a single-column scrolling form into a professional three-panel layout with interactive canvas, theme support, font picker, label templates, and undo/redo. Pro-tool aesthetic: dense, utilitarian, everything visible.

## 1. Element Model

### LabelElement Sealed Class

Replace the flat state fields (`_labelText`, `_fontSize`, `_importedImage`, `_imageTransform`) with a unified element model.

```kotlin
sealed class LabelElement {
    abstract val id: String
    abstract val x: Float       // label pixel coordinates
    abstract val y: Float
    abstract val width: Float
    abstract val height: Float
    abstract val rotation: Float

    data class TextElement(
        override val id: String,
        override val x: Float = 8f,
        override val y: Float = 8f,
        override val width: Float = 0f,   // auto-measured from text
        override val height: Float = 0f,
        override val rotation: Float = 0f,
        val text: String = "Hello",
        val fontSize: Float = 24f,
        val fontFamily: String = "default",
    ) : LabelElement()

    data class ImageElement(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val rotation: Float = 0f,
        val bitmap: ImageBitmap? = null,
        val scale: Float = 1f,
        val flipH: Boolean = false,
        val flipV: Boolean = false,
    ) : LabelElement()
}
```

All fields are `val` — mutations use `copy()` which produces a new immutable instance. This is consistent with the undo system (snapshots via `copy()`) and Compose state management (new object = recomposition).

### EditorState Changes

- `_elements: MutableStateFlow<List<LabelElement>>` replaces individual text/image flows
- `_selectedElementId: MutableStateFlow<String?>` tracks selection
- All existing mutators (setLabelText, setImageOffset, etc.) become operations on the selected element
- Element IDs generated via `UUID.randomUUID().toString()` (or expect/actual for KMP)

### Serialization

`LabelDesign` v2 stores a `List<SerializableLabelElement>` instead of flat fields. v1 .bpl files auto-migrate on load: text fields become a single `TextElement`, image transform becomes an `ImageElement` if present.

```kotlin
@Serializable
data class LabelDesign(
    val version: Int = 2,
    val model: String = "d110",
    val labelWidthMm: Double = 30.0,
    val labelHeightMm: Double = 15.0,
    val density: Int = 3,
    val quantity: Int = 1,
    val elements: List<SerializableLabelElement> = emptyList(),
    // v1 compat fields (ignored in v2, used for migration)
    val text: String? = null,
    val fontSize: Float? = null,
    val imageTransform: SerializableImageTransform? = null,
)
```

## 2. Three-Panel Layout

### Structure

```
+------------------------------------------------------------------+
|  [Save] [Load] [Undo] [Redo] [Templates]        [Theme] [Disc]  |
+------------------+------------------------+----------------------+
|   LEFT (200dp)   |   CENTER (fill)        |   RIGHT (260dp)      |
|                  |                        |                      |
|  [+ Text]        |  [Design] [Preview]    |  -- Properties --    |
|  [+ Image]       |                        |                      |
|                  |  +-----------------+   |  Position            |
|  -- Elements --  |  |                 |   |  X: [___] Y: [___]  |
|  > "Hello" (T)   |  |  Label Canvas   |   |  Size               |
|  > photo.png (I) |  |  (interactive)  |   |  W: [___] H: [___]  |
|                  |  |                 |   |  Rotation: [___]     |
|  -- Printer --   |  +-----------------+   |                      |
|  Model: [D110]   |                        |  -- Type Props --    |
|  Size: [30x15]   |                        |  (text or image      |
|  Density: 3      |                        |   specific fields)   |
|  Copies: 1       |                        |                      |
|                  |                        |  [    Print    ]     |
+------------------+------------------------+----------------------+
```

### Panel Details

**Left Panel (200dp fixed)**
- Add Element buttons: "+Text", "+Image" (calls ImagePicker)
- Element list: clickable rows showing element type icon + label (text preview or filename). Click to select. Drag to reorder z-index (future — not in v0.3.0).
- Printer section (collapsible): Model dropdown, label size chips, density slider, copies slider. Moved from current EditorScreen body.

**Center Panel (remaining space)**
- Tab row: "Design" | "Print Preview"
- Design tab: Interactive `LabelCanvas` with hit-testing, drag, resize handles, selection box
- Preview tab: `MonochromePreview` (read-only, no interaction)
- Canvas scales label pixel space to fill available width, maintaining aspect ratio

**Right Panel (260dp fixed)**
- Shows properties of the selected element
- Common fields: X, Y, Width, Height, Rotation — all as numeric `OutlinedTextField` inputs
- TextElement fields: text input (multi-line), font family dropdown (preview-in-picker), font size slider + numeric input
- ImageElement fields: scale slider + numeric, flip H/V toggles, rotate 90 button
- When nothing selected: shows printer info summary + Print button
- Print button always visible at bottom of right panel

**Top Toolbar (full width)**
- Left group: Save, Load, Undo, Redo, Templates
- Right group: Theme toggle (cycles System→Light→Dark), Disconnect
- Thin `TopAppBar` with `NavigationRow` layout

### Responsive Behavior

When window width < 800dp:
- Left panel collapses to icon-only strip (40dp)
- Right panel becomes a bottom sheet (swipe up to reveal)
- Top toolbar stays full width

## 3. Canvas Interaction

### Hit Testing

On pointer-down in the canvas:
1. Convert screen coordinates to label pixel coordinates: `labelX = screenX * (labelWidth / canvasWidth)`
2. Walk `elements` list in reverse (top z-order first)
3. For each element, check if `(labelX, labelY)` falls within its bounding rect (axis-aligned, ignoring rotation for simplicity in v0.3.0)
4. First hit = select that element. No hit = deselect all.

### Drag to Move

On `pointerInput` drag gesture:
- If drag starts within a selected element's bounds, enter drag mode
- Convert screen deltas to label pixel deltas using the same scale factor
- Update element's `x`, `y` on each drag event
- Snap to grid if enabled (quantize to nearest grid point)

### Resize Handles

8 handles drawn on the selected element:
- 4 corners: resize both axes (shift = lock aspect ratio)
- 4 edge midpoints: resize single axis
- Handle size: 6dp screen pixels (constant regardless of zoom)
- Minimum element size: 10x10 label pixels

### Keyboard Interaction

When canvas composable has focus and an element is selected:
- Arrow keys: move 1px
- Shift+Arrow: move 10px
- Delete/Backspace: remove element (with undo snapshot)
- Ctrl+A: select all (future — v0.4.0)

### Snap-to-Grid

- Default grid: 8px label pixels
- Snapping active during drag and resize
- Alt key held = disable snap temporarily
- Optional grid overlay on design canvas: thin dotted lines at grid intervals
- Grid toggle button in toolbar or keyboard shortcut (G key)

### Coordinate System

All positions in label pixel space (0,0 = top-left). Canvas composable computes a uniform scale factor:
```
scaleFactor = min(canvasWidth / labelWidthPx, canvasHeight / labelHeightPx)
```
Property panel shows label pixel values. Canvas draws scaled up for display.

## 4. Theme System

### ThemeMode Enum

```kotlin
enum class ThemeMode { System, Light, Dark }
```

Stored in `ThemeState` or hoisted as a `MutableStateFlow<ThemeMode>` in `BluePaperApp`.

### Color Scheme

Expand beyond current primary/secondary:

**Dark scheme:**
- `surface`: #1E1E1E (main background)
- `surfaceContainer`: #2A2A2A (panels)
- `surfaceContainerHigh`: #333333 (toolbar)
- `primary`: #42A5F5 (BluePaperLightBlue — accent)
- `onSurface`: #E0E0E0 (text)
- `outline`: #555555 (dividers)

**Light scheme:**
- `surface`: #FAFAFA
- `surfaceContainer`: #F0F0F0
- `surfaceContainerHigh`: #E8E8E8
- `primary`: #1565C0 (BluePaperBlue)
- `onSurface`: #212121
- `outline`: #CCCCCC

Canvas background is always white (it's paper).

### Toggle UI

Icon button in top toolbar. Cycles: System (auto icon) → Light (sun) → Dark (moon). Current mode shown as icon.

### Persistence

Theme preference saved as JSON in platform settings:
- Desktop: `~/.bluepaper/settings.json`
- Android: SharedPreferences
- iOS: NSUserDefaults

Expect/actual `ThemePreferences` interface with `save(mode: ThemeMode)` / `load(): ThemeMode` methods.

## 5. Font Family Picker

### Bundled Fonts (9 families)

| Key | Font | Category |
|-----|------|----------|
| `default` | Roboto | Sans-serif |
| `open_sans` | Open Sans | Sans-serif |
| `inter` | Inter | Sans-serif |
| `roboto_slab` | Roboto Slab | Serif |
| `noto_serif` | Noto Serif | Serif |
| `jetbrains_mono` | JetBrains Mono | Monospace |
| `roboto_mono` | Roboto Mono | Monospace |
| `oswald` | Oswald | Display |
| `anton` | Anton | Display |

Bundled as regular-weight .ttf files in `shared/src/commonMain/composeResources/font/`.

### FontRegistry

```kotlin
object FontRegistry {
    fun get(key: String): FontFamily  // returns loaded font or default
    fun allFonts(): List<Pair<String, FontFamily>>  // for picker UI
    fun nameFor(key: String): String  // display name
}
```

Loaded via Compose `Font()` resource API. Lazily initialized on first access.

### Picker UI

Dropdown in right property panel (TextElement selected). Each row renders the font name in that font. Grouped by category (Sans-serif, Serif, Monospace, Display) with section headers.

### Fallback

If a .bpl file references an unknown font key, `FontRegistry.get()` returns the default (Roboto). The properties panel shows a warning: "Font 'xyz' not available, using default."

## 6. Label Templates

### Starter Templates (6)

1. **Simple Text** — Single TextElement centered, fontSize 32, default font
2. **Two-Line** — TextElement "Title" (fontSize 28, oswald) at top + TextElement "Subtitle" (fontSize 16, default) below
3. **Image + Caption** — ImageElement filling left 60% + TextElement on right 40%
4. **Centered Image** — Single ImageElement centered, scaled to fill
5. **Price Tag** — TextElement "$0.00" large top (anton) + TextElement "Description" small bottom
6. **Inventory** — TextElement "ITEM" small top (roboto_mono) + TextElement "000" large center + TextElement "Note" small bottom

### Data Format

```kotlin
@Serializable
data class LabelTemplate(
    val name: String,
    val description: String,
    val elements: List<TemplateElement>,
)

@Serializable
data class TemplateElement(
    val type: String,           // "text" or "image"
    val xFraction: Float,       // 0.0-1.0 proportional to label width
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val text: String? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
)
```

Stored in `shared/src/commonMain/composeResources/files/templates.json`.

### Proportional Coordinates

Elements defined as fractions (0.0-1.0) of label dimensions. On apply, scaled to current label pixel size:
```
element.x = template.xFraction * labelWidthPx
element.y = template.yFraction * labelHeightPx
```

This makes templates work across D-series (240px) and B-series (384px) labels.

### Template UI

- Left panel: "Start from template" section when element list is empty (6 clickable cards)
- Top toolbar: "Templates" button (always accessible)
- If elements exist when applying a template: confirmation dialog "Replace current design?"
- Applying a template clears elements, creates new ones from template, pushes undo snapshot

## 7. Undo/Redo

### UndoManager

```kotlin
class UndoManager(private val maxHistory: Int = 50) {
    private val undoStack = mutableListOf<List<LabelElement>>()
    private val redoStack = mutableListOf<List<LabelElement>>()

    fun save(state: List<LabelElement>) {
        undoStack.add(state.map { it.copy() })  // deep copy
        redoStack.clear()
        if (undoStack.size > maxHistory) undoStack.removeAt(0)
    }

    fun undo(current: List<LabelElement>): List<LabelElement>? {
        if (undoStack.isEmpty()) return null
        redoStack.add(current.map { it.copy() })
        return undoStack.removeLast()
    }

    fun redo(current: List<LabelElement>): List<LabelElement>? {
        if (redoStack.isEmpty()) return null
        undoStack.add(current.map { it.copy() })
        return redoStack.removeLast()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
```

### Coalescing Rules

| Action | When to snapshot |
|--------|-----------------|
| Text typing | On focus-lost or 500ms idle (debounced) |
| Drag move | On pointer-up (drag end) |
| Slider changes | On slider release |
| Add/delete element | Immediately |
| Rotate 90, flip | Immediately |
| Apply template | Immediately (before clearing) |
| Load .bpl file | Immediately (before replacing) |

### Integration with EditorState

`EditorState` holds an `UndoManager` instance. Every element-mutating method calls `undoManager.save()` at the appropriate coalescing point. `undo()` and `redo()` methods on `EditorState` replace `_elements.value` with the returned snapshot.

Exposed as `canUndo: StateFlow<Boolean>` and `canRedo: StateFlow<Boolean>` for toolbar button states.

## 8. File Changes Summary

### New Files (~15-18)
- `ui/editor/LabelElement.kt` — sealed class + serializable variant
- `ui/editor/ElementList.kt` — left panel element list composable
- `ui/editor/ToolboxPanel.kt` — left panel (toolbox + elements + printer settings)
- `ui/editor/PropertiesPanel.kt` — right panel (selected element properties)
- `ui/editor/CanvasInteraction.kt` — hit testing, drag, resize handle logic
- `ui/editor/TopToolbar.kt` — toolbar composable
- `ui/editor/FontRegistry.kt` — font key → FontFamily mapping
- `ui/editor/FontPicker.kt` — font dropdown composable
- `ui/editor/TemplateManager.kt` — template loading + application
- `ui/editor/TemplatePickerDialog.kt` — template selection UI
- `ui/editor/UndoManager.kt` — undo/redo snapshot stack
- `ui/editor/ThemeState.kt` — theme mode management
- `ui/editor/ThemePreferences.kt` — expect declaration for theme persistence
- `desktopMain/.../ThemePreferences.desktop.kt` — actual (file-based JSON)
- `androidMain/.../ThemePreferences.android.kt` — actual (SharedPreferences)
- `iosMain/.../ThemePreferences.ios.kt` — actual (NSUserDefaults stub)
- `commonMain/composeResources/files/templates.json` — template definitions
- 9x `.ttf` font files in `commonMain/composeResources/font/`

### Modified Files (~8)
- `EditorScreen.kt` — rewrite to three-panel layout
- `EditorState.kt` — element model, undo integration, font support
- `LabelCanvas.kt` — draw from element list, selection rendering
- `LabelDesign.kt` — v2 format with elements, migration logic
- `MonochromePreview.kt` — receive elements instead of flat params
- `BluePaperApp.kt` — theme state, system dark theme detection
- `Theme.kt` — expanded color schemes, ThemeMode support
- `LabelRenderer.kt` — render from element list

### Deleted Files
- None. `ImageControls.kt`, `TextControls.kt`, `PrintSettings.kt` are superseded by the panel composables but can be removed once the new panels are verified.

## 9. Testing Strategy

### New Test Files
- `LabelElementTest.kt` — element creation, bounds, copy
- `UndoManagerTest.kt` — push/undo/redo/max history/coalescing
- `FontRegistryTest.kt` — lookup, fallback, all fonts enumeration
- `TemplateManagerTest.kt` — load templates, proportional scaling, migration
- `LabelDesignMigrationTest.kt` — v1 → v2 .bpl format migration
- `CanvasInteractionTest.kt` — hit testing logic (unit testable without UI)

### Existing Tests
All 118 existing tests remain green. Protocol, image encoding, and printer client are unchanged.

## 10. Out of Scope (Backlog)

- QR code / barcode generation
- Android/iOS file and image pickers
- Desktop BLE (Linux BlueZ, Windows WinRT)
- Custom user-saved templates
- Multi-element z-ordering drag
- Bold/italic font weight variants
- Image data serialization in .bpl
