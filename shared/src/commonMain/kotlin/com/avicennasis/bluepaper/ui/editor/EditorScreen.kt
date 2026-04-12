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

    // Compute monochrome preview reactively
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
        // Header with Save/Load/Disconnect
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Label Editor", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val path = pickSaveFile("label.bpl")
                    if (path != null) writeTextFile(path, LabelDesign.toJson(state.toDesign()))
                }) { Text("Save") }
                TextButton(onClick = {
                    val json = pickOpenFile()
                    if (json != null) {
                        try { state.loadDesign(LabelDesign.fromJson(json)) } catch (_: Exception) { }
                    }
                }) { Text("Load") }
                TextButton(onClick = onDisconnect) { Text("Disconnect") }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Printer model dropdown
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

        // Label size chips
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

        // Previews
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
        Text("Print Preview (monochrome)", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        MonochromePreview(rows = monochromeRows, width = previewWidth)

        Spacer(Modifier.height(16.dp))

        // Image import + manipulation
        ImageControls(
            importedImage = importedImage,
            imageTransform = imageTransform,
            onImageLoaded = { state.setImage(it) },
            onClear = { state.clearImage() },
            onOffsetChange = { x, y -> state.setImageOffset(x, y) },
            onScaleChange = { state.setImageScale(it) },
            onRotate90 = { state.rotateImage90() },
            onFlipH = { state.toggleFlipH() },
            onFlipV = { state.toggleFlipV() },
        )

        Spacer(Modifier.height(16.dp))

        // Text input + font size
        TextControls(
            labelText = labelText,
            fontSize = fontSize,
            onTextChange = { state.setLabelText(it) },
            onFontSizeChange = { state.setFontSize(it) },
        )

        Spacer(Modifier.height(8.dp))

        // Density + copies
        PrintSettings(
            density = density,
            maxDensity = selectedModel.maxDensity,
            quantity = quantity,
            onDensityChange = { state.setDensity(it) },
            onQuantityChange = { state.setQuantity(it) },
        )

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
        PrintDialog(progress = printProgress, onDismiss = { showPrintDialog = false })
    }
}
