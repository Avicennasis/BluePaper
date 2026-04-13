package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable

@Composable
actual fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
) {
    // TODO: iOS document picker
    if (trigger) onDone()
}

@Composable
actual fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
) {
    // TODO: iOS document picker
    if (trigger) onDone()
}
