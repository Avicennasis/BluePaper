package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Serializable label design — everything needed to recreate a label.
 * Image data is NOT serialized (too large) — only text and transforms.
 */
@Serializable
data class LabelDesign(
    val text: String = "",
    val fontSize: Float = 24f,
    val model: String = "d110",
    val labelWidthMm: Double = 30.0,
    val labelHeightMm: Double = 15.0,
    val density: Int = 3,
    val quantity: Int = 1,
    val imageTransform: SerializableImageTransform = SerializableImageTransform(),
) {
    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        fun toJson(design: LabelDesign): String = json.encodeToString(serializer(), design)

        fun fromJson(jsonString: String): LabelDesign = json.decodeFromString(serializer(), jsonString)
    }
}

@Serializable
data class SerializableImageTransform(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
)

fun ImageTransform.toSerializable() = SerializableImageTransform(offsetX, offsetY, scale, rotation, flipH, flipV)
fun SerializableImageTransform.toImageTransform() = ImageTransform(offsetX, offsetY, scale, rotation, flipH, flipV)
