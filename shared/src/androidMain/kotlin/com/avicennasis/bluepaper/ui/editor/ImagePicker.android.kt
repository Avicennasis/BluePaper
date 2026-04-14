package com.avicennasis.bluepaper.ui.editor

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ImagePickerButton(
    onImageLoaded: (ImageBitmap) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    bitmap?.asImageBitmap()?.let(onImageLoaded)
                }
            } catch (_: Exception) { }
        }
    }

    OutlinedButton(
        onClick = { launcher.launch(arrayOf("image/*")) },
        modifier = modifier,
    ) {
        Text("+ Image")
    }
}
