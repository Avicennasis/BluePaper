package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable

/** Platform-specific save file dialog. Returns the file path or null. */
expect fun pickSaveFile(defaultName: String): String?

/** Platform-specific open file dialog. Returns file contents or null. */
expect fun pickOpenFile(): String?

/** Write text to a file at the given path. */
expect fun writeTextFile(path: String, content: String)
