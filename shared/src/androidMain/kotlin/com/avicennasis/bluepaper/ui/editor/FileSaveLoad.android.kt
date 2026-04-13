package com.avicennasis.bluepaper.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) { }
        }
        onDone()
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            launcher.launch(defaultName)
        }
    }
}

@Composable
actual fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    onLoaded(json)
                }
            } catch (_: Exception) { }
        }
        onDone()
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            launcher.launch(arrayOf("application/json", "*/*"))
        }
    }
}
