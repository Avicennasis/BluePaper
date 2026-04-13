package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

sealed class LabelElement {
    abstract val id: String
    abstract val x: Float
    abstract val y: Float
    abstract val width: Float
    abstract val height: Float
    abstract val rotation: Float

    data class TextElement(
        override val id: String,
        override val x: Float = 8f,
        override val y: Float = 8f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val rotation: Float = 0f,
        val text: String = "Hello",
        val fontSize: Float = 24f,
        val fontFamily: String = "default",
        val fontWeight: Int = 400,
        val fontStyle: String = "normal",
    ) : LabelElement()

    data class ImageElement(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 0f,
        override val height: Float = 0f,
        override val rotation: Float = 0f,
        val bitmap: ImageBitmap? = null,
        val scale: Float = 1f,
        val flipH: Boolean = false,
        val flipV: Boolean = false,
    ) : LabelElement()

    data class BarcodeElement(
        override val id: String,
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val width: Float = 100f,
        override val height: Float = 100f,
        override val rotation: Float = 0f,
        val data: String = "",
        val format: BarcodeFormat = BarcodeFormat.QR_CODE,
        val errorCorrection: ErrorCorrection = ErrorCorrection.M,
        val dataStandard: DataStandard = DataStandard.RAW_TEXT,
        val structuredData: Map<String, String> = emptyMap(),
    ) : LabelElement()
}

@Serializable
data class SerializableLabelElement(
    val type: String,
    val id: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    val text: String? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
    val scale: Float? = null,
    val flipH: Boolean? = null,
    val flipV: Boolean? = null,
    val fontWeight: Int? = null,
    val fontStyle: String? = null,
    val imageData: String? = null,
    val barcodeData: String? = null,
    val barcodeFormat: String? = null,
    val errorCorrection: String? = null,
    val dataStandard: String? = null,
    val structuredData: Map<String, String>? = null,
)

fun LabelElement.toSerializable(): SerializableLabelElement = when (this) {
    is LabelElement.TextElement -> SerializableLabelElement(
        type = "text", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, text = text, fontSize = fontSize, fontFamily = fontFamily,
        fontWeight = fontWeight.takeIf { it != 400 },
        fontStyle = fontStyle.takeIf { it != "normal" },
    )
    is LabelElement.ImageElement -> SerializableLabelElement(
        type = "image", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, scale = scale, flipH = flipH, flipV = flipV,
        imageData = bitmap?.let { ImageEncoder.encode(it, 1024) },
    )
    is LabelElement.BarcodeElement -> SerializableLabelElement(
        type = "barcode", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, barcodeData = data, barcodeFormat = format.name,
        errorCorrection = errorCorrection.name, dataStandard = dataStandard.name,
        structuredData = structuredData.takeIf { it.isNotEmpty() },
    )
}

fun SerializableLabelElement.toLabelElement(): LabelElement = when (type) {
    "text" -> LabelElement.TextElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        text = text ?: "Hello", fontSize = fontSize ?: 24f, fontFamily = fontFamily ?: "default",
        fontWeight = fontWeight ?: 400, fontStyle = fontStyle ?: "normal",
    )
    "image" -> LabelElement.ImageElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        scale = scale ?: 1f, flipH = flipH ?: false, flipV = flipV ?: false,
        bitmap = imageData?.let { ImageEncoder.decode(it) },
    )
    "barcode" -> LabelElement.BarcodeElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        data = barcodeData ?: "",
        format = barcodeFormat?.let { runCatching { BarcodeFormat.valueOf(it) }.getOrNull() } ?: BarcodeFormat.QR_CODE,
        errorCorrection = errorCorrection?.let { runCatching { ErrorCorrection.valueOf(it) }.getOrNull() } ?: ErrorCorrection.M,
        dataStandard = dataStandard?.let { runCatching { DataStandard.valueOf(it) }.getOrNull() } ?: DataStandard.RAW_TEXT,
        structuredData = structuredData ?: emptyMap(),
    )
    else -> LabelElement.TextElement(id = id, x = x, y = y, text = "Unknown")
}
