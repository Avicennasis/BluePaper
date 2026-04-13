package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class Gs1128Encoder : DataEncoder {

    override val standard = DataStandard.GS1_128

    override fun fields(): List<DataField> = listOf(
        DataField("gtin", "GTIN (AI 01)", fieldType = FieldType.NUMBER),
        DataField("batchLot", "Batch/Lot (AI 10)"),
        DataField("serial", "Serial Number (AI 21)"),
        DataField("useBy", "Use By Date (AI 17)", hint = "YYMMDD"),
        DataField("count", "Count (AI 30)", fieldType = FieldType.NUMBER),
    )

    override fun encode(fields: Map<String, String>): String {
        return buildString {
            for ((key, ai) in FIELD_AIS) {
                val value = fields[key]
                if (!value.isNullOrEmpty()) {
                    append("($ai)$value")
                }
            }
        }
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val aiToKey = FIELD_AIS.entries.associate { (key, ai) -> ai to key }
        // GS1 Application Identifiers can be 2, 3, or 4 digits.
        // Note: FIELD_AIS only maps 2-digit AIs, so 3-4 digit AIs will be
        // parsed but won't match a named field and will be silently ignored.
        val pattern = Regex("""\((\d{2,4})\)([^(]*)""")

        for (match in pattern.findAll(data)) {
            val ai = match.groupValues[1]
            val value = match.groupValues[2]
            val key = aiToKey[ai]
            if (key != null && value.isNotEmpty()) {
                result[key] = value
            }
        }
        return result
    }

    companion object {
        internal val FIELD_AIS = linkedMapOf(
            "gtin" to "01",
            "useBy" to "17",
            "batchLot" to "10",
            "serial" to "21",
            "count" to "30",
        )
    }
}
