package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File

@Composable
actual fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        try {
            val chooser = JFileChooser().apply {
                dialogTitle = "Save Label Design"
                selectedFile = File(defaultName)
                fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
            }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                var file = chooser.selectedFile
                if (!file.name.endsWith(".bpl")) file = File(file.absolutePath + ".bpl")
                file.writeText(content)
            }
        } catch (_: Exception) { }
        onDone()
    }
}

@Composable
actual fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
) {
    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        try {
            val chooser = JFileChooser().apply {
                dialogTitle = "Open Label Design"
                fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val json = chooser.selectedFile.readText()
                onLoaded(json)
            }
        } catch (_: Exception) { }
        onDone()
    }
}
