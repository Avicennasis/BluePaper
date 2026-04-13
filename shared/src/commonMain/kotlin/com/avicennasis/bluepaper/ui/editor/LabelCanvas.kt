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

        drawRect(Color.White)

        if (showGrid && gridSize > 0f) {
            drawGrid(widthPx, heightPx, gridSize, scaleFactor)
        }

        for (element in elements) {
            drawElement(element, scaleFactor, textMeasurer)
        }

        val selected = elements.find { it.id == selectedElementId }
        if (selected != null) {
            drawSelectionBox(selected, scaleFactor)
            drawResizeHandles(selected, scaleFactor)
        }

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
        is LabelElement.BarcodeElement -> drawBarcodeElement(element, scaleFactor, textMeasurer)
    }
}

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

private fun DrawScope.drawTextElement(
    el: LabelElement.TextElement,
    scaleFactor: Float,
    textMeasurer: TextMeasurer,
) {
    if (el.text.isEmpty()) return

    val fontFamily = FontRegistry.get(el.fontFamily, el.fontWeight, el.fontStyle)
    val screenX = el.x * scaleFactor
    val screenY = el.y * scaleFactor
    val maxWidth = if (el.width > 0f) {
        (el.width * scaleFactor).toInt().coerceAtLeast(1)
    } else {
        (size.width - screenX).toInt().coerceAtLeast(1)
    }

    val textLayout = textMeasurer.measure(
        text = el.text,
        style = TextStyle(
            fontSize = (el.fontSize * scaleFactor).sp,
            color = Color.Black,
            fontFamily = fontFamily,
            fontWeight = androidx.compose.ui.text.font.FontWeight(el.fontWeight),
            fontStyle = if (el.fontStyle == "italic") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        ),
        constraints = Constraints(maxWidth = maxWidth),
        overflow = TextOverflow.Clip,
        softWrap = true,
    )

    drawText(textLayoutResult = textLayout, topLeft = Offset(screenX, screenY))
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
        drawImage(image = img, dstSize = IntSize(displayW, displayH))
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
        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)),
    )
}

private fun DrawScope.drawResizeHandles(element: LabelElement, scaleFactor: Float) {
    val x = element.x * scaleFactor
    val y = element.y * scaleFactor
    val w = element.width * scaleFactor
    val h = element.height * scaleFactor
    val hs = HANDLE_SIZE_DP * 2

    val handlePositions = listOf(
        Offset(x, y), Offset(x + w / 2, y), Offset(x + w, y),
        Offset(x, y + h / 2), Offset(x + w, y + h / 2),
        Offset(x, y + h), Offset(x + w / 2, y + h), Offset(x + w, y + h),
    )

    for (pos in handlePositions) {
        drawRect(Color.White, topLeft = Offset(pos.x - hs / 2, pos.y - hs / 2), size = Size(hs, hs))
        drawRect(Color(0xFF42A5F5), topLeft = Offset(pos.x - hs / 2, pos.y - hs / 2), size = Size(hs, hs), style = Stroke(2f))
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
