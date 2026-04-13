package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable

@Composable
expect fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
)

@Composable
expect fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
)
