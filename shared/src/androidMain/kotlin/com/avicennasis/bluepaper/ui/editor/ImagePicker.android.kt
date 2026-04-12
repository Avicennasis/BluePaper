package com.avicennasis.bluepaper.ui.editor

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun ImagePickerButton(onImageLoaded: (ImageBitmap) -> Unit) {
    OutlinedButton(onClick = { /* TODO: Android gallery picker via ActivityResult */ }, enabled = false) {
        Text("Import Image (Android — coming soon)")
    }
}
