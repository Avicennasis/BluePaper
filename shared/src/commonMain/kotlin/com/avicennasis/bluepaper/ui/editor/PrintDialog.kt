package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrintDialog(
    progress: PrintProgress,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!progress.isPrinting) onDismiss() },
        title = {
            Text(
                when {
                    progress.isPrinting -> "Printing..."
                    progress.error != null -> "Print Error"
                    else -> "Print Complete"
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when {
                    progress.isPrinting -> {
                        LinearProgressIndicator(
                            progress = { if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Page ${progress.completed} of ${progress.total}")
                    }
                    progress.error != null -> {
                        Text(progress.error, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text("All ${progress.total} labels printed successfully.")
                    }
                }
            }
        },
        confirmButton = {
            if (!progress.isPrinting) {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
    )
}
