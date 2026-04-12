package com.avicennasis.bluepaper.ui.editor

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun pickSaveFile(defaultName: String): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Save Label Design"
    chooser.selectedFile = File(defaultName)
    chooser.fileFilter = FileNameExtensionFilter("BluePaper Label (*.bpl)", "bpl")
    return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        var path = chooser.selectedFile.absolutePath
        if (!path.endsWith(".bpl")) path += ".bpl"
        path
    } else null
}

actual fun pickOpenFile(): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle = "Open Label Design"
    chooser.fileFilter = FileNameExtensionFilter("BluePaper Label (*.bpl)", "bpl")
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.readText()
    } else null
}

actual fun writeTextFile(path: String, content: String) {
    File(path).writeText(content)
}
