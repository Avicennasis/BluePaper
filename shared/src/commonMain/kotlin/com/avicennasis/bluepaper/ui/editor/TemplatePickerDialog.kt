package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplatePickerDialog(
    templates: List<LabelTemplate>,
    hasExistingElements: Boolean,
    onSelect: (LabelTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    var savedTemplates by remember { mutableStateOf<List<LabelTemplate>>(emptyList()) }
    LaunchedEffect(Unit) {
        savedTemplates = withContext(Dispatchers.IO) { TemplateStorage.loadAll() }
    }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Template") },
        text = {
            Column {
                if (hasExistingElements) {
                    Text(
                        "This will replace your current design.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn {
                    // Built-in templates
                    item {
                        Text("Built-in", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(templates) { template ->
                        TemplateCard(template = template, onClick = { onSelect(template) })
                    }

                    // Saved templates
                    if (savedTemplates.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("Saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                        }
                        items(savedTemplates) { template ->
                            TemplateCard(
                                template = template,
                                onClick = { onSelect(template) },
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        TemplateStorage.delete(template.name)
                                        val refreshed = TemplateStorage.loadAll()
                                        withContext(Dispatchers.Main) { savedTemplates = refreshed }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun TemplateCard(
    template: LabelTemplate,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${template.elements.size} element(s)",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
