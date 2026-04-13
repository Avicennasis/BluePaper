package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun CompactToolbox(
    onAddText: () -> Unit,
    onAddImage: (ImageBitmap) -> Unit,
    onAddBarcode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Text button
            FilledTonalIconButton(
                onClick = onAddText,
                modifier = Modifier.size(40.dp),
            ) {
                Text("T", style = MaterialTheme.typography.titleSmall)
            }

            // Image button — ImagePickerButton is an expect composable that
            // renders its own full-width button. Constrain it to 40dp so it
            // doesn't overflow the 48dp-wide toolbox.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                ImagePickerButton(onImageLoaded = onAddImage)
            }

            // Barcode button
            FilledTonalIconButton(
                onClick = onAddBarcode,
                modifier = Modifier.size(40.dp),
            ) {
                Text("B", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
