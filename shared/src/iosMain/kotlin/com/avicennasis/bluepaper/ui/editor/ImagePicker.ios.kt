package com.avicennasis.bluepaper.ui.editor

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun ImagePickerButton(
    onImageLoaded: (ImageBitmap) -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = { /* TODO: iOS PHPicker */ },
        enabled = false,
        modifier = modifier,
    ) {
        Text("+ Image (iOS — coming soon)")
    }
}
