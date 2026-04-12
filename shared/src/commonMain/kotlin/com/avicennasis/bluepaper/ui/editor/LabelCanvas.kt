package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LabelCanvas(
    text: String,
    fontSize: Float,
    widthPx: Int,
    heightPx: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val ratio = widthPx.toFloat() / heightPx.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        drawRect(Color.White)

        if (text.isNotEmpty()) {
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(8f, 8f),
                style = TextStyle(fontSize = fontSize.sp, color = Color.Black),
            )
        }

        drawRect(Color.LightGray, style = Stroke(1f))
    }
}

fun drawLabelContent(
    scope: DrawScope,
    text: String,
    fontSize: Float,
    textMeasurer: TextMeasurer,
) {
    scope.drawRect(Color.White)
    if (text.isNotEmpty()) {
        scope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset(8f, 8f),
            style = TextStyle(fontSize = fontSize.sp, color = Color.Black),
        )
    }
}
