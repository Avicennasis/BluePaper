package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPicker(
    selectedFontKey: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = FontRegistry.nameFor(selectedFontKey),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = TextStyle(
                fontFamily = FontRegistry.get(selectedFontKey),
                fontSize = 14.sp,
            ),
            label = { Text("Font") },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (category in FontRegistry.categories()) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                for (font in FontRegistry.fontsInCategory(category)) {
                    DropdownMenuItem(
                        text = { Text(text = font.displayName, fontFamily = font.family) },
                        onClick = { onFontSelected(font.key); expanded = false },
                    )
                }
            }
        }
    }
}
