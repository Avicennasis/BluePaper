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
import androidx.compose.ui.graphics.ImageBitmap
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

@Composable
fun LabelCanvas(
    text: String,
    fontSize: Float,
    widthPx: Int,
    heightPx: Int,
    textMeasurer: TextMeasurer,
    importedImage: ImageBitmap? = null,
    imageTransform: ImageTransform = ImageTransform(),
    modifier: Modifier = Modifier,
) {
    val ratio = widthPx.toFloat() / heightPx.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        drawLabelContent(this, text, fontSize, textMeasurer, importedImage, imageTransform)
        drawRect(Color.LightGray, style = Stroke(1f))
    }
}

fun drawLabelContent(
    scope: DrawScope,
    text: String,
    fontSize: Float,
    textMeasurer: TextMeasurer,
    importedImage: ImageBitmap? = null,
    imageTransform: ImageTransform = ImageTransform(),
) {
    scope.drawRect(Color.White)

    // Draw imported image with transforms
    importedImage?.let { img ->
        scope.withTransform({
            translate(imageTransform.offsetX, imageTransform.offsetY)
            rotate(
                imageTransform.rotation,
                pivot = Offset(img.width * imageTransform.scale / 2f, img.height * imageTransform.scale / 2f),
            )
            scale(
                scaleX = imageTransform.scale * (if (imageTransform.flipH) -1f else 1f),
                scaleY = imageTransform.scale * (if (imageTransform.flipV) -1f else 1f),
                pivot = Offset(img.width * imageTransform.scale / 2f, img.height * imageTransform.scale / 2f),
            )
        }) {
            drawImage(
                image = img,
                dstSize = IntSize(
                    (img.width * imageTransform.scale).toInt(),
                    (img.height * imageTransform.scale).toInt(),
                ),
            )
        }
    }

    // Draw text on top of image, wrapping within label bounds
    if (text.isNotEmpty()) {
        val padding = 8f
        val maxWidth = (scope.size.width - padding * 2).toInt().coerceAtLeast(1)
        val textLayout = textMeasurer.measure(
            text = text,
            style = TextStyle(fontSize = fontSize.sp, color = Color.Black),
            constraints = Constraints(maxWidth = maxWidth),
            overflow = TextOverflow.Clip,
            softWrap = true,
        )
        scope.drawText(
            textLayoutResult = textLayout,
            topLeft = Offset(padding, padding),
        )
    }
}
