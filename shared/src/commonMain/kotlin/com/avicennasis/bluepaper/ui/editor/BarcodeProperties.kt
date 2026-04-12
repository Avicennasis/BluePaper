package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeProperties(
    element: LabelElement.BarcodeElement,
    onFormatChange: (String, BarcodeFormat) -> Unit,
    onDataChange: (String, String) -> Unit,
    onDataChangeDone: (String) -> Unit,
    onErrorCorrectionChange: (String, ErrorCorrection) -> Unit,
    onDataStandardChange: (String, DataStandard) -> Unit,
    onStructuredDataChange: (String, Map<String, String>) -> Unit,
    onStructuredDataChangeDone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Barcode", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        // Format dropdown
        var formatExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = it }) {
            OutlinedTextField(
                value = element.format.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Format") },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                textStyle = MaterialTheme.typography.bodySmall,
            )
            ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                for ((category, formats) in BarcodeFormat.byCategory()) {
                    Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                    formats.forEach { fmt ->
                        val available = BarcodeRenderer.isFormatAvailable(fmt)
                        DropdownMenuItem(
                            text = { Text(fmt.displayName) },
                            onClick = { onFormatChange(element.id, fmt); formatExpanded = false },
                            enabled = available,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Data standard dropdown
        val standards = DataStandard.forFormat(element.format)
        if (standards.size > 1) {
            var stdExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = stdExpanded, onExpandedChange = { stdExpanded = it }) {
                OutlinedTextField(
                    value = element.dataStandard.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data Standard") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stdExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = stdExpanded, onDismissRequest = { stdExpanded = false }) {
                    standards.forEach { std ->
                        DropdownMenuItem(
                            text = { Text("${std.displayName} — ${std.description}") },
                            onClick = { onDataStandardChange(element.id, std); stdExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Data entry
        if (element.dataStandard == DataStandard.RAW_TEXT) {
            OutlinedTextField(
                value = element.data,
                onValueChange = { onDataChange(element.id, it) },
                label = { Text("Data") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false, minLines = 2, maxLines = 5,
            )
        } else {
            DataStandardForm(
                standard = element.dataStandard,
                fields = element.structuredData,
                onFieldChange = { key, value ->
                    onStructuredDataChange(element.id, element.structuredData + (key to value))
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        // Error correction (QR only)
        if (element.format == BarcodeFormat.QR_CODE) {
            var ecExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = ecExpanded, onExpandedChange = { ecExpanded = it }) {
                OutlinedTextField(
                    value = element.errorCorrection.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Error Correction") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ecExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = ecExpanded, onDismissRequest = { ecExpanded = false }) {
                    ErrorCorrection.entries.forEach { ec ->
                        DropdownMenuItem(text = { Text(ec.displayName) }, onClick = { onErrorCorrectionChange(element.id, ec); ecExpanded = false })
                    }
                }
            }
        }

        // Validation
        Spacer(Modifier.height(8.dp))
        if (element.data.isNotEmpty()) {
            val validation = BarcodeValidator.validate(element.format, element.data)
            Text(
                text = if (validation.isValid) "Valid" else validation.error ?: "Invalid data",
                style = MaterialTheme.typography.labelSmall,
                color = if (validation.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
