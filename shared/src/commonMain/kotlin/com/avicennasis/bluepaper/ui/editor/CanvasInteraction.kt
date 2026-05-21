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
    val elScale = if (element is LabelElement.ImageElement) element.scale else 1f
    val w = element.width * elScale
    val h = element.height * elScale
    val cx = element.x + w / 2f
    val cy = element.y + h / 2f
    val r = element.x + w
    val b = element.y + h

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

data class ElementBounds(val x: Float, val y: Float, val width: Float, val height: Float)

fun applyResize(
    element: LabelElement,
    handle: ResizeHandle,
    dx: Float,
    dy: Float,
    gridSize: Float,
): ElementBounds {
    val elScale = if (element is LabelElement.ImageElement) element.scale else 1f
    var x = element.x
    var y = element.y
    var w = element.width * elScale
    var h = element.height * elScale

    when (handle) {
        ResizeHandle.TOP_LEFT -> { x += dx; y += dy; w -= dx; h -= dy }
        ResizeHandle.TOP -> { y += dy; h -= dy }
        ResizeHandle.TOP_RIGHT -> { w += dx; y += dy; h -= dy }
        ResizeHandle.LEFT -> { x += dx; w -= dx }
        ResizeHandle.RIGHT -> { w += dx }
        ResizeHandle.BOTTOM_LEFT -> { x += dx; w -= dx; h += dy }
        ResizeHandle.BOTTOM -> { h += dy }
        ResizeHandle.BOTTOM_RIGHT -> { w += dx; h += dy }
    }

    if (w < MIN_ELEMENT_SIZE) {
        if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.LEFT || handle == ResizeHandle.BOTTOM_LEFT) {
            x = element.x + element.width * elScale - MIN_ELEMENT_SIZE
        }
        w = MIN_ELEMENT_SIZE
    }
    if (h < MIN_ELEMENT_SIZE) {
        if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP || handle == ResizeHandle.TOP_RIGHT) {
            y = element.y + element.height * elScale - MIN_ELEMENT_SIZE
        }
        h = MIN_ELEMENT_SIZE
    }

    if (gridSize > 0f) {
        x = snapToGrid(x, gridSize)
        y = snapToGrid(y, gridSize)
        w = snapToGrid(w, gridSize).coerceAtLeast(MIN_ELEMENT_SIZE)
        h = snapToGrid(h, gridSize).coerceAtLeast(MIN_ELEMENT_SIZE)
    }

    return ElementBounds(x, y, w / elScale, h / elScale)
}
