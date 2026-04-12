package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PropertiesPanel(
    selectedElement: LabelElement?,
    onPositionChange: (String, Float, Float, Float, Float) -> Unit,
    onRotationChange: (String, Float) -> Unit,
    onTextChange: (String, String) -> Unit,
    onTextChangeDone: (String) -> Unit,
    onFontSizeChange: (String, Float) -> Unit,
    onFontSizeChangeDone: (String) -> Unit,
    onFontFamilyChange: (String, String) -> Unit,
    onImageScaleChange: (String, Float) -> Unit,
    onImageScaleChangeDone: (String) -> Unit,
    onFlipH: (String) -> Unit,
    onFlipV: (String) -> Unit,
    onRotateImage90: (String) -> Unit,
    onBarcodeFormatChange: (String, BarcodeFormat) -> Unit = { _, _ -> },
    onBarcodeDataChange: (String, String) -> Unit = { _, _ -> },
    onBarcodeDataChangeDone: (String) -> Unit = {},
    onBarcodeErrorCorrectionChange: (String, ErrorCorrection) -> Unit = { _, _ -> },
    onBarcodeDataStandardChange: (String, DataStandard) -> Unit = { _, _ -> },
    onBarcodeStructuredDataChange: (String, Map<String, String>) -> Unit = { _, _ -> },
    onBarcodeStructuredDataChangeDone: (String) -> Unit = {},
    onPrint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (selectedElement != null) {
            Text("Properties", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            Text("Position", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField("X", selectedElement.x, modifier = Modifier.weight(1f)) { newX ->
                    onPositionChange(selectedElement.id, newX, selectedElement.y, selectedElement.width, selectedElement.height)
                }
                NumericField("Y", selectedElement.y, modifier = Modifier.weight(1f)) { newY ->
                    onPositionChange(selectedElement.id, selectedElement.x, newY, selectedElement.width, selectedElement.height)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Size", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumericField("W", selectedElement.width, modifier = Modifier.weight(1f)) { newW ->
                    onPositionChange(selectedElement.id, selectedElement.x, selectedElement.y, newW, selectedElement.height)
                }
                NumericField("H", selectedElement.height, modifier = Modifier.weight(1f)) { newH ->
                    onPositionChange(selectedElement.id, selectedElement.x, selectedElement.y, selectedElement.width, newH)
                }
            }
            Spacer(Modifier.height(4.dp))
            NumericField("Rotation", selectedElement.rotation, modifier = Modifier.fillMaxWidth()) { deg ->
                onRotationChange(selectedElement.id, deg)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            when (selectedElement) {
                is LabelElement.TextElement -> {
                    Text("Text", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedElement.text,
                        onValueChange = { onTextChange(selectedElement.id, it) },
                        label = { Text("Label Text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 5,
                    )

                    Spacer(Modifier.height(8.dp))

                    FontPicker(
                        selectedFontKey = selectedElement.fontFamily,
                        onFontSelected = { onFontFamilyChange(selectedElement.id, it) },
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Font Size: ${selectedElement.fontSize.toInt()}sp", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedElement.fontSize,
                        onValueChange = { onFontSizeChange(selectedElement.id, it) },
                        onValueChangeFinished = { onFontSizeChangeDone(selectedElement.id) },
                        valueRange = 8f..72f,
                    )
                }

                is LabelElement.ImageElement -> {
                    Text("Image", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))

                    Text("Scale: ${"%.1f".format(selectedElement.scale)}x", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedElement.scale,
                        onValueChange = { onImageScaleChange(selectedElement.id, it) },
                        onValueChangeFinished = { onImageScaleChangeDone(selectedElement.id) },
                        valueRange = 0.1f..5f,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = { onRotateImage90(selectedElement.id) }) { Text("Rot 90") }
                        OutlinedButton(onClick = { onFlipH(selectedElement.id) }) {
                            Text(if (selectedElement.flipH) "H (on)" else "Flip H")
                        }
                        OutlinedButton(onClick = { onFlipV(selectedElement.id) }) {
                            Text(if (selectedElement.flipV) "V (on)" else "Flip V")
                        }
                    }
                }

                is LabelElement.BarcodeElement -> {
                    BarcodeProperties(
                        element = selectedElement,
                        onFormatChange = onBarcodeFormatChange,
                        onDataChange = onBarcodeDataChange,
                        onDataChangeDone = onBarcodeDataChangeDone,
                        onErrorCorrectionChange = onBarcodeErrorCorrectionChange,
                        onDataStandardChange = onBarcodeDataStandardChange,
                        onStructuredDataChange = onBarcodeStructuredDataChange,
                        onStructuredDataChangeDone = onBarcodeStructuredDataChangeDone,
                    )
                }
            }
        } else {
            Text("No Selection", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text("Click an element on the canvas or add one from the left panel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onPrint,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Print", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    var textValue by remember(value) { mutableStateOf(value.toInt().toString()) }

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            textValue = newText
            newText.toFloatOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodySmall,
    )
}
