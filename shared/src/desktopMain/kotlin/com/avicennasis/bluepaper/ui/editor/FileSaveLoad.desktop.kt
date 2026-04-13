package com.avicennasis.bluepaper.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
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
            // Show the Swing dialog on the EDT — JFileChooser is not thread-safe
            var selectedFile: File? = null
            SwingUtilities.invokeAndWait {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Save Label Design"
                    this.selectedFile = File(defaultName)
                    fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    var file = chooser.selectedFile
                    if (!file.name.endsWith(".bpl")) file = File(file.absolutePath + ".bpl")
                    selectedFile = file
                }
            }
            // Perform file I/O on the IO dispatcher
            val file = selectedFile
            if (file != null) {
                withContext(Dispatchers.IO) {
                    file.writeText(content)
                }
            }
        } catch (e: Exception) {
            println("[FileSaveLoad] Error saving file: ${e.message}")
        }
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
            // Show the Swing dialog on the EDT — JFileChooser is not thread-safe
            var selectedFile: File? = null
            SwingUtilities.invokeAndWait {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Open Label Design"
                    fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selectedFile = chooser.selectedFile
                }
            }
            // Perform file I/O on the IO dispatcher
            val file = selectedFile
            if (file != null) {
                val json = withContext(Dispatchers.IO) {
                    file.readText()
                }
                onLoaded(json)
            }
        } catch (e: Exception) {
            println("[FileSaveLoad] Error loading file: ${e.message}")
        }
        onDone()
    }
}
