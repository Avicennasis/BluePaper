package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.config.LabelSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ToolboxPanel(
    elements: List<LabelElement>,
    selectedElementId: String?,
    selectedModel: DeviceConfig,
    selectedLabelSize: LabelSize,
    density: Int,
    quantity: Int,
    onSelectElement: (String) -> Unit,
    onDeleteElement: (String) -> Unit,
    onAddText: () -> Unit,
    onAddImage: (ImageBitmap) -> Unit,
    onAddBarcode: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectLabelSize: (LabelSize) -> Unit,
    onDensityChange: (Int) -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight()
            .padding(8.dp),
    ) {
        Text("Elements", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onAddText, modifier = Modifier.weight(1f)) {
                Text("+ Text")
            }
            ImagePickerButton(onImageLoaded = onAddImage)
        }
        OutlinedButton(onClick = onAddBarcode, modifier = Modifier.fillMaxWidth()) {
            Text("+ Barcode")
        }

        Spacer(Modifier.height(8.dp))

        ElementList(
            elements = elements,
            selectedElementId = selectedElementId,
            onSelect = onSelectElement,
            onDelete = onDeleteElement,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("Printer", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))

            var modelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
                OutlinedTextField(
                    value = selectedModel.model.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                    DeviceRegistry.models().forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model.uppercase()) },
                            onClick = { onSelectModel(model); modelExpanded = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedModel.labelSizes.forEach { size ->
                    FilterChip(
                        selected = size == selectedLabelSize,
                        onClick = { onSelectLabelSize(size) },
                        label = { Text(size.displayName, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Density: $density", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = density.toFloat(),
                onValueChange = { onDensityChange(it.toInt()) },
                valueRange = 1f..selectedModel.maxDensity.toFloat(),
                steps = selectedModel.maxDensity - 2,
            )

            Text("Copies: $quantity", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = quantity.toFloat(),
                onValueChange = { onQuantityChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )
        }
    }
}
