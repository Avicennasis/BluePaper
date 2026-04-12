package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.image.LabelRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onDisconnect: () -> Unit,
) {
    val labelText by state.labelText.collectAsState()
    val fontSize by state.fontSize.collectAsState()
    val selectedModel by state.selectedModel.collectAsState()
    val selectedLabelSize by state.selectedLabelSize.collectAsState()
    val density by state.density.collectAsState()
    val quantity by state.quantity.collectAsState()
    val printProgress by state.printProgress.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    var showPrintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Label Editor", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }

        Spacer(Modifier.height(16.dp))

        Text("Printer Model", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        var modelExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
            OutlinedTextField(
                value = selectedModel.model.uppercase(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
            )
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                DeviceRegistry.models().forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.uppercase()) },
                        onClick = { state.selectModel(model); modelExpanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Label Size", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedModel.labelSizes.forEach { size ->
                FilterChip(
                    selected = size == selectedLabelSize,
                    onClick = { state.selectLabelSize(size) },
                    label = { Text(size.displayName) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Preview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        LabelCanvas(
            text = labelText,
            fontSize = fontSize,
            widthPx = selectedLabelSize.widthPx,
            heightPx = selectedLabelSize.heightPx,
            textMeasurer = textMeasurer,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = labelText,
            onValueChange = { state.setLabelText(it) },
            label = { Text("Label Text") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = fontSize,
            onValueChange = { state.setFontSize(it) },
            valueRange = 8f..72f,
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Density: $density", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = density.toFloat(),
                    onValueChange = { state.setDensity(it.toInt()) },
                    valueRange = 1f..selectedModel.maxDensity.toFloat(),
                    steps = selectedModel.maxDensity - 2,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Copies: $quantity", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = quantity.toFloat(),
                    onValueChange = { state.setQuantity(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val w = selectedLabelSize.widthPx
                val h = selectedLabelSize.heightPx
                val rows = LabelRenderer.render(w, h) { drawScope ->
                    drawLabelContent(drawScope, labelText, fontSize, textMeasurer)
                }
                state.print(rows, w, h)
                showPrintDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Print", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showPrintDialog) {
        PrintDialog(
            progress = printProgress,
            onDismiss = { showPrintDialog = false },
        )
    }
}
