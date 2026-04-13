package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LabelDesign(
    val version: Int = 1, // Default 1 for deserialization of v1 files without a version field
    val model: String = "d110",
    val labelWidthMm: Double = 30.0,
    val labelHeightMm: Double = 15.0,
    val density: Int = 3,
    val quantity: Int = 1,
    val elements: List<SerializableLabelElement> = emptyList(),
    // v1 compat fields (read for migration, not written in v2)
    val text: String? = null,
    val fontSize: Float? = null,
    val imageTransform: SerializableImageTransform? = null,
) {
    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        fun toJson(design: LabelDesign): String = json.encodeToString(serializer(), design)

        fun fromJson(jsonString: String): LabelDesign = json.decodeFromString(serializer(), jsonString)
    }

    fun migrateToV2(): LabelDesign {
        if (version >= 2) return this

        val migrated = mutableListOf<SerializableLabelElement>()

        if (text != null && text.isNotEmpty()) {
            migrated.add(
                SerializableLabelElement(
                    type = "text",
                    id = "migrated_text",
                    x = 8f,
                    y = 8f,
                    text = text,
                    fontSize = fontSize ?: 24f,
                    fontFamily = "default",
                ),
            )
        }

        imageTransform?.let { transform ->
            val hasImage = transform.offsetX != 0f || transform.offsetY != 0f ||
                transform.scale != 1f || transform.rotation != 0f ||
                transform.flipH || transform.flipV
            if (hasImage) {
                migrated.add(
                    SerializableLabelElement(
                        type = "image",
                        id = "migrated_image",
                        x = transform.offsetX,
                        y = transform.offsetY,
                        rotation = transform.rotation,
                        scale = transform.scale,
                        flipH = transform.flipH,
                        flipV = transform.flipV,
                    ),
                )
            }
        }

        return copy(version = 2, elements = migrated, text = null, fontSize = null, imageTransform = null)
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
