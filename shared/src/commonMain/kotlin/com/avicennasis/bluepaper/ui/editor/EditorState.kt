package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.config.LabelSize
import com.avicennasis.bluepaper.printer.PrinterClient

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImageTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

data class PrintProgress(
    val completed: Int,
    val total: Int,
    val isPrinting: Boolean,
    val error: String? = null,
)

class EditorState(
    private val transport: BleTransport,
    private val scope: CoroutineScope,
) {
    private val client = PrinterClient(transport)
    val undoManager = UndoManager()

    private val _elements = MutableStateFlow<List<LabelElement>>(emptyList())
    val elements: StateFlow<List<LabelElement>> = _elements

    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId

    private val _selectedModel = MutableStateFlow(DeviceRegistry.get("d110")!!)
    val selectedModel: StateFlow<DeviceConfig> = _selectedModel

    private val _selectedLabelSize = MutableStateFlow(_selectedModel.value.labelSizes.first())
    val selectedLabelSize: StateFlow<LabelSize> = _selectedLabelSize

    private val _density = MutableStateFlow(3)
    val density: StateFlow<Int> = _density

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    private val _printProgress = MutableStateFlow(PrintProgress(0, 0, false))
    val printProgress: StateFlow<PrintProgress> = _printProgress

    private val _gridSize = MutableStateFlow(DEFAULT_GRID_SIZE)
    val gridSize: StateFlow<Float> = _gridSize

    private val _showGrid = MutableStateFlow(false)
    val showGrid: StateFlow<Boolean> = _showGrid

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoState() {
        _canUndo.value = undoManager.canUndo
        _canRedo.value = undoManager.canRedo
    }

    private fun saveUndoSnapshot() {
        undoManager.save(_elements.value)
        updateUndoState()
    }

    private val interactionSnapshotsInProgress = mutableSetOf<String>()

    private fun saveUndoSnapshotIfNeeded(operationKey: String) {
        if (operationKey !in interactionSnapshotsInProgress) {
            saveUndoSnapshot()
            interactionSnapshotsInProgress.add(operationKey)
        }
    }

    private fun finishInteraction(operationKey: String) {
        interactionSnapshotsInProgress.remove(operationKey)
    }

    fun undo() {
        val restored = undoManager.undo(_elements.value) ?: return
        _elements.value = restored
        if (restored.none { it.id == _selectedElementId.value }) {
            _selectedElementId.value = null
        }
        updateUndoState()
    }

    fun redo() {
        val restored = undoManager.redo(_elements.value) ?: return
        _elements.value = restored
        if (restored.none { it.id == _selectedElementId.value }) {
            _selectedElementId.value = null
        }
        updateUndoState()
    }

    fun selectElement(id: String?) { _selectedElementId.value = id }

    fun selectedElement(): LabelElement? =
        _elements.value.find { it.id == _selectedElementId.value }

    // Thread-confined to main thread (all callers are UI/Compose operations)
    private var nextId = 0
    private fun newId(prefix: String) = "${prefix}_${nextId++}"

    fun addTextElement() {
        saveUndoSnapshot()
        val el = LabelElement.TextElement(id = newId("text"))
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun addImageElement(bitmap: ImageBitmap) {
        saveUndoSnapshot()
        val el = LabelElement.ImageElement(
            id = newId("img"),
            width = bitmap.width.toFloat(),
            height = bitmap.height.toFloat(),
            bitmap = bitmap,
        )
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun removeElement(id: String) {
        saveUndoSnapshot()
        _elements.value = _elements.value.filter { it.id != id }
        if (_selectedElementId.value == id) _selectedElementId.value = null
    }

    private fun updateElement(id: String, transform: (LabelElement) -> LabelElement) {
        _elements.value = _elements.value.map { if (it.id == id) transform(it) else it }
    }

    fun moveElement(id: String, x: Float, y: Float) {
        saveUndoSnapshotIfNeeded("move_$id")
        val labelSize = _selectedLabelSize.value
        updateElement(id) { el ->
            val moved = when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y)
                is LabelElement.ImageElement -> el.copy(x = x, y = y)
                is LabelElement.BarcodeElement -> el.copy(x = x, y = y)
            }
            clampToLabel(moved, labelSize.widthPx, labelSize.heightPx)
        }
    }

    fun moveElementDone(id: String) { finishInteraction("move_$id") }

    fun resizeElement(id: String, width: Float, height: Float) {
        saveUndoSnapshotIfNeeded("resize_$id")
        val w = width.coerceAtLeast(MIN_ELEMENT_SIZE)
        val h = height.coerceAtLeast(MIN_ELEMENT_SIZE)
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(width = w, height = h)
                is LabelElement.ImageElement -> el.copy(width = w, height = h)
                is LabelElement.BarcodeElement -> el.copy(width = w, height = h)
            }
        }
    }

    fun resizeElementDone(id: String) { finishInteraction("resize_$id") }

    fun setElementPosition(id: String, x: Float, y: Float, width: Float, height: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y, width = width, height = height)
                is LabelElement.ImageElement -> el.copy(x = x, y = y, width = width, height = height)
                is LabelElement.BarcodeElement -> el.copy(x = x, y = y, width = width, height = height)
            }
        }
    }

    fun setElementRotation(id: String, degrees: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(rotation = degrees)
                is LabelElement.ImageElement -> el.copy(rotation = degrees)
                is LabelElement.BarcodeElement -> el.copy(rotation = degrees)
            }
        }
    }

    fun setTextContent(id: String, text: String) {
        saveUndoSnapshotIfNeeded("text_$id")
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(text = text) else el
        }
    }

    fun setTextContentDone(id: String) { finishInteraction("text_$id") }

    fun setFontSize(id: String, size: Float) {
        saveUndoSnapshotIfNeeded("fontSize_$id")
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontSize = size) else el
        }
    }

    fun setFontSizeDone(id: String) { finishInteraction("fontSize_$id") }

    fun setFontFamily(id: String, fontFamily: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontFamily = fontFamily) else el
        }
    }

    fun setImageScale(id: String, scale: Float) {
        saveUndoSnapshotIfNeeded("scale_$id")
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(scale = scale.coerceIn(0.1f, 5f)) else el
        }
    }

    fun setImageScaleDone(id: String) { finishInteraction("scale_$id") }

    fun toggleImageFlipH(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(flipH = !el.flipH) else el
        }
    }

    fun toggleImageFlipV(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(flipV = !el.flipV) else el
        }
    }

    fun rotateImage90(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(rotation = (el.rotation + 90f) % 360f) else el
        }
    }

    // --- Barcode methods ---

    fun addBarcodeElement(format: BarcodeFormat, data: String, dataStandard: DataStandard = DataStandard.RAW_TEXT, structuredData: Map<String, String> = emptyMap()) {
        saveUndoSnapshot()
        val size = if (format.is2D) 100f else 200f
        val height = if (format.is2D) 100f else 60f
        val el = LabelElement.BarcodeElement(
            id = newId("barcode"),
            width = size,
            height = height,
            data = data,
            format = format,
            dataStandard = dataStandard,
            structuredData = structuredData,
        )
        _elements.value = _elements.value + el
        _selectedElementId.value = el.id
    }

    fun setBarcodeData(id: String, data: String) {
        saveUndoSnapshotIfNeeded("barcodeData_$id")
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(data = data) else el
        }
    }

    fun setBarcodeDataDone(id: String) { finishInteraction("barcodeData_$id") }

    fun setBarcodeFormat(id: String, format: BarcodeFormat) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) {
                val autoFixed = BarcodeValidator.autoFix(format, el.data)
                el.copy(format = format, data = autoFixed)
            } else el
        }
    }

    fun setBarcodeErrorCorrection(id: String, ec: ErrorCorrection) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(errorCorrection = ec) else el
        }
    }

    fun setBarcodeDataStandard(id: String, standard: DataStandard) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) el.copy(dataStandard = standard, structuredData = emptyMap(), data = "") else el
        }
    }

    fun setBarcodeStructuredData(id: String, fields: Map<String, String>) {
        saveUndoSnapshotIfNeeded("barcodeStruct_$id")
        updateElement(id) { el ->
            if (el is LabelElement.BarcodeElement) {
                val encoded = DataEncoderRegistry.encode(el.dataStandard, fields)
                el.copy(structuredData = fields, data = encoded)
            } else el
        }
    }

    fun setBarcodeStructuredDataDone(id: String) { finishInteraction("barcodeStruct_$id") }

    // --- Bold/Italic ---

    fun setFontWeight(id: String, weight: Int) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontWeight = weight) else el
        }
    }

    fun toggleBold(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement)
                el.copy(fontWeight = if (el.fontWeight == 700) 400 else 700)
            else el
        }
    }

    fun setFontStyleValue(id: String, style: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontStyle = style) else el
        }
    }

    fun toggleItalic(id: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement)
                el.copy(fontStyle = if (el.fontStyle == "italic") "normal" else "italic")
            else el
        }
    }

    // --- Z-Ordering ---

    fun bringToFront(id: String) {
        saveUndoSnapshot()
        _elements.value = bringElementToFront(_elements.value, id)
    }

    fun sendToBack(id: String) {
        saveUndoSnapshot()
        _elements.value = sendElementToBack(_elements.value, id)
    }

    fun moveUp(id: String) {
        saveUndoSnapshot()
        _elements.value = moveElementUp(_elements.value, id)
    }

    fun moveDown(id: String) {
        saveUndoSnapshot()
        _elements.value = moveElementDown(_elements.value, id)
    }

    // --- Template Save ---

    fun saveAsTemplate(name: String, description: String) {
        val labelSize = _selectedLabelSize.value
        val w = labelSize.widthPx.toFloat().takeIf { it > 0f } ?: 1f
        val h = labelSize.heightPx.toFloat().takeIf { it > 0f } ?: 1f
        val templateElements = _elements.value.map { el ->
            TemplateElement(
                type = when (el) {
                    is LabelElement.TextElement -> "text"
                    is LabelElement.ImageElement -> "image"
                    is LabelElement.BarcodeElement -> "barcode"
                },
                xFraction = el.x / w,
                yFraction = el.y / h,
                widthFraction = el.width / w,
                heightFraction = el.height / h,
                text = (el as? LabelElement.TextElement)?.text,
                fontSize = (el as? LabelElement.TextElement)?.fontSize,
                fontFamily = (el as? LabelElement.TextElement)?.fontFamily,
                fontWeight = (el as? LabelElement.TextElement)?.fontWeight,
                fontStyle = (el as? LabelElement.TextElement)?.fontStyle,
                barcodeFormat = (el as? LabelElement.BarcodeElement)?.format?.name,
                barcodeData = (el as? LabelElement.BarcodeElement)?.data,
                errorCorrection = (el as? LabelElement.BarcodeElement)?.errorCorrection?.name,
                dataStandard = (el as? LabelElement.BarcodeElement)?.dataStandard?.name,
                imageScale = (el as? LabelElement.ImageElement)?.scale,
                rotation = el.rotation,
            )
        }
        val template = LabelTemplate(name, description, templateElements)
        TemplateStorage.save(template)
    }

    // --- Printer config ---

    fun setDensity(d: Int) { _density.value = d.coerceIn(1, _selectedModel.value.maxDensity) }
    fun setQuantity(q: Int) { _quantity.value = q.coerceIn(1, 100) }

    fun selectModel(modelName: String) {
        val config = DeviceRegistry.get(modelName) ?: return
        _selectedModel.value = config
        _selectedLabelSize.value = config.labelSizes.first()
        _density.value = _density.value.coerceAtMost(config.maxDensity)
    }

    fun selectLabelSize(size: LabelSize) {
        _selectedLabelSize.value = size
        // Re-clamp all elements to the new label dimensions
        _elements.value = clampAllToLabel(_elements.value, size.widthPx, size.heightPx)
    }

    fun setGridSize(size: Float) { _gridSize.value = size }
    fun toggleGrid() { _showGrid.value = !_showGrid.value }

    fun applyTemplate(template: LabelTemplate) {
        saveUndoSnapshot()
        val labelSize = _selectedLabelSize.value
        _elements.value = TemplateManager.applyTemplate(template, labelSize.widthPx, labelSize.heightPx)
        _selectedElementId.value = null
    }

    fun toDesign(): LabelDesign = LabelDesign(
        version = 2,
        model = _selectedModel.value.model,
        labelWidthMm = _selectedLabelSize.value.widthMm,
        labelHeightMm = _selectedLabelSize.value.heightMm,
        density = _density.value,
        quantity = _quantity.value,
        elements = _elements.value.map { it.toSerializable() },
    )

    fun loadDesign(design: LabelDesign) {
        saveUndoSnapshot()
        val migrated = design.migrateToV2()
        selectModel(migrated.model)
        val matchingSize = _selectedModel.value.labelSizes.find {
            it.widthMm == migrated.labelWidthMm && it.heightMm == migrated.labelHeightMm
        }
        if (matchingSize != null) _selectedLabelSize.value = matchingSize
        _density.value = migrated.density
        _quantity.value = migrated.quantity
        _elements.value = migrated.elements.map { it.toLabelElement() }
        _selectedElementId.value = null
    }

    fun print(imageRows: List<ByteArray>, width: Int, height: Int) {
        if (imageRows.isEmpty()) {
            _printProgress.value = PrintProgress(0, 0, false, error = "Label is still rendering, please wait")
            return
        }
        if (_printProgress.value.isPrinting) return  // already printing

        val config = _selectedModel.value
        val qty = _quantity.value
        val dens = _density.value

        _printProgress.value = PrintProgress(0, qty, true)

        scope.launch {
            try {
                client.print(
                    imageRows = imageRows,
                    width = width,
                    height = height,
                    density = dens,
                    quantity = qty,
                    isV2 = config.isV2,
                    onProgress = { completed, total ->
                        _printProgress.value = PrintProgress(completed, total, true)
                    },
                )
                _printProgress.value = PrintProgress(qty, qty, false)
            } catch (e: Exception) {
                _printProgress.value = PrintProgress(0, 0, false, error = e.message)
            }
        }
    }
}
