# BluePaper v0.3.0 UI Overhaul — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign BluePaper's editor from a single-column scrolling form into a three-panel pro-tool layout with interactive canvas, element model, theme system, font picker, label templates, and undo/redo.

**Architecture:** Incremental refactor — introduce a `LabelElement` sealed class that unifies text and image state, then rebuild the UI around it. Existing protocol, BLE, and image encoding layers are untouched. All changes are in the `ui/editor/` package (commonMain) plus theme and platform actuals.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.7.3, Material3, kotlinx.serialization, Compose resources API for fonts/templates.

**Build/Test commands:**
```bash
ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest
ANDROID_HOME=~/Android/Sdk ./gradlew :desktopApp:run
```

**Base path:** `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/`
**Test base:** `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/`

---

## File Plan

### New Files (commonMain)
| File | Responsibility |
|------|---------------|
| `ui/editor/LabelElement.kt` | `LabelElement` sealed class (TextElement, ImageElement) + serializable variants |
| `ui/editor/UndoManager.kt` | Snapshot-based undo/redo stack |
| `ui/editor/FontRegistry.kt` | Font key → FontFamily mapping, enumeration, fallback |
| `ui/editor/FontPicker.kt` | Font family dropdown composable with preview-in-picker |
| `ui/editor/TemplateManager.kt` | Template data classes, loading from resources, apply logic |
| `ui/editor/TemplatePickerDialog.kt` | Template selection dialog composable |
| `ui/editor/ToolboxPanel.kt` | Left panel: add buttons, element list, printer settings |
| `ui/editor/ElementList.kt` | Clickable element list composable |
| `ui/editor/PropertiesPanel.kt` | Right panel: position/size/type-specific property editors |
| `ui/editor/TopToolbar.kt` | Top bar: Save, Load, Undo, Redo, Templates, Theme, Disconnect |
| `ui/editor/CanvasInteraction.kt` | Hit testing, drag, resize handle logic (pure functions + Modifier) |
| `ui/editor/ThemePreferences.kt` | Expect declarations for theme persistence |

### New Files (platform actuals)
| File | Responsibility |
|------|---------------|
| `desktopMain/.../ui/editor/ThemePreferences.desktop.kt` | File-based JSON persistence |
| `androidMain/.../ui/editor/ThemePreferences.android.kt` | Stub (return System default) |
| `iosMain/.../ui/editor/ThemePreferences.ios.kt` | Stub (return System default) |

### New Resource Files
| File | Responsibility |
|------|---------------|
| `shared/src/commonMain/composeResources/files/templates.json` | 6 starter label templates |
| `shared/src/commonMain/composeResources/font/*.ttf` | 9 bundled font files |

### New Test Files
| File | Responsibility |
|------|---------------|
| `ui/editor/LabelElementTest.kt` | Element creation, bounds, copy, serialization roundtrip |
| `ui/editor/UndoManagerTest.kt` | Push/undo/redo/max history/clear on new save |
| `ui/editor/CanvasInteractionTest.kt` | Hit testing, snap-to-grid, coordinate conversion |
| `ui/editor/TemplateManagerTest.kt` | Template loading, proportional scaling |
| `ui/editor/LabelDesignMigrationTest.kt` | v1 → v2 .bpl format migration |

### Modified Files
| File | Changes |
|------|---------|
| `ui/editor/EditorState.kt` | Replace flat state with elements list + selection + undo |
| `ui/editor/EditorScreen.kt` | Rewrite to three-panel layout |
| `ui/editor/LabelCanvas.kt` | Draw from elements list, selection box, resize handles |
| `ui/editor/LabelDesign.kt` | v2 format with elements, v1 migration |
| `ui/editor/MonochromePreview.kt` | Minor: no API change, receives rows as before |
| `ui/theme/Theme.kt` | Expanded color schemes, ThemeMode, system detection |
| `ui/BluePaperApp.kt` | Theme state, ThemeMode flow, isSystemInDarkTheme |
| `image/LabelRenderer.kt` | Render from elements list instead of flat params |
| `desktopApp/.../Main.kt` | Wider default window (1200x800) |

---

## Task 1: LabelElement Model + Serialization

**Files:**
- Create: `ui/editor/LabelElement.kt`
- Test: `ui/editor/LabelElementTest.kt`

- [ ] **Step 1: Write LabelElement tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/LabelElementTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class LabelElementTest {

    @Test
    fun textElementDefaults() {
        val el = LabelElement.TextElement(id = "t1")
        assertEquals(8f, el.x)
        assertEquals(8f, el.y)
        assertEquals("Hello", el.text)
        assertEquals(24f, el.fontSize)
        assertEquals("default", el.fontFamily)
    }

    @Test
    fun imageElementDefaults() {
        val el = LabelElement.ImageElement(id = "i1")
        assertEquals(0f, el.x)
        assertEquals(0f, el.y)
        assertEquals(1f, el.scale)
        assertEquals(false, el.flipH)
        assertEquals(false, el.flipV)
    }

    @Test
    fun copyProducesNewInstance() {
        val original = LabelElement.TextElement(id = "t1", x = 10f, y = 20f, text = "Hi")
        val moved = original.copy(x = 50f)
        assertEquals(10f, original.x)
        assertEquals(50f, moved.x)
        assertEquals("Hi", moved.text)
    }

    @Test
    fun boundsCheck() {
        val el = LabelElement.TextElement(id = "t1", x = 10f, y = 20f, width = 100f, height = 50f)
        // Point inside
        val inside = el.x <= 50f && 50f <= el.x + el.width && el.y <= 30f && 30f <= el.y + el.height
        assertEquals(true, inside)
        // Point outside
        val outside = el.x <= 200f && 200f <= el.x + el.width
        assertEquals(false, outside)
    }

    @Test
    fun serializableRoundTrip() {
        val text = LabelElement.TextElement(
            id = "t1", x = 10f, y = 20f, width = 100f, height = 50f,
            text = "Hello", fontSize = 32f, fontFamily = "oswald",
        )
        val serializable = text.toSerializable()
        assertEquals("text", serializable.type)
        assertEquals("t1", serializable.id)
        assertEquals(10f, serializable.x)
        assertEquals("Hello", serializable.text)
        assertEquals("oswald", serializable.fontFamily)

        val restored = serializable.toLabelElement()
        assertIs<LabelElement.TextElement>(restored)
        assertEquals(text.id, restored.id)
        assertEquals(text.x, restored.x)
        assertEquals(text.text, restored.text)
        assertEquals(text.fontFamily, restored.fontFamily)
    }

    @Test
    fun imageSerializableRoundTrip() {
        val img = LabelElement.ImageElement(
            id = "i1", x = 5f, y = 10f, width = 200f, height = 150f,
            scale = 1.5f, flipH = true, flipV = false, rotation = 90f,
        )
        val serializable = img.toSerializable()
        assertEquals("image", serializable.type)
        assertEquals(1.5f, serializable.scale)
        assertEquals(true, serializable.flipH)

        val restored = serializable.toLabelElement()
        assertIs<LabelElement.ImageElement>(restored)
        assertEquals(img.scale, (restored as LabelElement.ImageElement).scale)
        assertEquals(img.flipH, restored.flipH)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.LabelElementTest" -q`
Expected: Compilation failure — `LabelElement` class doesn't exist yet.

- [ ] **Step 3: Implement LabelElement.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelElement.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

sealed class LabelElement {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val width: Float
    abstract val height: Float
    abstract val rotation: Float

    data class TextElement(
        override val id: String,
        override val x: Float = 8f,
        override val y: Float = 8f,
        override val width: Float = 0f,
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

/** Serializable version of LabelElement for .bpl JSON persistence. */
@Serializable
data class SerializableLabelElement(
    val type: String,
    val id: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    // TextElement fields
    val text: String? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
    // ImageElement fields
    val scale: Float? = null,
    val flipH: Boolean? = null,
    val flipV: Boolean? = null,
)

fun LabelElement.toSerializable(): SerializableLabelElement = when (this) {
    is LabelElement.TextElement -> SerializableLabelElement(
        type = "text", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, text = text, fontSize = fontSize, fontFamily = fontFamily,
    )
    is LabelElement.ImageElement -> SerializableLabelElement(
        type = "image", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, scale = scale, flipH = flipH, flipV = flipV,
    )
}

fun SerializableLabelElement.toLabelElement(): LabelElement = when (type) {
    "text" -> LabelElement.TextElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        text = text ?: "Hello", fontSize = fontSize ?: 24f, fontFamily = fontFamily ?: "default",
    )
    "image" -> LabelElement.ImageElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        scale = scale ?: 1f, flipH = flipH ?: false, flipV = flipV ?: false,
    )
    else -> LabelElement.TextElement(id = id, x = x, y = y, text = "Unknown")
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.LabelElementTest" -q`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelElement.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/LabelElementTest.kt
git commit -m "feat: LabelElement sealed class with serialization"
```

---

## Task 2: UndoManager

**Files:**
- Create: `ui/editor/UndoManager.kt`
- Test: `ui/editor/UndoManagerTest.kt`

- [ ] **Step 1: Write UndoManager tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/UndoManagerTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UndoManagerTest {

    private fun textEl(id: String, text: String) =
        LabelElement.TextElement(id = id, text = text)

    @Test
    fun undoEmptyReturnsNull() {
        val mgr = UndoManager()
        assertNull(mgr.undo(emptyList()))
        assertFalse(mgr.canUndo)
    }

    @Test
    fun redoEmptyReturnsNull() {
        val mgr = UndoManager()
        assertNull(mgr.redo(emptyList()))
        assertFalse(mgr.canRedo)
    }

    @Test
    fun saveAndUndo() {
        val mgr = UndoManager()
        val state0 = listOf(textEl("t1", "A"))
        mgr.save(state0)
        assertTrue(mgr.canUndo)

        val state1 = listOf(textEl("t1", "B"))
        val restored = mgr.undo(state1)
        assertEquals("A", (restored!![0] as LabelElement.TextElement).text)
    }

    @Test
    fun undoThenRedo() {
        val mgr = UndoManager()
        val state0 = listOf(textEl("t1", "A"))
        mgr.save(state0)

        val state1 = listOf(textEl("t1", "B"))
        mgr.undo(state1)
        assertTrue(mgr.canRedo)

        val redone = mgr.redo(state0)
        assertEquals("B", (redone!![0] as LabelElement.TextElement).text)
    }

    @Test
    fun saveClearsRedoStack() {
        val mgr = UndoManager()
        mgr.save(listOf(textEl("t1", "A")))
        mgr.undo(listOf(textEl("t1", "B")))
        assertTrue(mgr.canRedo)

        mgr.save(listOf(textEl("t1", "C")))
        assertFalse(mgr.canRedo)
    }

    @Test
    fun maxHistoryEnforced() {
        val mgr = UndoManager(maxHistory = 3)
        mgr.save(listOf(textEl("t1", "A")))
        mgr.save(listOf(textEl("t1", "B")))
        mgr.save(listOf(textEl("t1", "C")))
        mgr.save(listOf(textEl("t1", "D")))

        // Should only have 3 entries; oldest (A) dropped
        val r1 = mgr.undo(listOf(textEl("t1", "E")))
        assertEquals("D", (r1!![0] as LabelElement.TextElement).text)
        val r2 = mgr.undo(r1)
        assertEquals("C", (r2!![0] as LabelElement.TextElement).text)
        val r3 = mgr.undo(r2)
        assertEquals("B", (r3!![0] as LabelElement.TextElement).text)
        val r4 = mgr.undo(r3)
        assertNull(r4) // A was dropped
    }

    @Test
    fun snapshotsAreDeepCopies() {
        val mgr = UndoManager()
        val elements = listOf(textEl("t1", "Original"))
        mgr.save(elements)

        // The saved snapshot should not be affected by mutations to the original list
        val restored = mgr.undo(listOf(textEl("t1", "Modified")))
        assertEquals("Original", (restored!![0] as LabelElement.TextElement).text)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.UndoManagerTest" -q`
Expected: Compilation failure — `UndoManager` doesn't exist.

- [ ] **Step 3: Implement UndoManager.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/UndoManager.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

class UndoManager(private val maxHistory: Int = 50) {

    private val undoStack = mutableListOf<List<LabelElement>>()
    private val redoStack = mutableListOf<List<LabelElement>>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun save(state: List<LabelElement>) {
        undoStack.add(deepCopy(state))
        redoStack.clear()
        if (undoStack.size > maxHistory) undoStack.removeAt(0)
    }

    fun undo(current: List<LabelElement>): List<LabelElement>? {
        if (undoStack.isEmpty()) return null
        redoStack.add(deepCopy(current))
        return undoStack.removeLast()
    }

    fun redo(current: List<LabelElement>): List<LabelElement>? {
        if (redoStack.isEmpty()) return null
        undoStack.add(deepCopy(current))
        return redoStack.removeLast()
    }

    private fun deepCopy(elements: List<LabelElement>): List<LabelElement> =
        elements.map { element ->
            when (element) {
                is LabelElement.TextElement -> element.copy()
                is LabelElement.ImageElement -> element.copy()
            }
        }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.UndoManagerTest" -q`
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/UndoManager.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/UndoManagerTest.kt
git commit -m "feat: UndoManager with snapshot-based undo/redo"
```

---

## Task 3: Canvas Interaction Logic (Hit Testing + Snap)

**Files:**
- Create: `ui/editor/CanvasInteraction.kt`
- Test: `ui/editor/CanvasInteractionTest.kt`

- [ ] **Step 1: Write CanvasInteraction tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/CanvasInteractionTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CanvasInteractionTest {

    private val elements = listOf(
        LabelElement.TextElement(id = "t1", x = 10f, y = 10f, width = 100f, height = 50f),
        LabelElement.TextElement(id = "t2", x = 80f, y = 40f, width = 100f, height = 50f),
    )

    @Test
    fun hitTestFindsTopElement() {
        // Point (90, 50) is inside both t1 and t2. t2 is on top (later in list).
        val hit = hitTest(elements, 90f, 50f)
        assertEquals("t2", hit?.id)
    }

    @Test
    fun hitTestFindsOnlyMatchingElement() {
        // Point (20, 20) is only inside t1
        val hit = hitTest(elements, 20f, 20f)
        assertEquals("t1", hit?.id)
    }

    @Test
    fun hitTestMissReturnsNull() {
        val hit = hitTest(elements, 300f, 300f)
        assertNull(hit)
    }

    @Test
    fun hitTestOnBoundary() {
        // Point exactly on the edge of t1 (10, 10) should hit
        val hit = hitTest(elements, 10f, 10f)
        assertEquals("t1", hit?.id)
    }

    @Test
    fun snapToGridRoundsCorrectly() {
        assertEquals(0f, snapToGrid(3f, 8f))
        assertEquals(8f, snapToGrid(5f, 8f))
        assertEquals(8f, snapToGrid(8f, 8f))
        assertEquals(8f, snapToGrid(11f, 8f))
        assertEquals(16f, snapToGrid(13f, 8f))
    }

    @Test
    fun snapToGridDisabled() {
        // Grid size 0 means no snapping
        assertEquals(3.7f, snapToGrid(3.7f, 0f))
    }

    @Test
    fun screenToLabelCoordinates() {
        // Canvas is 400x200 screen pixels, label is 200x100 label pixels
        // Scale factor = min(400/200, 200/100) = 2.0
        val (lx, ly) = screenToLabel(100f, 50f, canvasWidth = 400f, canvasHeight = 200f, labelWidth = 200, labelHeight = 100)
        assertEquals(50f, lx)
        assertEquals(25f, ly)
    }

    @Test
    fun screenToLabelWithNonUniformAspect() {
        // Canvas 600x200, label 200x100. scaleFactor = min(3.0, 2.0) = 2.0
        val (lx, ly) = screenToLabel(200f, 100f, canvasWidth = 600f, canvasHeight = 200f, labelWidth = 200, labelHeight = 100)
        assertEquals(100f, lx)
        assertEquals(50f, ly)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.CanvasInteractionTest" -q`
Expected: Compilation failure.

- [ ] **Step 3: Implement CanvasInteraction.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/CanvasInteraction.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Hit-test elements in reverse z-order (last element = top).
 * Returns the topmost element containing the point, or null.
 */
fun hitTest(elements: List<LabelElement>, labelX: Float, labelY: Float): LabelElement? {
    for (element in elements.asReversed()) {
        if (labelX >= element.x && labelX <= element.x + element.width &&
            labelY >= element.y && labelY <= element.y + element.height
        ) {
            return element
        }
    }
    return null
}

/**
 * Snap a coordinate to the nearest grid point.
 * gridSize <= 0 disables snapping.
 */
fun snapToGrid(value: Float, gridSize: Float): Float {
    if (gridSize <= 0f) return value
    return (value / gridSize).roundToInt() * gridSize
}

/**
 * Convert screen coordinates to label pixel coordinates.
 * Returns (labelX, labelY).
 */
fun screenToLabel(
    screenX: Float,
    screenY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    labelWidth: Int,
    labelHeight: Int,
): Pair<Float, Float> {
    val scaleFactor = min(canvasWidth / labelWidth, canvasHeight / labelHeight)
    return Pair(screenX / scaleFactor, screenY / scaleFactor)
}

/**
 * Convert a screen delta to a label pixel delta.
 */
fun screenDeltaToLabel(
    deltaX: Float,
    deltaY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    labelWidth: Int,
    labelHeight: Int,
): Pair<Float, Float> {
    val scaleFactor = min(canvasWidth / labelWidth, canvasHeight / labelHeight)
    return Pair(deltaX / scaleFactor, deltaY / scaleFactor)
}

/** Size of resize handles in screen dp. */
const val HANDLE_SIZE_DP = 6f

/** Minimum element size in label pixels. */
const val MIN_ELEMENT_SIZE = 10f

/** Default grid size in label pixels. */
const val DEFAULT_GRID_SIZE = 8f

/**
 * Identifies which resize handle (if any) is under the pointer.
 * Returns null if no handle is hit.
 */
enum class ResizeHandle {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
}

fun hitTestHandle(
    element: LabelElement,
    labelX: Float,
    labelY: Float,
    handleSizeLabel: Float,
): ResizeHandle? {
    val hs = handleSizeLabel / 2f
    val cx = element.x + element.width / 2f
    val cy = element.y + element.height / 2f
    val r = element.x + element.width
    val b = element.y + element.height

    data class HandleDef(val hx: Float, val hy: Float, val handle: ResizeHandle)

    val handles = listOf(
        HandleDef(element.x, element.y, ResizeHandle.TOP_LEFT),
        HandleDef(cx, element.y, ResizeHandle.TOP),
        HandleDef(r, element.y, ResizeHandle.TOP_RIGHT),
        HandleDef(element.x, cy, ResizeHandle.LEFT),
        HandleDef(r, cy, ResizeHandle.RIGHT),
        HandleDef(element.x, b, ResizeHandle.BOTTOM_LEFT),
        HandleDef(cx, b, ResizeHandle.BOTTOM),
        HandleDef(r, b, ResizeHandle.BOTTOM_RIGHT),
    )

    for ((hx, hy, handle) in handles) {
        if (labelX >= hx - hs && labelX <= hx + hs && labelY >= hy - hs && labelY <= hy + hs) {
            return handle
        }
    }
    return null
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.CanvasInteractionTest" -q`
Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/CanvasInteraction.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/CanvasInteractionTest.kt
git commit -m "feat: canvas interaction logic — hit testing, snap-to-grid, coordinate conversion"
```

---

## Task 4: LabelDesign v2 Format + Migration

**Files:**
- Modify: `ui/editor/LabelDesign.kt`
- Test: `ui/editor/LabelDesignMigrationTest.kt`

- [ ] **Step 1: Write v1→v2 migration tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/LabelDesignMigrationTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LabelDesignMigrationTest {

    @Test
    fun v2RoundTrip() {
        val design = LabelDesign(
            version = 2,
            model = "b21",
            labelWidthMm = 50.0,
            labelHeightMm = 30.0,
            density = 5,
            quantity = 3,
            elements = listOf(
                SerializableLabelElement(type = "text", id = "t1", x = 10f, y = 20f, text = "Hello", fontSize = 32f, fontFamily = "oswald"),
                SerializableLabelElement(type = "image", id = "i1", x = 0f, y = 0f, width = 100f, height = 80f, scale = 1.5f),
            ),
        )
        val json = LabelDesign.toJson(design)
        val restored = LabelDesign.fromJson(json)
        assertEquals(2, restored.version)
        assertEquals(2, restored.elements.size)
        assertEquals("Hello", restored.elements[0].text)
        assertEquals(1.5f, restored.elements[1].scale)
    }

    @Test
    fun v1MigratesToV2() {
        // v1 JSON has flat text/imageTransform fields, no version or elements
        val v1Json = """
        {
            "text": "Old Label",
            "fontSize": 18.0,
            "model": "d110",
            "labelWidthMm": 30.0,
            "labelHeightMm": 15.0,
            "density": 3,
            "quantity": 1,
            "imageTransform": {
                "offsetX": 5.0,
                "offsetY": 10.0,
                "scale": 2.0,
                "rotation": 90.0,
                "flipH": true,
                "flipV": false
            }
        }
        """.trimIndent()

        val design = LabelDesign.fromJson(v1Json)
        val migrated = design.migrateToV2()

        assertEquals(2, migrated.version)
        assertEquals(2, migrated.elements.size)

        val textEl = migrated.elements[0]
        assertEquals("text", textEl.type)
        assertEquals("Old Label", textEl.text)
        assertEquals(18f, textEl.fontSize)

        val imgEl = migrated.elements[1]
        assertEquals("image", imgEl.type)
        assertEquals(5f, imgEl.x)
        assertEquals(10f, imgEl.y)
        assertEquals(2f, imgEl.scale)
        assertEquals(true, imgEl.flipH)
    }

    @Test
    fun v1TextOnlyMigration() {
        val v1Json = """
        {
            "text": "Simple",
            "fontSize": 24.0,
            "model": "d110",
            "labelWidthMm": 30.0,
            "labelHeightMm": 15.0,
            "density": 3,
            "quantity": 1,
            "imageTransform": {}
        }
        """.trimIndent()

        val design = LabelDesign.fromJson(v1Json)
        val migrated = design.migrateToV2()

        assertEquals(2, migrated.version)
        assertEquals(1, migrated.elements.size) // only text, no image
        assertEquals("text", migrated.elements[0].type)
    }

    @Test
    fun v2DesignMigrateIsNoop() {
        val design = LabelDesign(
            version = 2,
            elements = listOf(
                SerializableLabelElement(type = "text", id = "t1", text = "Hi"),
            ),
        )
        val migrated = design.migrateToV2()
        assertEquals(design.elements, migrated.elements)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.LabelDesignMigrationTest" -q`
Expected: Compilation failure — `migrateToV2()` doesn't exist.

- [ ] **Step 3: Update LabelDesign.kt**

Replace the entire contents of `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelDesign.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LabelDesign(
    val version: Int = 2,
    val model: String = "d110",
    val labelWidthMm: Double = 30.0,
    val labelHeightMm: Double = 15.0,
    val density: Int = 3,
    val quantity: Int = 1,
    val elements: List<SerializableLabelElement> = emptyList(),
    // v1 compat fields (read for migration, not written in v2)
    val text: String? = null,
    val fontSize: Float? = null,
    val imageTransform: SerializableImageTransform? = null,
) {
    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        fun toJson(design: LabelDesign): String = json.encodeToString(serializer(), design)

        fun fromJson(jsonString: String): LabelDesign = json.decodeFromString(serializer(), jsonString)
    }

    /**
     * Migrate v1 designs to v2 element format.
     * v1 designs have flat text/imageTransform fields and no elements list.
     * v2 designs have an elements list. If already v2, returns as-is.
     */
    fun migrateToV2(): LabelDesign {
        if (version >= 2 || elements.isNotEmpty()) return this

        val migrated = mutableListOf<SerializableLabelElement>()

        // Migrate text
        if (text != null && text.isNotEmpty()) {
            migrated.add(
                SerializableLabelElement(
                    type = "text",
                    id = "migrated_text",
                    x = 8f,
                    y = 8f,
                    text = text,
                    fontSize = fontSize ?: 24f,
                    fontFamily = "default",
                ),
            )
        }

        // Migrate image transform (only if it has non-default values)
        imageTransform?.let { transform ->
            val hasImage = transform.offsetX != 0f || transform.offsetY != 0f ||
                transform.scale != 1f || transform.rotation != 0f ||
                transform.flipH || transform.flipV
            if (hasImage) {
                migrated.add(
                    SerializableLabelElement(
                        type = "image",
                        id = "migrated_image",
                        x = transform.offsetX,
                        y = transform.offsetY,
                        rotation = transform.rotation,
                        scale = transform.scale,
                        flipH = transform.flipH,
                        flipV = transform.flipV,
                    ),
                )
            }
        }

        return copy(version = 2, elements = migrated, text = null, fontSize = null, imageTransform = null)
    }
}

// Keep v1 compat types for deserialization
@Serializable
data class SerializableImageTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

fun ImageTransform.toSerializable() = SerializableImageTransform(offsetX, offsetY, scale, rotation, flipH, flipV)
fun SerializableImageTransform.toImageTransform() = ImageTransform(offsetX, offsetY, scale, rotation, flipH, flipV)
```

- [ ] **Step 4: Run ALL tests to verify nothing broke**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS (existing LabelDesignTest still works because `ignoreUnknownKeys = true` handles the new fields, and the old constructor params are still accepted via defaults).

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelDesign.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/LabelDesignMigrationTest.kt
git commit -m "feat: LabelDesign v2 format with elements list + v1 migration"
```

---

## Task 5: Theme System

**Files:**
- Modify: `ui/theme/Theme.kt`
- Create: `ui/editor/ThemePreferences.kt` (expect)
- Create: `desktopMain/.../ThemePreferences.desktop.kt` (actual)
- Create: `androidMain/.../ThemePreferences.android.kt` (actual)
- Create: `iosMain/.../ThemePreferences.ios.kt` (actual)
- Modify: `ui/BluePaperApp.kt`

- [ ] **Step 1: Update Theme.kt with expanded color schemes and ThemeMode**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/theme/Theme.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { System, Light, Dark }

private val BluePaperBlue = Color(0xFF1565C0)
private val BluePaperLightBlue = Color(0xFF42A5F5)

private val LightColors = lightColorScheme(
    primary = BluePaperBlue,
    secondary = BluePaperLightBlue,
    surface = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    onSurface = Color(0xFF212121),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
)

private val DarkColors = darkColorScheme(
    primary = BluePaperLightBlue,
    secondary = BluePaperBlue,
    surface = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF333333),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF444444),
)

@Composable
fun BluePaperTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
```

- [ ] **Step 2: Create ThemePreferences expect declaration**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode

expect object ThemePreferences {
    fun save(mode: ThemeMode)
    fun load(): ThemeMode
}
```

- [ ] **Step 3: Create desktop actual**

Create `shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.desktop.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode
import java.io.File

actual object ThemePreferences {
    private val settingsDir = File(System.getProperty("user.home"), ".bluepaper")
    private val settingsFile = File(settingsDir, "settings.json")

    actual fun save(mode: ThemeMode) {
        settingsDir.mkdirs()
        settingsFile.writeText("""{"themeMode":"${mode.name}"}""")
    }

    actual fun load(): ThemeMode {
        if (!settingsFile.exists()) return ThemeMode.System
        return try {
            val content = settingsFile.readText()
            val modeStr = Regex(""""themeMode"\s*:\s*"(\w+)"""").find(content)?.groupValues?.get(1)
            ThemeMode.valueOf(modeStr ?: "System")
        } catch (_: Exception) {
            ThemeMode.System
        }
    }
}
```

- [ ] **Step 4: Create Android actual (stub)**

Create `shared/src/androidMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.android.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode

actual object ThemePreferences {
    actual fun save(mode: ThemeMode) { /* TODO: SharedPreferences */ }
    actual fun load(): ThemeMode = ThemeMode.System
}
```

- [ ] **Step 5: Create iOS actual (stub)**

Create `shared/src/iosMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.ios.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode

actual object ThemePreferences {
    actual fun save(mode: ThemeMode) { /* TODO: NSUserDefaults */ }
    actual fun load(): ThemeMode = ThemeMode.System
}
```

- [ ] **Step 6: Update BluePaperApp.kt with theme state**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt`:

```kotlin
package com.avicennasis.bluepaper.ui

import androidx.compose.runtime.*
import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ui.editor.EditorScreen
import com.avicennasis.bluepaper.ui.editor.EditorState
import com.avicennasis.bluepaper.ui.editor.ThemePreferences
import com.avicennasis.bluepaper.ui.scanner.ScannerScreen
import com.avicennasis.bluepaper.ui.scanner.ScannerState
import com.avicennasis.bluepaper.ui.theme.BluePaperTheme
import com.avicennasis.bluepaper.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope

enum class Screen { Scanner, Editor }

@Composable
fun BluePaperApp(
    scanner: BleScanner,
    transport: BleTransport,
    scope: CoroutineScope,
    startScreen: Screen = Screen.Scanner,
) {
    var currentScreen by remember { mutableStateOf(startScreen) }
    var themeMode by remember { mutableStateOf(ThemePreferences.load()) }

    val scannerState = remember { ScannerState(scanner, transport, scope) }
    val editorState = remember { EditorState(transport, scope) }

    BluePaperTheme(themeMode = themeMode) {
        when (currentScreen) {
            Screen.Scanner -> ScannerScreen(
                state = scannerState,
                onConnected = { currentScreen = Screen.Editor },
            )
            Screen.Editor -> EditorScreen(
                state = editorState,
                themeMode = themeMode,
                onThemeToggle = {
                    themeMode = when (themeMode) {
                        ThemeMode.System -> ThemeMode.Light
                        ThemeMode.Light -> ThemeMode.Dark
                        ThemeMode.Dark -> ThemeMode.System
                    }
                    ThemePreferences.save(themeMode)
                },
                onDisconnect = {
                    scannerState.disconnect()
                    currentScreen = Screen.Scanner
                },
            )
        }
    }
}
```

- [ ] **Step 7: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileDebugKotlinAndroid :shared:compileKotlinDesktop -q`
Expected: Compilation will fail because `EditorScreen` doesn't accept `themeMode`/`onThemeToggle` yet. That's expected — Task 8 will update EditorScreen. For now, temporarily keep the old `EditorScreen` signature by reverting BluePaperApp changes to not pass theme params. We'll fix this in Task 8.

**Temporary:** Keep BluePaperApp.kt calling `EditorScreen(state, onDisconnect)` without theme params. Store `themeMode` and `onThemeToggle` logic in BluePaperApp — it'll be wired up when EditorScreen gets the toolbar in Task 8.

Update BluePaperApp.kt to not pass theme params to EditorScreen yet:

```kotlin
// In BluePaperApp, keep the EditorScreen call as:
Screen.Editor -> EditorScreen(
    state = editorState,
    onDisconnect = {
        scannerState.disconnect()
        currentScreen = Screen.Scanner
    },
)
// themeMode and onThemeToggle will be wired in Task 8
```

- [ ] **Step 8: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS.

- [ ] **Step 9: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/theme/Theme.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.kt \
       shared/src/desktopMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.desktop.kt \
       shared/src/androidMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.android.kt \
       shared/src/iosMain/kotlin/com/avicennasis/bluepaper/ui/editor/ThemePreferences.ios.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt
git commit -m "feat: theme system — ThemeMode, expanded color schemes, platform persistence"
```

---

## Task 6: Font Registry + Bundled Fonts

**Files:**
- Create: `ui/editor/FontRegistry.kt`
- Create: 9 font .ttf files in `composeResources/font/`

- [ ] **Step 1: Download and place font files**

Create the composeResources directory structure and download the 9 OFL-licensed fonts:

```bash
mkdir -p shared/src/commonMain/composeResources/font
```

Download these Google Fonts (all OFL-licensed) as single regular-weight .ttf files into `shared/src/commonMain/composeResources/font/`:
- `roboto_regular.ttf`
- `opensans_regular.ttf`
- `inter_regular.ttf`
- `robotoslab_regular.ttf`
- `notoserif_regular.ttf`
- `jetbrainsmono_regular.ttf`
- `robotomono_regular.ttf`
- `oswald_regular.ttf`
- `anton_regular.ttf`

Use `curl` to fetch from Google Fonts API or download manually.

- [ ] **Step 2: Implement FontRegistry.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/FontRegistry.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.text.font.FontFamily

data class FontEntry(
    val key: String,
    val displayName: String,
    val category: String,
    val family: FontFamily,
)

/**
 * Registry of bundled fonts for label text.
 *
 * Note: Compose Multiplatform resource-based fonts require platform-specific
 * loading via `Font()` resource API. This registry uses FontFamily.Default
 * as a placeholder — platform entry points should call `initPlatformFonts()`
 * to register actual loaded FontFamily instances.
 *
 * For desktop JVM, system fonts can be loaded via java.awt.Font and
 * FontFamily(Typeface(...)). For now we start with the system default
 * and add resource-based loading when composeResources font loading is set up.
 */
object FontRegistry {
    private val entries = mutableListOf<FontEntry>()
    private var initialized = false

    private val defaultEntries = listOf(
        Triple("default", "Default", "Sans-serif"),
        Triple("open_sans", "Open Sans", "Sans-serif"),
        Triple("inter", "Inter", "Sans-serif"),
        Triple("roboto_slab", "Roboto Slab", "Serif"),
        Triple("noto_serif", "Noto Serif", "Serif"),
        Triple("jetbrains_mono", "JetBrains Mono", "Monospace"),
        Triple("roboto_mono", "Roboto Mono", "Monospace"),
        Triple("oswald", "Oswald", "Display"),
        Triple("anton", "Anton", "Display"),
    )

    fun init(fonts: Map<String, FontFamily> = emptyMap()) {
        entries.clear()
        for ((key, name, category) in defaultEntries) {
            entries.add(FontEntry(key, name, category, fonts[key] ?: FontFamily.Default))
        }
        initialized = true
    }

    fun get(key: String): FontFamily {
        if (!initialized) init()
        return entries.find { it.key == key }?.family ?: entries.first().family
    }

    fun allFonts(): List<FontEntry> {
        if (!initialized) init()
        return entries.toList()
    }

    fun nameFor(key: String): String {
        if (!initialized) init()
        return entries.find { it.key == key }?.displayName ?: "Default"
    }

    fun categories(): List<String> {
        if (!initialized) init()
        return entries.map { it.category }.distinct()
    }

    fun fontsInCategory(category: String): List<FontEntry> {
        if (!initialized) init()
        return entries.filter { it.category == category }
    }
}
```

- [ ] **Step 3: Run all tests to ensure nothing broke**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/FontRegistry.kt \
       shared/src/commonMain/composeResources/font/
git commit -m "feat: FontRegistry with 9 bundled font slots"
```

---

## Task 7: Template Manager + Template Data

**Files:**
- Create: `ui/editor/TemplateManager.kt`
- Create: `composeResources/files/templates.json`
- Test: `ui/editor/TemplateManagerTest.kt`

- [ ] **Step 1: Write template tests**

Create `shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/TemplateManagerTest.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateManagerTest {

    @Test
    fun builtInTemplatesLoad() {
        val templates = TemplateManager.builtInTemplates()
        assertEquals(6, templates.size)
    }

    @Test
    fun simpleTextTemplateHasOneElement() {
        val templates = TemplateManager.builtInTemplates()
        val simple = templates.find { it.name == "Simple Text" }!!
        assertEquals(1, simple.elements.size)
        assertEquals("text", simple.elements[0].type)
    }

    @Test
    fun proportionalScaling() {
        val templateEl = TemplateElement(
            type = "text", xFraction = 0.1f, yFraction = 0.2f,
            widthFraction = 0.8f, heightFraction = 0.3f,
            text = "Test", fontSize = 24f,
        )
        val labelElement = TemplateManager.scaleToLabel(templateEl, labelWidthPx = 240, labelHeightPx = 120, idPrefix = "t")
        assertEquals(24f, labelElement.x)   // 0.1 * 240
        assertEquals(24f, labelElement.y)   // 0.2 * 120
        assertEquals(192f, labelElement.width)  // 0.8 * 240
        assertEquals(36f, labelElement.height)  // 0.3 * 120
    }

    @Test
    fun applyTemplateProducesCorrectElements() {
        val template = LabelTemplate(
            name = "Test",
            description = "test",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0f, yFraction = 0f, widthFraction = 1f, heightFraction = 0.5f, text = "Title", fontSize = 28f, fontFamily = "oswald"),
                TemplateElement(type = "text", xFraction = 0f, yFraction = 0.5f, widthFraction = 1f, heightFraction = 0.5f, text = "Subtitle", fontSize = 16f),
            ),
        )
        val elements = TemplateManager.applyTemplate(template, labelWidthPx = 200, labelHeightPx = 100)
        assertEquals(2, elements.size)

        val title = elements[0] as LabelElement.TextElement
        assertEquals("Title", title.text)
        assertEquals(28f, title.fontSize)
        assertEquals("oswald", title.fontFamily)
        assertEquals(0f, title.x)
        assertEquals(0f, title.y)
        assertEquals(200f, title.width)

        val subtitle = elements[1] as LabelElement.TextElement
        assertEquals("Subtitle", subtitle.text)
        assertEquals(50f, subtitle.y)  // 0.5 * 100
    }

    @Test
    fun imageTemplateElementProducesImageElement() {
        val template = LabelTemplate(
            name = "Test",
            description = "test",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 0.6f, heightFraction = 1f),
            ),
        )
        val elements = TemplateManager.applyTemplate(template, labelWidthPx = 200, labelHeightPx = 100)
        assertEquals(1, elements.size)
        assertTrue(elements[0] is LabelElement.ImageElement)
        assertEquals(120f, elements[0].width)  // 0.6 * 200
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.TemplateManagerTest" -q`
Expected: Compilation failure.

- [ ] **Step 3: Implement TemplateManager.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TemplateManager.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.Serializable

@Serializable
data class LabelTemplate(
    val name: String,
    val description: String,
    val elements: List<TemplateElement>,
)

@Serializable
data class TemplateElement(
    val type: String,
    val xFraction: Float = 0f,
    val yFraction: Float = 0f,
    val widthFraction: Float = 1f,
    val heightFraction: Float = 1f,
    val text: String? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
)

object TemplateManager {

    fun builtInTemplates(): List<LabelTemplate> = listOf(
        LabelTemplate(
            name = "Simple Text",
            description = "Single centered text",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.1f, widthFraction = 0.9f, heightFraction = 0.8f, text = "Label Text", fontSize = 32f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Two-Line",
            description = "Title + subtitle",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.05f, widthFraction = 0.9f, heightFraction = 0.45f, text = "Title", fontSize = 28f, fontFamily = "oswald"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.55f, widthFraction = 0.9f, heightFraction = 0.4f, text = "Subtitle", fontSize = 16f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Image + Caption",
            description = "Image left, text right",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 0.6f, heightFraction = 1f),
                TemplateElement(type = "text", xFraction = 0.62f, yFraction = 0.1f, widthFraction = 0.35f, heightFraction = 0.8f, text = "Caption", fontSize = 16f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Centered Image",
            description = "Full-bleed image",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 1f, heightFraction = 1f),
            ),
        ),
        LabelTemplate(
            name = "Price Tag",
            description = "Large price + description",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.05f, widthFraction = 0.9f, heightFraction = 0.55f, text = "$0.00", fontSize = 36f, fontFamily = "anton"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.65f, widthFraction = 0.9f, heightFraction = 0.3f, text = "Description", fontSize = 14f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Inventory",
            description = "Title, code, note",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.02f, widthFraction = 0.9f, heightFraction = 0.2f, text = "ITEM", fontSize = 14f, fontFamily = "roboto_mono"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.25f, widthFraction = 0.9f, heightFraction = 0.45f, text = "000", fontSize = 40f, fontFamily = "default"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.75f, widthFraction = 0.9f, heightFraction = 0.2f, text = "Note", fontSize = 12f, fontFamily = "default"),
            ),
        ),
    )

    fun scaleToLabel(
        templateEl: TemplateElement,
        labelWidthPx: Int,
        labelHeightPx: Int,
        idPrefix: String,
    ): LabelElement {
        val x = templateEl.xFraction * labelWidthPx
        val y = templateEl.yFraction * labelHeightPx
        val w = templateEl.widthFraction * labelWidthPx
        val h = templateEl.heightFraction * labelHeightPx
        val id = "${idPrefix}_${System.identityHashCode(templateEl)}"

        return when (templateEl.type) {
            "text" -> LabelElement.TextElement(
                id = id, x = x, y = y, width = w, height = h,
                text = templateEl.text ?: "Text",
                fontSize = templateEl.fontSize ?: 24f,
                fontFamily = templateEl.fontFamily ?: "default",
            )
            "image" -> LabelElement.ImageElement(
                id = id, x = x, y = y, width = w, height = h,
            )
            else -> LabelElement.TextElement(id = id, x = x, y = y, text = "Unknown")
        }
    }

    fun applyTemplate(
        template: LabelTemplate,
        labelWidthPx: Int,
        labelHeightPx: Int,
    ): List<LabelElement> {
        return template.elements.mapIndexed { index, el ->
            scaleToLabel(el, labelWidthPx, labelHeightPx, idPrefix = "tmpl$index")
        }
    }
}
```

- [ ] **Step 4: Create templates.json resource file**

Create `shared/src/commonMain/composeResources/files/templates.json`:

```json
[
  {
    "name": "Simple Text",
    "description": "Single centered text",
    "elements": [
      {"type": "text", "xFraction": 0.05, "yFraction": 0.1, "widthFraction": 0.9, "heightFraction": 0.8, "text": "Label Text", "fontSize": 32.0, "fontFamily": "default"}
    ]
  },
  {
    "name": "Two-Line",
    "description": "Title + subtitle",
    "elements": [
      {"type": "text", "xFraction": 0.05, "yFraction": 0.05, "widthFraction": 0.9, "heightFraction": 0.45, "text": "Title", "fontSize": 28.0, "fontFamily": "oswald"},
      {"type": "text", "xFraction": 0.05, "yFraction": 0.55, "widthFraction": 0.9, "heightFraction": 0.4, "text": "Subtitle", "fontSize": 16.0, "fontFamily": "default"}
    ]
  },
  {
    "name": "Image + Caption",
    "description": "Image left, text right",
    "elements": [
      {"type": "image", "xFraction": 0.0, "yFraction": 0.0, "widthFraction": 0.6, "heightFraction": 1.0},
      {"type": "text", "xFraction": 0.62, "yFraction": 0.1, "widthFraction": 0.35, "heightFraction": 0.8, "text": "Caption", "fontSize": 16.0, "fontFamily": "default"}
    ]
  },
  {
    "name": "Centered Image",
    "description": "Full-bleed image",
    "elements": [
      {"type": "image", "xFraction": 0.0, "yFraction": 0.0, "widthFraction": 1.0, "heightFraction": 1.0}
    ]
  },
  {
    "name": "Price Tag",
    "description": "Large price + description",
    "elements": [
      {"type": "text", "xFraction": 0.05, "yFraction": 0.05, "widthFraction": 0.9, "heightFraction": 0.55, "text": "$0.00", "fontSize": 36.0, "fontFamily": "anton"},
      {"type": "text", "xFraction": 0.05, "yFraction": 0.65, "widthFraction": 0.9, "heightFraction": 0.3, "text": "Description", "fontSize": 14.0, "fontFamily": "default"}
    ]
  },
  {
    "name": "Inventory",
    "description": "Title, code, note",
    "elements": [
      {"type": "text", "xFraction": 0.05, "yFraction": 0.02, "widthFraction": 0.9, "heightFraction": 0.2, "text": "ITEM", "fontSize": 14.0, "fontFamily": "roboto_mono"},
      {"type": "text", "xFraction": 0.05, "yFraction": 0.25, "widthFraction": 0.9, "heightFraction": 0.45, "text": "000", "fontSize": 40.0, "fontFamily": "default"},
      {"type": "text", "xFraction": 0.05, "yFraction": 0.75, "widthFraction": 0.9, "heightFraction": 0.2, "text": "Note", "fontSize": 12.0, "fontFamily": "default"}
    ]
  }
]
```

- [ ] **Step 5: Run tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest --tests "com.avicennasis.bluepaper.ui.editor.TemplateManagerTest" -q`
Expected: All 5 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TemplateManager.kt \
       shared/src/commonTest/kotlin/com/avicennasis/bluepaper/ui/editor/TemplateManagerTest.kt \
       shared/src/commonMain/composeResources/files/templates.json
git commit -m "feat: TemplateManager with 6 built-in label templates"
```

---

## Task 8: EditorState Refactor (Element Model + Undo)

**Files:**
- Modify: `ui/editor/EditorState.kt`

This is the core state refactor. Replace flat text/image state with the elements list, integrate UndoManager, and wire up selection.

- [ ] **Step 1: Rewrite EditorState.kt**

Replace the entire contents of `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorState.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.config.LabelSize
import com.avicennasis.bluepaper.printer.PrinterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Keep ImageTransform for backward compat during migration
data class ImageTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

data class PrintProgress(
    val completed: Int,
    val total: Int,
    val isPrinting: Boolean,
    val error: String? = null,
)

class EditorState(
    private val transport: BleTransport,
    private val scope: CoroutineScope,
) {
    private val client = PrinterClient(transport)
    val undoManager = UndoManager()

    // --- Element model ---
    private val _elements = MutableStateFlow<List<LabelElement>>(emptyList())
    val elements: StateFlow<List<LabelElement>> = _elements

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId

    // --- Printer config ---
    private val _selectedModel = MutableStateFlow(DeviceRegistry.get("d110")!!)
    val selectedModel: StateFlow<DeviceConfig> = _selectedModel

    private val _selectedLabelSize = MutableStateFlow(_selectedModel.value.labelSizes.first())
    val selectedLabelSize: StateFlow<LabelSize> = _selectedLabelSize

    private val _density = MutableStateFlow(3)
    val density: StateFlow<Int> = _density

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    private val _printProgress = MutableStateFlow(PrintProgress(0, 0, false))
    val printProgress: StateFlow<PrintProgress> = _printProgress

    // --- Grid ---
    private val _gridSize = MutableStateFlow(DEFAULT_GRID_SIZE)
    val gridSize: StateFlow<Float> = _gridSize

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid

    // --- Undo state flows ---
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoState() {
        _canUndo.value = undoManager.canUndo
        _canRedo.value = undoManager.canRedo
    }

    private fun saveUndoSnapshot() {
        undoManager.save(_elements.value)
        updateUndoState()
    }

    fun undo() {
        val restored = undoManager.undo(_elements.value) ?: return
        _elements.value = restored
        updateUndoState()
    }

    fun redo() {
        val restored = undoManager.redo(_elements.value) ?: return
        _elements.value = restored
        updateUndoState()
    }

    // --- Selection ---
    fun selectElement(id: String?) { _selectedElementId.value = id }

    fun selectedElement(): LabelElement? =
        _elements.value.find { it.id == _selectedElementId.value }

    // --- Add elements ---
    private var nextId = 0
    private fun newId(prefix: String) = "${prefix}_${nextId++}"

    fun addTextElement() {
        saveUndoSnapshot()
        val el = LabelElement.TextElement(id = newId("text"))
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun addImageElement(bitmap: ImageBitmap) {
        saveUndoSnapshot()
        val el = LabelElement.ImageElement(
            id = newId("img"),
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            bitmap = bitmap,
        )
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun removeElement(id: String) {
        saveUndoSnapshot()
        _elements.value = _elements.value.filter { it.id != id }
        if (_selectedElementId.value == id) _selectedElementId.value = null
    }

    // --- Element mutations ---
    private fun updateElement(id: String, transform: (LabelElement) -> LabelElement) {
        _elements.value = _elements.value.map { if (it.id == id) transform(it) else it }
    }

    fun moveElement(id: String, x: Float, y: Float) {
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y)
                is LabelElement.ImageElement -> el.copy(x = x, y = y)
            }
        }
    }

    fun moveElementDone(id: String) { saveUndoSnapshot() }

    fun resizeElement(id: String, width: Float, height: Float) {
        val w = width.coerceAtLeast(MIN_ELEMENT_SIZE)
        val h = height.coerceAtLeast(MIN_ELEMENT_SIZE)
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(width = w, height = h)
                is LabelElement.ImageElement -> el.copy(width = w, height = h)
            }
        }
    }

    fun resizeElementDone(id: String) { saveUndoSnapshot() }

    fun setElementPosition(id: String, x: Float, y: Float, width: Float, height: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y, width = width, height = height)
                is LabelElement.ImageElement -> el.copy(x = x, y = y, width = width, height = height)
            }
        }
    }

    fun setElementRotation(id: String, degrees: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(rotation = degrees)
                is LabelElement.ImageElement -> el.copy(rotation = degrees)
            }
        }
    }

    // --- Text-specific ---
    fun setTextContent(id: String, text: String) {
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(text = text) else el
        }
    }

    fun setTextContentDone(id: String) { saveUndoSnapshot() }

    fun setFontSize(id: String, size: Float) {
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontSize = size) else el
        }
    }

    fun setFontSizeDone(id: String) { saveUndoSnapshot() }

    fun setFontFamily(id: String, fontFamily: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontFamily = fontFamily) else el
        }
    }

    // --- Image-specific ---
    fun setImageScale(id: String, scale: Float) {
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(scale = scale.coerceIn(0.1f, 5f)) else el
        }
    }

    fun setImageScaleDone(id: String) { saveUndoSnapshot() }

    fun toggleImageFlipH(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(flipH = !el.flipH) else el
        }
    }

    fun toggleImageFlipV(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(flipV = !el.flipV) else el
        }
    }

    fun rotateImage90(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(rotation = (el.rotation + 90f) % 360f) else el
        }
    }

    // --- Printer config ---
    fun setDensity(d: Int) { _density.value = d.coerceIn(1, _selectedModel.value.maxDensity) }
    fun setQuantity(q: Int) { _quantity.value = q.coerceIn(1, 100) }

    fun selectModel(modelName: String) {
        val config = DeviceRegistry.get(modelName) ?: return
        _selectedModel.value = config
        _selectedLabelSize.value = config.labelSizes.first()
        _density.value = _density.value.coerceAtMost(config.maxDensity)
    }

    fun selectLabelSize(size: LabelSize) { _selectedLabelSize.value = size }

    // --- Grid ---
    fun setGridSize(size: Float) { _gridSize.value = size }
    fun toggleGrid() { _showGrid.value = !_showGrid.value }

    // --- Templates ---
    fun applyTemplate(template: LabelTemplate) {
        saveUndoSnapshot()
        val labelSize = _selectedLabelSize.value
        _elements.value = TemplateManager.applyTemplate(template, labelSize.widthPx, labelSize.heightPx)
        _selectedElementId.value = null
    }

    // --- Save/Load ---
    fun toDesign(): LabelDesign = LabelDesign(
        version = 2,
        model = _selectedModel.value.model,
        labelWidthMm = _selectedLabelSize.value.widthMm,
        labelHeightMm = _selectedLabelSize.value.heightMm,
        density = _density.value,
        quantity = _quantity.value,
        elements = _elements.value.map { it.toSerializable() },
    )

    fun loadDesign(design: LabelDesign) {
        saveUndoSnapshot()
        val migrated = design.migrateToV2()
        selectModel(migrated.model)
        val matchingSize = _selectedModel.value.labelSizes.find {
            it.widthMm == migrated.labelWidthMm && it.heightMm == migrated.labelHeightMm
        }
        if (matchingSize != null) _selectedLabelSize.value = matchingSize
        _density.value = migrated.density
        _quantity.value = migrated.quantity
        _elements.value = migrated.elements.map { it.toLabelElement() }
        _selectedElementId.value = null
    }

    // --- Print ---
    fun print(imageRows: List<ByteArray>, width: Int, height: Int) {
        val config = _selectedModel.value
        val qty = _quantity.value
        val dens = _density.value

        _printProgress.value = PrintProgress(0, qty, true)

        scope.launch {
            try {
                client.print(
                    imageRows = imageRows,
                    width = width,
                    height = height,
                    density = dens,
                    quantity = qty,
                    isV2 = config.isV2,
                    onProgress = { completed, total ->
                        _printProgress.value = PrintProgress(completed, total, true)
                    },
                )
                _printProgress.value = PrintProgress(qty, qty, false)
            } catch (e: Exception) {
                _printProgress.value = PrintProgress(0, 0, false, error = e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Run all tests to check nothing broke**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All existing tests PASS. The `LabelDesignTest` uses `LabelDesign()` constructor which still works (v2 defaults). The `ImageTransform` data class is kept for backward compat.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorState.kt
git commit -m "refactor: EditorState — element model, undo integration, selection"
```

---

## Task 9: LabelCanvas Refactor (Draw Elements + Selection)

**Files:**
- Modify: `ui/editor/LabelCanvas.kt`

- [ ] **Step 1: Rewrite LabelCanvas.kt to draw from elements list**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelCanvas.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun LabelCanvas(
    elements: List<LabelElement>,
    selectedElementId: String?,
    widthPx: Int,
    heightPx: Int,
    textMeasurer: TextMeasurer,
    showGrid: Boolean = false,
    gridSize: Float = DEFAULT_GRID_SIZE,
    modifier: Modifier = Modifier,
) {
    if (widthPx <= 0 || heightPx <= 0) return
    val ratio = widthPx.toFloat() / heightPx.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        val scaleFactor = min(size.width / widthPx, size.height / heightPx)

        // White paper background
        drawRect(Color.White)

        // Grid overlay
        if (showGrid && gridSize > 0f) {
            drawGrid(widthPx, heightPx, gridSize, scaleFactor)
        }

        // Draw all elements
        for (element in elements) {
            drawElement(element, scaleFactor, textMeasurer)
        }

        // Selection box + handles
        val selected = elements.find { it.id == selectedElementId }
        if (selected != null) {
            drawSelectionBox(selected, scaleFactor)
            drawResizeHandles(selected, scaleFactor)
        }

        // Canvas border
        drawRect(Color.LightGray, style = Stroke(1f))
    }
}

private fun DrawScope.drawElement(
    element: LabelElement,
    scaleFactor: Float,
    textMeasurer: TextMeasurer,
) {
    when (element) {
        is LabelElement.TextElement -> drawTextElement(element, scaleFactor, textMeasurer)
        is LabelElement.ImageElement -> drawImageElement(element, scaleFactor)
    }
}

private fun DrawScope.drawTextElement(
    el: LabelElement.TextElement,
    scaleFactor: Float,
    textMeasurer: TextMeasurer,
) {
    if (el.text.isEmpty()) return

    val fontFamily = FontRegistry.get(el.fontFamily)
    val screenX = el.x * scaleFactor
    val screenY = el.y * scaleFactor
    val maxWidth = (el.width * scaleFactor).toInt().coerceAtLeast(1).let {
        if (it <= 1 && el.width == 0f) (size.width - screenX).toInt().coerceAtLeast(1) else it
    }

    val textLayout = textMeasurer.measure(
        text = el.text,
        style = TextStyle(
            fontSize = (el.fontSize * scaleFactor).sp,
            color = Color.Black,
            fontFamily = fontFamily,
        ),
        constraints = Constraints(maxWidth = maxWidth),
        overflow = TextOverflow.Clip,
        softWrap = true,
    )

    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(screenX, screenY),
    )
}

private fun DrawScope.drawImageElement(
    el: LabelElement.ImageElement,
    scaleFactor: Float,
) {
    val img = el.bitmap ?: return
    val screenX = el.x * scaleFactor
    val screenY = el.y * scaleFactor
    val displayW = (el.width * scaleFactor * el.scale).toInt()
    val displayH = (el.height * scaleFactor * el.scale).toInt()

    withTransform({
        translate(screenX, screenY)
        if (el.rotation != 0f) {
            rotate(el.rotation, pivot = Offset(displayW / 2f, displayH / 2f))
        }
        scale(
            scaleX = if (el.flipH) -1f else 1f,
            scaleY = if (el.flipV) -1f else 1f,
            pivot = Offset(displayW / 2f, displayH / 2f),
        )
    }) {
        drawImage(
            image = img,
            dstSize = IntSize(displayW, displayH),
        )
    }
}

private fun DrawScope.drawSelectionBox(element: LabelElement, scaleFactor: Float) {
    val x = element.x * scaleFactor
    val y = element.y * scaleFactor
    val w = element.width * scaleFactor
    val h = element.height * scaleFactor

    drawRect(
        color = Color(0xFF42A5F5),
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f),
        ),
    )
}

private fun DrawScope.drawResizeHandles(element: LabelElement, scaleFactor: Float) {
    val x = element.x * scaleFactor
    val y = element.y * scaleFactor
    val w = element.width * scaleFactor
    val h = element.height * scaleFactor
    val hs = HANDLE_SIZE_DP * 2  // handle size in screen px

    val handlePositions = listOf(
        Offset(x, y), Offset(x + w / 2, y), Offset(x + w, y),
        Offset(x, y + h / 2), Offset(x + w, y + h / 2),
        Offset(x, y + h), Offset(x + w / 2, y + h), Offset(x + w, y + h),
    )

    for (pos in handlePositions) {
        drawRect(
            color = Color.White,
            topLeft = Offset(pos.x - hs / 2, pos.y - hs / 2),
            size = Size(hs, hs),
        )
        drawRect(
            color = Color(0xFF42A5F5),
            topLeft = Offset(pos.x - hs / 2, pos.y - hs / 2),
            size = Size(hs, hs),
            style = Stroke(2f),
        )
    }
}

private fun DrawScope.drawGrid(widthPx: Int, heightPx: Int, gridSize: Float, scaleFactor: Float) {
    val gridColor = Color(0x20000000)
    var gx = gridSize
    while (gx < widthPx) {
        drawLine(gridColor, Offset(gx * scaleFactor, 0f), Offset(gx * scaleFactor, heightPx * scaleFactor))
        gx += gridSize
    }
    var gy = gridSize
    while (gy < heightPx) {
        drawLine(gridColor, Offset(0f, gy * scaleFactor), Offset(widthPx * scaleFactor, gy * scaleFactor))
        gy += gridSize
    }
}

/**
 * Draw all elements to a DrawScope for rendering (used by LabelRenderer).
 * This version draws at 1:1 label pixel scale without selection UI.
 */
fun drawElementsForPrint(
    scope: DrawScope,
    elements: List<LabelElement>,
    textMeasurer: TextMeasurer,
) {
    scope.drawRect(Color.White)
    for (element in elements) {
        scope.drawElement(element, scaleFactor = 1f, textMeasurer)
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS (LabelCanvas is a composable — no direct unit tests on it, but existing tests verify the underlying logic).

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelCanvas.kt
git commit -m "refactor: LabelCanvas — draw from elements list, selection box, resize handles, grid"
```

---

## Task 10: LabelRenderer Refactor

**Files:**
- Modify: `image/LabelRenderer.kt`

Update LabelRenderer to accept an elements list for print rendering.

- [ ] **Step 1: Update LabelRenderer.kt**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/image/LabelRenderer.kt`:

```kotlin
package com.avicennasis.bluepaper.image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

object LabelRenderer {

    /**
     * Render with a custom draw lambda (original API — kept for backward compat and tests).
     */
    fun render(
        width: Int,
        height: Int,
        rotationDegrees: Int = 0,
        horizontalOffset: Int = 0,
        verticalOffset: Int = 0,
        draw: (DrawScope) -> Unit,
    ): List<ByteArray> {
        val bitmap = ImageBitmap(width, height)
        val canvas = Canvas(bitmap)
        val scope = CanvasDrawScope()
        scope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
        ) {
            draw(this)
        }

        var pixels = IntArray(width * height)
        bitmap.readPixels(
            buffer = pixels,
            startX = 0,
            startY = 0,
            width = width,
            height = height,
            bufferOffset = 0,
            stride = width,
        )

        val (rotatedPixels, rotatedWidth, rotatedHeight) =
            MonochromeEncoder.rotatePixels(pixels, width, height, rotationDegrees)

        return MonochromeEncoder.encode(rotatedPixels, rotatedWidth, rotatedHeight, horizontalOffset, verticalOffset)
    }
}
```

No changes needed — the original `render()` with a draw lambda is exactly what we need. `EditorScreen` will call it with `drawElementsForPrint()` from `LabelCanvas.kt` as the draw lambda. The API is already correct.

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS.

- [ ] **Step 3: Commit (skip if no changes)**

If no changes were needed, skip this commit.

---

## Task 11: UI Composables — ToolboxPanel (Left Panel)

**Files:**
- Create: `ui/editor/ElementList.kt`
- Create: `ui/editor/ToolboxPanel.kt`

- [ ] **Step 1: Create ElementList.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ElementList.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ElementList(
    elements: List<LabelElement>,
    selectedElementId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(elements, key = { it.id }) { element ->
            val isSelected = element.id == selectedElementId
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(element.id) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(
                        text = when (element) {
                            is LabelElement.TextElement -> "T"
                            is LabelElement.ImageElement -> "I"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (element) {
                            is LabelElement.TextElement -> element.text.take(20)
                            is LabelElement.ImageElement -> "Image"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { onDelete(element.id) },
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        Text("X", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create ToolboxPanel.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ToolboxPanel.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.config.LabelSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolboxPanel(
    elements: List<LabelElement>,
    selectedElementId: String?,
    selectedModel: DeviceConfig,
    selectedLabelSize: LabelSize,
    density: Int,
    quantity: Int,
    onSelectElement: (String) -> Unit,
    onDeleteElement: (String) -> Unit,
    onAddText: () -> Unit,
    onAddImage: (ImageBitmap) -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectLabelSize: (LabelSize) -> Unit,
    onDensityChange: (Int) -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight()
            .padding(8.dp),
    ) {
        // Add element buttons
        Text("Elements", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onAddText, modifier = Modifier.weight(1f)) {
                Text("+ Text")
            }
            ImagePickerButton(onImageLoaded = onAddImage)
        }

        Spacer(Modifier.height(8.dp))

        // Element list
        ElementList(
            elements = elements,
            selectedElementId = selectedElementId,
            onSelect = onSelectElement,
            onDelete = onDeleteElement,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Printer settings (scrollable)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("Printer", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            // Model dropdown
            var modelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                OutlinedTextField(
                    value = selectedModel.model.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                    DeviceRegistry.models().forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.uppercase()) },
                            onClick = { onSelectModel(model); modelExpanded = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Label size chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedModel.labelSizes.forEach { size ->
                    FilterChip(
                        selected = size == selectedLabelSize,
                        onClick = { onSelectLabelSize(size) },
                        label = { Text(size.displayName, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Density
            Text("Density: $density", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = density.toFloat(),
                onValueChange = { onDensityChange(it.toInt()) },
                valueRange = 1f..selectedModel.maxDensity.toFloat(),
                steps = selectedModel.maxDensity - 2,
            )

            // Copies
            Text("Copies: $quantity", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = quantity.toFloat(),
                onValueChange = { onQuantityChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ElementList.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ToolboxPanel.kt
git commit -m "feat: ToolboxPanel — left panel with element list and printer settings"
```

---

## Task 12: UI Composables — PropertiesPanel (Right Panel)

**Files:**
- Create: `ui/editor/PropertiesPanel.kt`
- Create: `ui/editor/FontPicker.kt`

- [ ] **Step 1: Create FontPicker.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/FontPicker.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPicker(
    selectedFontKey: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = FontRegistry.nameFor(selectedFontKey),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(
                fontFamily = FontRegistry.get(selectedFontKey),
                fontSize = 14.sp,
            ),
            label = { Text("Font") },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (category in FontRegistry.categories()) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                for (font in FontRegistry.fontsInCategory(category)) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.displayName,
                                fontFamily = font.family,
                            )
                        },
                        onClick = {
                            onFontSelected(font.key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create PropertiesPanel.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PropertiesPanel.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PropertiesPanel(
    selectedElement: LabelElement?,
    onPositionChange: (String, Float, Float, Float, Float) -> Unit,
    onRotationChange: (String, Float) -> Unit,
    onTextChange: (String, String) -> Unit,
    onTextChangeDone: (String) -> Unit,
    onFontSizeChange: (String, Float) -> Unit,
    onFontSizeChangeDone: (String) -> Unit,
    onFontFamilyChange: (String, String) -> Unit,
    onImageScaleChange: (String, Float) -> Unit,
    onImageScaleChangeDone: (String) -> Unit,
    onFlipH: (String) -> Unit,
    onFlipV: (String) -> Unit,
    onRotateImage90: (String) -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (selectedElement != null) {
            Text("Properties", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            // Common position fields
            Text("Position", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField("X", selectedElement.x, modifier = Modifier.weight(1f)) { newX ->
                    onPositionChange(selectedElement.id, newX, selectedElement.y, selectedElement.width, selectedElement.height)
                }
                NumericField("Y", selectedElement.y, modifier = Modifier.weight(1f)) { newY ->
                    onPositionChange(selectedElement.id, selectedElement.x, newY, selectedElement.width, selectedElement.height)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Size", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField("W", selectedElement.width, modifier = Modifier.weight(1f)) { newW ->
                    onPositionChange(selectedElement.id, selectedElement.x, selectedElement.y, newW, selectedElement.height)
                }
                NumericField("H", selectedElement.height, modifier = Modifier.weight(1f)) { newH ->
                    onPositionChange(selectedElement.id, selectedElement.x, selectedElement.y, selectedElement.width, newH)
                }
            }
            Spacer(Modifier.height(4.dp))
            NumericField("Rotation", selectedElement.rotation, modifier = Modifier.fillMaxWidth()) { deg ->
                onRotationChange(selectedElement.id, deg)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Type-specific properties
            when (selectedElement) {
                is LabelElement.TextElement -> {
                    Text("Text", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedElement.text,
                        onValueChange = { onTextChange(selectedElement.id, it) },
                        label = { Text("Label Text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 5,
                    )

                    Spacer(Modifier.height(8.dp))

                    FontPicker(
                        selectedFontKey = selectedElement.fontFamily,
                        onFontSelected = { onFontFamilyChange(selectedElement.id, it) },
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Font Size: ${selectedElement.fontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedElement.fontSize,
                        onValueChange = { onFontSizeChange(selectedElement.id, it) },
                        onValueChangeFinished = { onFontSizeChangeDone(selectedElement.id) },
                        valueRange = 8f..72f,
                    )
                }

                is LabelElement.ImageElement -> {
                    Text("Image", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))

                    Text("Scale: ${"%.1f".format(selectedElement.scale)}x", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedElement.scale,
                        onValueChange = { onImageScaleChange(selectedElement.id, it) },
                        onValueChangeFinished = { onImageScaleChangeDone(selectedElement.id) },
                        valueRange = 0.1f..5f,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { onRotateImage90(selectedElement.id) }) { Text("Rot 90") }
                        OutlinedButton(onClick = { onFlipH(selectedElement.id) }) {
                            Text(if (selectedElement.flipH) "H (on)" else "Flip H")
                        }
                        OutlinedButton(onClick = { onFlipV(selectedElement.id) }) {
                            Text(if (selectedElement.flipV) "V (on)" else "Flip V")
                        }
                    }
                }
            }
        } else {
            Text("No Selection", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text("Click an element on the canvas or add one from the left panel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))

        // Print button always at bottom
        Button(
            onClick = onPrint,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Print", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toInt().toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            newText.toFloatOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodySmall,
    )
}
```

- [ ] **Step 3: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/FontPicker.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PropertiesPanel.kt
git commit -m "feat: PropertiesPanel + FontPicker — right panel with element properties"
```

---

## Task 13: UI Composables — TopToolbar + TemplatePickerDialog

**Files:**
- Create: `ui/editor/TopToolbar.kt`
- Create: `ui/editor/TemplatePickerDialog.kt`

- [ ] **Step 1: Create TopToolbar.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TopToolbar.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.ui.theme.ThemeMode

@Composable
fun TopToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    themeMode: ThemeMode,
    showGrid: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTemplates: () -> Unit,
    onThemeToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left group
            TextButton(onClick = onSave) { Text("Save") }
            TextButton(onClick = onLoad) { Text("Load") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = onRedo, enabled = canRedo) { Text("Redo") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onTemplates) { Text("Templates") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onGridToggle) {
                Text(if (showGrid) "Grid On" else "Grid Off")
            }

            Spacer(Modifier.weight(1f))

            // Right group
            TextButton(onClick = onThemeToggle) {
                Text(
                    when (themeMode) {
                        ThemeMode.System -> "Auto"
                        ThemeMode.Light -> "Light"
                        ThemeMode.Dark -> "Dark"
                    }
                )
            }
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }
    }
}
```

- [ ] **Step 2: Create TemplatePickerDialog.kt**

Create `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TemplatePickerDialog.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TemplatePickerDialog(
    templates: List<LabelTemplate>,
    hasExistingElements: Boolean,
    onSelect: (LabelTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Template") },
        text = {
            Column {
                if (hasExistingElements) {
                    Text(
                        "This will replace your current design.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn {
                    items(templates) { template ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(template) }
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${template.elements.size} element(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 3: Verify compilation**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop -q`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TopToolbar.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TemplatePickerDialog.kt
git commit -m "feat: TopToolbar + TemplatePickerDialog composables"
```

---

## Task 14: EditorScreen Rewrite (Three-Panel Layout)

**Files:**
- Modify: `ui/editor/EditorScreen.kt`
- Modify: `ui/BluePaperApp.kt`
- Modify: `desktopApp/.../Main.kt`

This is the integration task that wires everything together.

- [ ] **Step 1: Rewrite EditorScreen.kt**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt`:

```kotlin
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.image.LabelRenderer
import com.avicennasis.bluepaper.ui.theme.ThemeMode

@Composable
fun EditorScreen(
    state: EditorState,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeToggle: () -> Unit = {},
    onDisconnect: () -> Unit,
) {
    val elements by state.elements.collectAsState()
    val selectedElementId by state.selectedElementId.collectAsState()
    val selectedModel by state.selectedModel.collectAsState()
    val selectedLabelSize by state.selectedLabelSize.collectAsState()
    val density by state.density.collectAsState()
    val quantity by state.quantity.collectAsState()
    val printProgress by state.printProgress.collectAsState()
    val canUndo by state.canUndo.collectAsState()
    val canRedo by state.canRedo.collectAsState()
    val showGrid by state.showGrid.collectAsState()
    val gridSize by state.gridSize.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    var showPrintDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Design, 1 = Preview

    val selectedElement = elements.find { it.id == selectedElementId }
    val focusRequester = remember { FocusRequester() }

    // Monochrome preview (recomputed when elements change)
    val monochromeRows = remember(elements, selectedLabelSize, selectedModel) {
        val w = selectedLabelSize.widthPx
        val h = selectedLabelSize.heightPx
        if (w <= 0 || h <= 0) emptyList()
        else LabelRenderer.render(w, h, rotationDegrees = selectedModel.rotation) { scope ->
            drawElementsForPrint(scope, elements, textMeasurer)
        }
    }
    val previewWidth = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.heightPx else selectedLabelSize.widthPx
    }
    val previewHeight = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.widthPx else selectedLabelSize.heightPx
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top toolbar
        TopToolbar(
            canUndo = canUndo,
            canRedo = canRedo,
            themeMode = themeMode,
            showGrid = showGrid,
            onSave = {
                val path = pickSaveFile("label.bpl")
                if (path != null) writeTextFile(path, LabelDesign.toJson(state.toDesign()))
            },
            onLoad = {
                val json = pickOpenFile()
                if (json != null) {
                    try { state.loadDesign(LabelDesign.fromJson(json)) } catch (_: Exception) { }
                }
            },
            onUndo = { state.undo() },
            onRedo = { state.redo() },
            onTemplates = { showTemplateDialog = true },
            onThemeToggle = onThemeToggle,
            onGridToggle = { state.toggleGrid() },
            onDisconnect = onDisconnect,
        )

        // Three-panel layout
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                ToolboxPanel(
                    elements = elements,
                    selectedElementId = selectedElementId,
                    selectedModel = selectedModel,
                    selectedLabelSize = selectedLabelSize,
                    density = density,
                    quantity = quantity,
                    onSelectElement = { state.selectElement(it) },
                    onDeleteElement = { state.removeElement(it) },
                    onAddText = { state.addTextElement() },
                    onAddImage = { state.addImageElement(it) },
                    onSelectModel = { state.selectModel(it) },
                    onSelectLabelSize = { state.selectLabelSize(it) },
                    onDensityChange = { state.setDensity(it) },
                    onQuantityChange = { state.setQuantity(it) },
                )
            }

            VerticalDivider()

            // Center panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && selectedElementId != null) {
                            val id = selectedElementId!!
                            val el = elements.find { it.id == id } ?: return@onKeyEvent false
                            val step = if (event.isShiftPressed) 10f else 1f
                            when (event.key) {
                                Key.DirectionLeft -> { state.moveElement(id, el.x - step, el.y); state.moveElementDone(id); true }
                                Key.DirectionRight -> { state.moveElement(id, el.x + step, el.y); state.moveElementDone(id); true }
                                Key.DirectionUp -> { state.moveElement(id, el.x, el.y - step); state.moveElementDone(id); true }
                                Key.DirectionDown -> { state.moveElement(id, el.x, el.y + step); state.moveElementDone(id); true }
                                Key.Delete, Key.Backspace -> { state.removeElement(id); true }
                                else -> false
                            }
                        } else false
                    },
            ) {
                // Tabs
                TabRow(selectedTabIndex = activeTab) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                        Text("Design", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                        Text("Print Preview", modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                when (activeTab) {
                    0 -> {
                        // Interactive design canvas
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(elements, selectedElementId) {
                                    detectTapGestures { offset ->
                                        val (lx, ly) = screenToLabel(
                                            offset.x, offset.y,
                                            size.width.toFloat(), size.height.toFloat(),
                                            selectedLabelSize.widthPx, selectedLabelSize.heightPx,
                                        )
                                        val hit = hitTest(elements, lx, ly)
                                        state.selectElement(hit?.id)
                                        focusRequester.requestFocus()
                                    }
                                }
                                .pointerInput(elements, selectedElementId) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            selectedElementId?.let { state.moveElementDone(it) }
                                        },
                                    ) { _, dragAmount ->
                                        val id = selectedElementId ?: return@detectDragGestures
                                        val el = elements.find { it.id == id } ?: return@detectDragGestures
                                        val (dx, dy) = screenDeltaToLabel(
                                            dragAmount.x, dragAmount.y,
                                            size.width.toFloat(), size.height.toFloat(),
                                            selectedLabelSize.widthPx, selectedLabelSize.heightPx,
                                        )
                                        val newX = if (gridSize > 0f) snapToGrid(el.x + dx, gridSize) else el.x + dx
                                        val newY = if (gridSize > 0f) snapToGrid(el.y + dy, gridSize) else el.y + dy
                                        state.moveElement(id, newX, newY)
                                    }
                                },
                        ) {
                            LabelCanvas(
                                elements = elements,
                                selectedElementId = selectedElementId,
                                widthPx = selectedLabelSize.widthPx,
                                heightPx = selectedLabelSize.heightPx,
                                textMeasurer = textMeasurer,
                                showGrid = showGrid,
                                gridSize = gridSize,
                            )
                        }
                    }
                    1 -> {
                        MonochromePreview(rows = monochromeRows, width = previewWidth)
                    }
                }
            }

            VerticalDivider()

            // Right panel
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                PropertiesPanel(
                    selectedElement = selectedElement,
                    onPositionChange = { id, x, y, w, h -> state.setElementPosition(id, x, y, w, h) },
                    onRotationChange = { id, deg -> state.setElementRotation(id, deg) },
                    onTextChange = { id, text -> state.setTextContent(id, text) },
                    onTextChangeDone = { id -> state.setTextContentDone(id) },
                    onFontSizeChange = { id, size -> state.setFontSize(id, size) },
                    onFontSizeChangeDone = { id -> state.setFontSizeDone(id) },
                    onFontFamilyChange = { id, family -> state.setFontFamily(id, family) },
                    onImageScaleChange = { id, scale -> state.setImageScale(id, scale) },
                    onImageScaleChangeDone = { id -> state.setImageScaleDone(id) },
                    onFlipH = { id -> state.toggleImageFlipH(id) },
                    onFlipV = { id -> state.toggleImageFlipV(id) },
                    onRotateImage90 = { id -> state.rotateImage90(id) },
                    onPrint = {
                        state.print(monochromeRows, previewWidth, previewHeight)
                        showPrintDialog = true
                    },
                )
            }
        }
    }

    // Dialogs
    if (showPrintDialog) {
        PrintDialog(progress = printProgress, onDismiss = { showPrintDialog = false })
    }

    if (showTemplateDialog) {
        TemplatePickerDialog(
            templates = TemplateManager.builtInTemplates(),
            hasExistingElements = elements.isNotEmpty(),
            onSelect = { template ->
                state.applyTemplate(template)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false },
        )
    }
}
```

- [ ] **Step 2: Update BluePaperApp.kt to pass theme params**

Replace `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt`:

```kotlin
package com.avicennasis.bluepaper.ui

import androidx.compose.runtime.*
import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ui.editor.EditorScreen
import com.avicennasis.bluepaper.ui.editor.EditorState
import com.avicennasis.bluepaper.ui.editor.ThemePreferences
import com.avicennasis.bluepaper.ui.scanner.ScannerScreen
import com.avicennasis.bluepaper.ui.scanner.ScannerState
import com.avicennasis.bluepaper.ui.theme.BluePaperTheme
import com.avicennasis.bluepaper.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope

enum class Screen { Scanner, Editor }

@Composable
fun BluePaperApp(
    scanner: BleScanner,
    transport: BleTransport,
    scope: CoroutineScope,
    startScreen: Screen = Screen.Scanner,
) {
    var currentScreen by remember { mutableStateOf(startScreen) }
    var themeMode by remember { mutableStateOf(ThemePreferences.load()) }

    val scannerState = remember { ScannerState(scanner, transport, scope) }
    val editorState = remember { EditorState(transport, scope) }

    BluePaperTheme(themeMode = themeMode) {
        when (currentScreen) {
            Screen.Scanner -> ScannerScreen(
                state = scannerState,
                onConnected = { currentScreen = Screen.Editor },
            )
            Screen.Editor -> EditorScreen(
                state = editorState,
                themeMode = themeMode,
                onThemeToggle = {
                    themeMode = when (themeMode) {
                        ThemeMode.System -> ThemeMode.Light
                        ThemeMode.Light -> ThemeMode.Dark
                        ThemeMode.Dark -> ThemeMode.System
                    }
                    ThemePreferences.save(themeMode)
                },
                onDisconnect = {
                    scannerState.disconnect()
                    currentScreen = Screen.Scanner
                },
            )
        }
    }
}
```

- [ ] **Step 3: Update desktop Main.kt window size**

Edit `desktopApp/src/desktopMain/kotlin/com/avicennasis/bluepaper/Main.kt` — change the window dimensions:

```kotlin
state = rememberWindowState(width = 1200.dp, height = 800.dp),
```

- [ ] **Step 4: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS.

- [ ] **Step 5: Run the desktop app and verify the three-panel layout**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :desktopApp:run`
Expected: Window opens with three-panel layout. Left panel has add buttons and printer settings. Center has tabbed canvas. Right has properties panel with Print button.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt \
       desktopApp/src/desktopMain/kotlin/com/avicennasis/bluepaper/Main.kt
git commit -m "feat: three-panel editor layout with toolbar, canvas interaction, theme toggle"
```

---

## Task 15: Cleanup Old Composables + Final Integration

**Files:**
- Delete: `ui/editor/ImageControls.kt`
- Delete: `ui/editor/TextControls.kt`
- Delete: `ui/editor/PrintSettings.kt`

- [ ] **Step 1: Remove superseded composables**

Delete these three files that are no longer used (their functionality is now in ToolboxPanel and PropertiesPanel):
- `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ImageControls.kt`
- `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TextControls.kt`
- `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PrintSettings.kt`

```bash
rm shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/ImageControls.kt \
   shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/TextControls.kt \
   shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PrintSettings.kt
```

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest -q`
Expected: All tests PASS.

- [ ] **Step 3: Run the desktop app for final verification**

Run: `ANDROID_HOME=~/Android/Sdk ./gradlew :desktopApp:run`
Expected: App launches with the full three-panel layout. Verify:
- Add text element works
- Click-to-select on canvas works
- Properties panel updates on selection
- Undo/redo buttons work
- Theme toggle cycles System → Light → Dark
- Templates dialog opens and applies templates
- Grid toggle shows/hides grid overlay
- Save/Load still works
- Print button triggers print dialog
- Tab switching between Design and Print Preview works

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: remove superseded ImageControls, TextControls, PrintSettings"
```

- [ ] **Step 5: Update version to 0.3.0**

Update README.md version references and any version constants. Update TODO.md to mark v0.3.0 items as complete.

```bash
git add README.md TODO.md
git commit -m "chore: bump version to 0.3.0"
```

---

## Dependency Graph

```
Task 1 (LabelElement) ─────┬──→ Task 2 (UndoManager)
                            ├──→ Task 3 (CanvasInteraction)
                            ├──→ Task 4 (LabelDesign v2)
                            ├──→ Task 7 (TemplateManager)
                            └──→ Task 8 (EditorState refactor)
                                      │
Task 5 (Theme) ──────────────────────→├──→ Task 14 (EditorScreen)
Task 6 (FontRegistry) ──────────────→│
Task 9 (LabelCanvas) ───────────────→│
Task 10 (LabelRenderer) ────────────→│
Task 11 (ToolboxPanel) ─────────────→│
Task 12 (PropertiesPanel) ──────────→│
Task 13 (TopToolbar + Templates) ───→│
                                      │
                                      └──→ Task 15 (Cleanup)
```

**Parallel groups:**
- Group A (no deps): Tasks 1
- Group B (depends on Task 1): Tasks 2, 3, 4, 6, 7
- Group C (no deps): Tasks 5
- Group D (depends on Tasks 1+8): Tasks 9, 10, 11, 12, 13
- Group E (depends on all): Tasks 8, 14
- Group F (depends on 14): Task 15
