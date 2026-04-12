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
)

fun LabelElement.toSerializable(): SerializableLabelElement = when (this) {
    is LabelElement.TextElement -> SerializableLabelElement(
        type = "text", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, text = text, fontSize = fontSize, fontFamily = fontFamily,
    )
    is LabelElement.ImageElement -> SerializableLabelElement(
        type = "image", id = id, x = x, y = y, width = width, height = height,
        rotation = rotation, scale = scale, flipH = flipH, flipV = flipV,
    )
}

fun SerializableLabelElement.toLabelElement(): LabelElement = when (type) {
    "text" -> LabelElement.TextElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        text = text ?: "Hello", fontSize = fontSize ?: 24f, fontFamily = fontFamily ?: "default",
    )
    "image" -> LabelElement.ImageElement(
        id = id, x = x, y = y, width = width, height = height, rotation = rotation,
        scale = scale ?: 1f, flipH = flipH ?: false, flipV = flipV ?: false,
    )
    else -> LabelElement.TextElement(id = id, x = x, y = y, text = "Unknown")
}
