package com.avicennasis.bluepaper.ui.editor

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun ImagePickerButton(onImageLoaded: (ImageBitmap) -> Unit) {
    OutlinedButton(onClick = { /* TODO: iOS PHPicker */ }, enabled = false) {
        Text("Import Image (iOS — coming soon)")
    }
}
