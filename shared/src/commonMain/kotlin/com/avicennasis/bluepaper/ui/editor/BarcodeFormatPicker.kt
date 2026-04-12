package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BarcodeFormatPicker(
    onAdd: (BarcodeFormat, String, DataStandard, Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFormat by remember { mutableStateOf(BarcodeFormat.QR_CODE) }
    var selectedStandard by remember { mutableStateOf(DataStandard.RAW_TEXT) }
    var rawData by remember { mutableStateOf("") }
    var structuredFields by remember { mutableStateOf(mapOf<String, String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Barcode") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for ((category, formats) in BarcodeFormat.byCategory()) {
                    Text(category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    formats.forEach { format ->
                        val available = BarcodeRenderer.isFormatAvailable(format)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selectedFormat == format,
                                onClick = {
                                    selectedFormat = format
                                    selectedStandard = DataStandard.RAW_TEXT
                                    structuredFields = emptyMap()
                                },
                                enabled = available,
                            )
                            Text(
                                text = format.displayName + if (!available) " (unavailable)" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                val standards = DataStandard.forFormat(selectedFormat)
                if (standards.size > 1) {
                    Text("Data Standard", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    standards.forEach { std ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = selectedStandard == std,
                                onClick = { selectedStandard = std; structuredFields = emptyMap(); rawData = "" },
                            )
                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(std.displayName, style = MaterialTheme.typography.bodySmall)
                                Text(std.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (selectedStandard == DataStandard.RAW_TEXT) {
                    OutlinedTextField(
                        value = rawData,
                        onValueChange = { rawData = it },
                        label = { Text("Data") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                    )
                } else {
                    DataStandardForm(
                        standard = selectedStandard,
                        fields = structuredFields,
                        onFieldChange = { key, value -> structuredFields = structuredFields + (key to value) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val data = if (selectedStandard == DataStandard.RAW_TEXT) rawData
                else DataEncoderRegistry.encode(selectedStandard, structuredFields)
                onAdd(selectedFormat, data, selectedStandard, structuredFields)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
