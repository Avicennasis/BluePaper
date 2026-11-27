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
            val file: File? = runOnEdt {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Save Label Design"
                    selectedFile = File(defaultName)
                    fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val chosen = chooser.selectedFile
                    if (chosen.name.endsWith(".bpl")) chosen else File(chosen.absolutePath + ".bpl")
                } else {
                    null
                }
            }
            // Perform file I/O on the IO dispatcher
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
            val file: File? = runOnEdt {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Open Label Design"
                    fileFilter = FileNameExtensionFilter("BluePaper Label (.bpl)", "bpl")
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
            }
            // Perform file I/O on the IO dispatcher
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

/**
 * Runs [block] on the Swing EDT and returns its result. Compose Desktop's LaunchedEffect
 * already runs ON the EDT, where SwingUtilities.invokeAndWait throws
 * "Cannot call invokeAndWait from the event dispatcher thread" — so run inline there.
 * (Modal JFileChooser dialogs pump events themselves, so blocking the EDT here is fine.)
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> runOnEdt(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return block()
    var result: Any? = null
    SwingUtilities.invokeAndWait { result = block() }
    return result as T
}

