package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.ui.theme.ThemeMode

@Composable
fun TopToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    themeMode: ThemeMode,
    showGrid: Boolean,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTemplates: () -> Unit,
    onSaveTemplate: () -> Unit,
    onThemeToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSave) { Text("Save") }
            TextButton(onClick = onLoad) { Text("Load") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = onRedo, enabled = canRedo) { Text("Redo") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onTemplates) { Text("Templates") }
            TextButton(onClick = onSaveTemplate) { Text("Save Tmpl") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onGridToggle) {
                Text(if (showGrid) "Grid On" else "Grid Off")
            }

            Spacer(Modifier.weight(1f))

            TextButton(onClick = onThemeToggle) {
                Text(
                    when (themeMode) {
                        ThemeMode.System -> "Auto"
                        ThemeMode.Light -> "Light"
                        ThemeMode.Dark -> "Dark"
                    }
                )
            }
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }
    }
}
