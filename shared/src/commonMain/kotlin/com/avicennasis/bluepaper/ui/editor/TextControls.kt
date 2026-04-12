package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TextControls(
    labelText: String,
    fontSize: Float,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
) {
    Text("Text", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = labelText,
        onValueChange = onTextChange,
        label = { Text("Label Text") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        minLines = 2,
        maxLines = 5,
    )

    Spacer(Modifier.height(8.dp))

    Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
    Slider(
        value = fontSize,
        onValueChange = onFontSizeChange,
        valueRange = 8f..72f,
    )
}
