package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.Serializable

@Serializable
data class LabelTemplate(
    val name: String,
    val description: String,
    val elements: List<TemplateElement>,
)

@Serializable
data class TemplateElement(
    val type: String,
    val xFraction: Float = 0f,
    val yFraction: Float = 0f,
    val widthFraction: Float = 1f,
    val heightFraction: Float = 1f,
    val text: String? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null,
    val barcodeFormat: String? = null,
    val barcodeData: String? = null,
    val errorCorrection: String? = null,
    val dataStandard: String? = null,
    val imageScale: Float? = null,
    val rotation: Float = 0f,
    val fontWeight: Int? = null,
    val fontStyle: String? = null,
)

object TemplateManager {

    fun builtInTemplates(): List<LabelTemplate> = listOf(
        LabelTemplate(
            name = "Simple Text",
            description = "Single centered text",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.1f, widthFraction = 0.9f, heightFraction = 0.8f, text = "Label Text", fontSize = 32f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Two-Line",
            description = "Title + subtitle",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.05f, widthFraction = 0.9f, heightFraction = 0.45f, text = "Title", fontSize = 28f, fontFamily = "oswald"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.55f, widthFraction = 0.9f, heightFraction = 0.4f, text = "Subtitle", fontSize = 16f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Image + Caption",
            description = "Image left, text right",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 0.6f, heightFraction = 1f),
                TemplateElement(type = "text", xFraction = 0.62f, yFraction = 0.1f, widthFraction = 0.35f, heightFraction = 0.8f, text = "Caption", fontSize = 16f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Centered Image",
            description = "Full-bleed image",
            elements = listOf(
                TemplateElement(type = "image", xFraction = 0f, yFraction = 0f, widthFraction = 1f, heightFraction = 1f),
            ),
        ),
        LabelTemplate(
            name = "Price Tag",
            description = "Large price + description",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.05f, widthFraction = 0.9f, heightFraction = 0.55f, text = "$0.00", fontSize = 36f, fontFamily = "anton"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.65f, widthFraction = 0.9f, heightFraction = 0.3f, text = "Description", fontSize = 14f, fontFamily = "default"),
            ),
        ),
        LabelTemplate(
            name = "Inventory",
            description = "Title, code, note",
            elements = listOf(
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.02f, widthFraction = 0.9f, heightFraction = 0.2f, text = "ITEM", fontSize = 14f, fontFamily = "roboto_mono"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.25f, widthFraction = 0.9f, heightFraction = 0.45f, text = "000", fontSize = 40f, fontFamily = "default"),
                TemplateElement(type = "text", xFraction = 0.05f, yFraction = 0.75f, widthFraction = 0.9f, heightFraction = 0.2f, text = "Note", fontSize = 12f, fontFamily = "default"),
            ),
        ),
    )

    fun scaleToLabel(
        templateEl: TemplateElement,
        labelWidthPx: Int,
        labelHeightPx: Int,
        idPrefix: String,
    ): LabelElement {
        val x = templateEl.xFraction * labelWidthPx
        val y = templateEl.yFraction * labelHeightPx
        val w = templateEl.widthFraction * labelWidthPx
        val h = templateEl.heightFraction * labelHeightPx
        val id = "${idPrefix}_${templateEl.hashCode()}"

        return when (templateEl.type) {
            "text" -> LabelElement.TextElement(
                id = id, x = x, y = y, width = w, height = h,
                rotation = templateEl.rotation,
                text = templateEl.text ?: "Text",
                fontSize = templateEl.fontSize ?: 24f,
                fontFamily = templateEl.fontFamily ?: "default",
                fontWeight = templateEl.fontWeight ?: 400,
                fontStyle = templateEl.fontStyle ?: "normal",
            )
            "image" -> LabelElement.ImageElement(
                id = id, x = x, y = y, width = w, height = h,
                rotation = templateEl.rotation,
                scale = templateEl.imageScale ?: 1f,
            )
            "barcode" -> {
                val format = templateEl.barcodeFormat
                    ?.let { runCatching { BarcodeFormat.valueOf(it) }.getOrNull() }
                if (format != null) {
                    LabelElement.BarcodeElement(
                        id = id, x = x, y = y, width = w, height = h,
                        rotation = templateEl.rotation,
                        data = templateEl.barcodeData ?: "",
                        format = format,
                        errorCorrection = templateEl.errorCorrection
                            ?.let { runCatching { ErrorCorrection.valueOf(it) }.getOrNull() }
                            ?: ErrorCorrection.M,
                        dataStandard = templateEl.dataStandard
                            ?.let { runCatching { DataStandard.valueOf(it) }.getOrNull() }
                            ?: DataStandard.RAW_TEXT,
                    )
                } else {
                    LabelElement.TextElement(id = id, x = x, y = y, text = "Barcode")
                }
            }
            else -> LabelElement.TextElement(id = id, x = x, y = y, text = "Unknown")
        }
    }

    fun applyTemplate(
        template: LabelTemplate,
        labelWidthPx: Int,
        labelHeightPx: Int,
    ): List<LabelElement> {
        return template.elements.mapIndexed { index, el ->
            scaleToLabel(el, labelWidthPx, labelHeightPx, idPrefix = "tmpl$index")
        }
    }
}
