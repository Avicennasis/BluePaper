package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class HibcEncoder : DataEncoder {

    override val standard = DataStandard.HIBC

    override fun fields(): List<DataField> = listOf(
        // LIC must be exactly 4 uppercase alphanumeric characters per HIBC standard
        DataField("lic", "Labeler ID Code", required = true),
        DataField("pcn", "Product/Catalog Number", required = true),
        DataField("uom", "Unit of Measure", hint = "0"),
        DataField("quantity", "Quantity", fieldType = FieldType.NUMBER),
    )

    override fun encode(fields: Map<String, String>): String {
        val rawLic = fields["lic"].orEmpty().uppercase()
        // Validate and normalize LIC to exactly 4 uppercase alphanumeric characters
        val lic = rawLic.filter { it.isLetterOrDigit() }.take(4).padEnd(4, ' ')
        val pcn = fields["pcn"].orEmpty()
        val uom = fields["uom"]?.takeIf { it.isNotEmpty() } ?: "0"
        val quantity = fields["quantity"]

        return buildString {
            append("+$lic$pcn$uom")
            if (!quantity.isNullOrEmpty()) {
                append("/$quantity")
            }
        }
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var body = data

        if (body.startsWith("+")) {
            body = body.removePrefix("+")
        }

        val slashIndex = body.indexOf('/')
        val primary: String
        val secondary: String?

        if (slashIndex >= 0) {
            primary = body.substring(0, slashIndex)
            secondary = body.substring(slashIndex + 1)
        } else {
            primary = body
            secondary = null
        }

        if (primary.length >= 4) {
            result["lic"] = primary.take(4)
            if (primary.length > 5) {
                result["pcn"] = primary.substring(4, primary.length - 1)
                result["uom"] = primary.last().toString()
            } else if (primary.length == 5) {
                result["pcn"] = ""
                result["uom"] = primary.last().toString()
            }
        }

        if (!secondary.isNullOrEmpty()) {
            result["quantity"] = secondary
        }

        return result
    }
}
