package com.avicennasis.bluepaper.ui.editor

import kotlin.math.min
import kotlin.math.roundToInt

// TODO: hitTest uses AABB and ignores element rotation.
// For rotated elements, click targets may not align with visual bounds.
// To fix: transform click coordinates by inverse rotation around element center.
fun hitTest(elements: List<LabelElement>, labelX: Float, labelY: Float): LabelElement? {
    for (element in elements.asReversed()) {
        val elScale = if (element is LabelElement.ImageElement) element.scale else 1f
        if (labelX >= element.x && labelX <= element.x + element.width * elScale &&
            labelY >= element.y && labelY <= element.y + element.height * elScale
        ) {
            return element
        }
    }
    return null
}

fun snapToGrid(value: Float, gridSize: Float): Float {
    if (gridSize <= 0f) return value
    return (value / gridSize).roundToInt() * gridSize
}

fun screenToLabel(
    screenX: Float,
    screenY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    labelWidth: Int,
    labelHeight: Int,
): Pair<Float, Float> {
    if (labelWidth <= 0 || labelHeight <= 0 || canvasWidth <= 0f || canvasHeight <= 0f) {
        return Pair(0f, 0f)
    }
    val scaleFactor = min(canvasWidth / labelWidth, canvasHeight / labelHeight)
    return Pair(screenX / scaleFactor, screenY / scaleFactor)
}

fun screenDeltaToLabel(
    deltaX: Float,
    deltaY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    labelWidth: Int,
    labelHeight: Int,
): Pair<Float, Float> {
    if (labelWidth <= 0 || labelHeight <= 0 || canvasWidth <= 0f || canvasHeight <= 0f) {
        return Pair(0f, 0f)
    }
    val scaleFactor = min(canvasWidth / labelWidth, canvasHeight / labelHeight)
    return Pair(deltaX / scaleFactor, deltaY / scaleFactor)
}

const val HANDLE_SIZE_DP = 6f
const val MIN_ELEMENT_SIZE = 10f
const val DEFAULT_GRID_SIZE = 8f

// Note: hitTestHandle is not yet wired to pointer input handlers. Resize handles are drawn visually but not interactive.
enum class ResizeHandle {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
}

// Note: hitTestHandle is not yet wired to pointer input handlers. Resize handles are drawn visually but not interactive.
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
