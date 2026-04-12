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

    fun undo() {
        val restored = undoManager.undo(_elements.value) ?: return
        _elements.value = restored
        updateUndoState()
    }

    fun redo() {
        val restored = undoManager.redo(_elements.value) ?: return
        _elements.value = restored
        updateUndoState()
    }

    fun selectElement(id: String?) { _selectedElementId.value = id }

    fun selectedElement(): LabelElement? =
        _elements.value.find { it.id == _selectedElementId.value }

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
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y)
                is LabelElement.ImageElement -> el.copy(x = x, y = y)
            }
        }
    }

    fun moveElementDone(id: String) { saveUndoSnapshot() }

    fun resizeElement(id: String, width: Float, height: Float) {
        val w = width.coerceAtLeast(MIN_ELEMENT_SIZE)
        val h = height.coerceAtLeast(MIN_ELEMENT_SIZE)
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(width = w, height = h)
                is LabelElement.ImageElement -> el.copy(width = w, height = h)
            }
        }
    }

    fun resizeElementDone(id: String) { saveUndoSnapshot() }

    fun setElementPosition(id: String, x: Float, y: Float, width: Float, height: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(x = x, y = y, width = width, height = height)
                is LabelElement.ImageElement -> el.copy(x = x, y = y, width = width, height = height)
            }
        }
    }

    fun setElementRotation(id: String, degrees: Float) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            when (el) {
                is LabelElement.TextElement -> el.copy(rotation = degrees)
                is LabelElement.ImageElement -> el.copy(rotation = degrees)
            }
        }
    }

    fun setTextContent(id: String, text: String) {
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(text = text) else el
        }
    }

    fun setTextContentDone(id: String) { saveUndoSnapshot() }

    fun setFontSize(id: String, size: Float) {
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontSize = size) else el
        }
    }

    fun setFontSizeDone(id: String) { saveUndoSnapshot() }

    fun setFontFamily(id: String, fontFamily: String) {
        saveUndoSnapshot()
        updateElement(id) { el ->
            if (el is LabelElement.TextElement) el.copy(fontFamily = fontFamily) else el
        }
    }

    fun setImageScale(id: String, scale: Float) {
        updateElement(id) { el ->
            if (el is LabelElement.ImageElement) el.copy(scale = scale.coerceIn(0.1f, 5f)) else el
        }
    }

    fun setImageScaleDone(id: String) { saveUndoSnapshot() }

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

    fun setDensity(d: Int) { _density.value = d.coerceIn(1, _selectedModel.value.maxDensity) }
    fun setQuantity(q: Int) { _quantity.value = q.coerceIn(1, 100) }

    fun selectModel(modelName: String) {
        val config = DeviceRegistry.get(modelName) ?: return
        _selectedModel.value = config
        _selectedLabelSize.value = config.labelSizes.first()
        _density.value = _density.value.coerceAtMost(config.maxDensity)
    }

    fun selectLabelSize(size: LabelSize) { _selectedLabelSize.value = size }

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
