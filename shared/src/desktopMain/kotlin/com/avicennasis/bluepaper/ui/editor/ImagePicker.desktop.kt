package com.avicennasis.bluepaper.ui.editor

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun ImagePickerButton(
    onImageLoaded: (ImageBitmap) -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Images (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp", "gif")
            chooser.dialogTitle = "Import Image"
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val file = chooser.selectedFile
                loadImageFile(file)?.let(onImageLoaded)
            }
        },
        modifier = modifier,
    ) {
        Text("+ Image")
    }
}

private fun loadImageFile(file: File): ImageBitmap? {
    return try {
        val bytes = file.readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
