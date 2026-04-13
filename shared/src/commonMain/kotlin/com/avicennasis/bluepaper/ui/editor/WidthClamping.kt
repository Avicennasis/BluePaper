package com.avicennasis.bluepaper.ui.editor

fun clampToLabel(
    element: LabelElement,
    labelWidth: Int,
    labelHeight: Int,
): LabelElement {
    val w = element.width.coerceIn(MIN_ELEMENT_SIZE, labelWidth.toFloat())
    val h = element.height.coerceIn(MIN_ELEMENT_SIZE, labelHeight.toFloat())
    val x = element.x.coerceIn(0f, (labelWidth - w).coerceAtLeast(0f))
    val y = element.y.coerceIn(0f, (labelHeight - h).coerceAtLeast(0f))

    return when (element) {
        is LabelElement.TextElement -> element.copy(x = x, y = y, width = w, height = h)
        is LabelElement.ImageElement -> element.copy(x = x, y = y, width = w, height = h)
        is LabelElement.BarcodeElement -> element.copy(x = x, y = y, width = w, height = h)
    }
}

fun clampAllToLabel(
    elements: List<LabelElement>,
    labelWidth: Int,
    labelHeight: Int,
): List<LabelElement> = elements.map { clampToLabel(it, labelWidth, labelHeight) }
