package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-specific image picker button.
 * Opens a file dialog/gallery picker and returns the loaded ImageBitmap.
 */
@Composable
expect fun ImagePickerButton(
    onImageLoaded: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
)
