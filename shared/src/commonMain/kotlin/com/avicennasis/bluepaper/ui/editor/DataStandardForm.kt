package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DataStandardForm(
    standard: DataStandard,
    fields: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldDefs = DataEncoderRegistry.fieldsFor(standard)
    Column(modifier = modifier) {
        for (field in fieldDefs) {
            val value = fields[field.key] ?: ""
            OutlinedTextField(
                value = value,
                onValueChange = { onFieldChange(field.key, it) },
                label = { Text(field.label + if (field.required) " *" else "", style = MaterialTheme.typography.bodySmall) },
                placeholder = { if (field.hint.isNotEmpty()) Text(field.hint, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = when (field.fieldType) {
                    FieldType.NUMBER -> KeyboardType.Number
                    FieldType.EMAIL -> KeyboardType.Email
                    FieldType.PHONE -> KeyboardType.Phone
                    FieldType.URL -> KeyboardType.Uri
                    FieldType.TEXT -> KeyboardType.Text
                }),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
