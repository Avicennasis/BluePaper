package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun ImageControls(
    importedImage: ImageBitmap?,
    imageTransform: ImageTransform,
    onImageLoaded: (ImageBitmap) -> Unit,
    onClear: () -> Unit,
    onOffsetChange: (Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onRotate90: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
) {
    Text("Image", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ImagePickerButton(onImageLoaded = onImageLoaded)
        if (importedImage != null) {
            OutlinedButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }

    if (importedImage != null) {
        Spacer(Modifier.height(8.dp))

        Text("Position X: ${imageTransform.offsetX.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = imageTransform.offsetX,
            onValueChange = { onOffsetChange(it, imageTransform.offsetY) },
            valueRange = -200f..500f,
        )
        Text("Position Y: ${imageTransform.offsetY.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = imageTransform.offsetY,
            onValueChange = { onOffsetChange(imageTransform.offsetX, it) },
            valueRange = -200f..500f,
        )

        Text("Scale: ${"%.1f".format(imageTransform.scale)}x", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = imageTransform.scale,
            onValueChange = onScaleChange,
            valueRange = 0.1f..3f,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRotate90) { Text("Rotate 90") }
            OutlinedButton(onClick = onFlipH) {
                Text(if (imageTransform.flipH) "Flip H (on)" else "Flip H")
            }
            OutlinedButton(onClick = onFlipV) {
                Text(if (imageTransform.flipV) "Flip V (on)" else "Flip V")
            }
        }
    }
}
