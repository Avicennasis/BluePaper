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
        // GS1 General Specifications: barcode data must NOT contain parentheses.
        // Fixed-length AIs need no separator; variable-length AIs are terminated
        // by a FNC1/GS character (\u001D) when followed by another AI.
        val parts = mutableListOf<Pair<String, String>>() // (ai, value)
        for ((key, ai) in FIELD_AIS) {
            val value = fields[key]
            if (!value.isNullOrEmpty()) {
                parts.add(ai to value)
            }
        }
        return buildString {
            for ((index, pair) in parts.withIndex()) {
                val (ai, value) = pair
                append(ai)
                append(value)
                // Insert GS separator after variable-length AIs when not the last element
                if (index < parts.size - 1 && ai !in FIXED_LENGTH_AIS) {
                    append(GS)
                }
            }
        }
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val aiToKey = FIELD_AIS.entries.associate { (key, ai) -> ai to key }
        // Split on GS delimiters to get segments. Each segment starts with an AI
        // and may contain multiple concatenated AI+value pairs (only if the leading
        // AIs in the segment are fixed-length). A GS always terminates a
        // variable-length AI, so the last AI in each segment is either fixed-length
        // (consuming its exact character count) or variable-length (consuming the
        // rest of the segment).
        val segments = data.split(GS)
        for (segment in segments) {
            parseSegment(segment, aiToKey, result)
        }
        return result
    }

    private fun parseSegment(
        segment: String,
        aiToKey: Map<String, String>,
        result: MutableMap<String, String>,
    ) {
        var pos = 0
        while (pos < segment.length) {
            if (pos + 2 > segment.length) break
            val ai = segment.substring(pos, pos + 2)
            pos += 2
            val key = aiToKey[ai]
            if (key == null) {
                // Unknown AI — stop parsing this segment
                break
            }
            val fixedLen = FIXED_LENGTH_AIS[ai]
            if (fixedLen != null) {
                // Fixed-length: consume exactly fixedLen characters
                if (pos + fixedLen > segment.length) break
                result[key] = segment.substring(pos, pos + fixedLen)
                pos += fixedLen
            } else {
                // Variable-length: consume rest of segment (GS already split it)
                val value = segment.substring(pos)
                if (value.isNotEmpty()) {
                    result[key] = value
                }
                pos = segment.length
            }
        }
    }

    companion object {
        /** Group Separator — FNC1 delimiter between variable-length AIs. */
        const val GS = '\u001D'

        internal val FIELD_AIS = linkedMapOf(
            "gtin" to "01",
            "useBy" to "17",
            "batchLot" to "10",
            "serial" to "21",
            "count" to "30",
        )

        /**
         * AIs with fixed data lengths (digits after the AI code).
         * These never need a GS separator after them because the scanner
         * knows exactly how many characters to consume.
         */
        internal val FIXED_LENGTH_AIS = mapOf(
            "01" to 14, // GTIN: 14 digits
            "17" to 6,  // Use By date: YYMMDD
        )
    }
}
