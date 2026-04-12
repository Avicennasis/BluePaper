package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.image.LabelRenderer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val importedImage by state.importedImage.collectAsState()
    val imageTransform by state.imageTransform.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    var showPrintDialog by remember { mutableStateOf(false) }

    // Compute monochrome preview reactively (includes imported image)
    val monochromeRows = remember(labelText, fontSize, selectedLabelSize, selectedModel, importedImage, imageTransform) {
        val w = selectedLabelSize.widthPx
        val h = selectedLabelSize.heightPx
        if (w <= 0 || h <= 0) emptyList()
        else LabelRenderer.render(w, h, rotationDegrees = selectedModel.rotation) { drawScope ->
            drawLabelContent(drawScope, labelText, fontSize, textMeasurer, importedImage, imageTransform)
        }
    }
    val previewWidth = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.heightPx else selectedLabelSize.widthPx
    }
    val previewHeight = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.widthPx else selectedLabelSize.heightPx
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Label Editor", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }

        Spacer(Modifier.height(16.dp))

        // Printer model
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

        // Label size
        Text("Label Size", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedModel.labelSizes.forEach { size ->
                FilterChip(
                    selected = size == selectedLabelSize,
                    onClick = { state.selectLabelSize(size) },
                    label = { Text(size.displayName) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Design preview
        Text("Design Preview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        LabelCanvas(
            text = labelText,
            fontSize = fontSize,
            widthPx = selectedLabelSize.widthPx,
            heightPx = selectedLabelSize.heightPx,
            textMeasurer = textMeasurer,
            importedImage = importedImage,
            imageTransform = imageTransform,
        )

        Spacer(Modifier.height(12.dp))

        // Print preview
        Text("Print Preview (monochrome)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        MonochromePreview(rows = monochromeRows, width = previewWidth)

        Spacer(Modifier.height(16.dp))

        // ---- Image Import ----
        Text("Image", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImagePickerButton(onImageLoaded = { state.setImage(it) })
            if (importedImage != null) {
                OutlinedButton(onClick = { state.clearImage() }) {
                    Text("Clear")
                }
            }
        }

        // Image manipulation controls (visible when image is loaded)
        if (importedImage != null) {
            Spacer(Modifier.height(8.dp))

            // Position
            Text("Position X: ${imageTransform.offsetX.toInt()}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = imageTransform.offsetX,
                onValueChange = { state.setImageOffset(it, imageTransform.offsetY) },
                valueRange = -200f..500f,
            )
            Text("Position Y: ${imageTransform.offsetY.toInt()}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = imageTransform.offsetY,
                onValueChange = { state.setImageOffset(imageTransform.offsetX, it) },
                valueRange = -200f..500f,
            )

            // Scale
            Text("Scale: ${"%.1f".format(imageTransform.scale)}x", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = imageTransform.scale,
                onValueChange = { state.setImageScale(it) },
                valueRange = 0.1f..3f,
            )

            // Rotate + Flip buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state.rotateImage90() }) {
                    Text("Rotate 90")
                }
                OutlinedButton(onClick = { state.toggleFlipH() }) {
                    Text(if (imageTransform.flipH) "Flip H (on)" else "Flip H")
                }
                OutlinedButton(onClick = { state.toggleFlipV() }) {
                    Text(if (imageTransform.flipV) "Flip V (on)" else "Flip V")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---- Text ----
        Text("Text", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = labelText,
            onValueChange = { state.setLabelText(it) },
            label = { Text("Label Text") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = fontSize,
            onValueChange = { state.setFontSize(it) },
            valueRange = 8f..72f,
        )

        Spacer(Modifier.height(8.dp))

        // Density + Quantity
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Density: $density", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = density.toFloat(),
                    onValueChange = { state.setDensity(it.toInt()) },
                    valueRange = 1f..selectedModel.maxDensity.toFloat(),
                    steps = selectedModel.maxDensity - 2,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Copies: $quantity", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = quantity.toFloat(),
                    onValueChange = { state.setQuantity(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Print button
        Button(
            onClick = {
                state.print(monochromeRows, previewWidth, previewHeight)
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
