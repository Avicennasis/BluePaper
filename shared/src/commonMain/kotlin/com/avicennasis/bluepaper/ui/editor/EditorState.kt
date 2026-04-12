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

    private val _labelText = MutableStateFlow("Hello")
    val labelText: StateFlow<String> = _labelText

    private val _fontSize = MutableStateFlow(24f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _selectedModel = MutableStateFlow(DeviceRegistry.get("d110")!!)
    val selectedModel: StateFlow<DeviceConfig> = _selectedModel

    private val _selectedLabelSize = MutableStateFlow(_selectedModel.value.labelSizes.first())
    val selectedLabelSize: StateFlow<LabelSize> = _selectedLabelSize

    private val _density = MutableStateFlow(3)
    val density: StateFlow<Int> = _density

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    private val _importedImage = MutableStateFlow<ImageBitmap?>(null)
    val importedImage: StateFlow<ImageBitmap?> = _importedImage

    private val _imageTransform = MutableStateFlow(ImageTransform())
    val imageTransform: StateFlow<ImageTransform> = _imageTransform

    private val _printProgress = MutableStateFlow(PrintProgress(0, 0, false))
    val printProgress: StateFlow<PrintProgress> = _printProgress

    fun setLabelText(text: String) { _labelText.value = text }
    fun setFontSize(size: Float) { _fontSize.value = size }
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
    }

    // ---- Image Methods ----

    fun setImage(bitmap: ImageBitmap) {
        _importedImage.value = bitmap
        _imageTransform.value = ImageTransform() // reset transforms
    }

    fun clearImage() {
        _importedImage.value = null
        _imageTransform.value = ImageTransform()
    }

    fun setImageOffset(x: Float, y: Float) {
        _imageTransform.value = _imageTransform.value.copy(offsetX = x, offsetY = y)
    }

    fun setImageScale(scale: Float) {
        _imageTransform.value = _imageTransform.value.copy(scale = scale.coerceIn(0.1f, 5f))
    }

    fun setImageRotation(degrees: Float) {
        _imageTransform.value = _imageTransform.value.copy(rotation = degrees)
    }

    fun toggleFlipH() {
        _imageTransform.value = _imageTransform.value.copy(flipH = !_imageTransform.value.flipH)
    }

    fun toggleFlipV() {
        _imageTransform.value = _imageTransform.value.copy(flipV = !_imageTransform.value.flipV)
    }

    fun rotateImage90() {
        _imageTransform.value = _imageTransform.value.copy(
            rotation = (_imageTransform.value.rotation + 90f) % 360f,
        )
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
