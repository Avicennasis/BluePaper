package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
) {
    // TODO: iOS document picker
    LaunchedEffect(trigger) {
        if (trigger) onDone()
    }
}

@Composable
actual fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
) {
    // TODO: iOS document picker
    LaunchedEffect(trigger) {
        if (trigger) onDone()
    }
}
