package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class MeCardEncoder : DataEncoder {

    override val standard: DataStandard = DataStandard.MECARD

    override fun fields(): List<DataField> = listOf(
        DataField(
            key = "name",
            label = "Name",
            required = true,
            fieldType = FieldType.TEXT,
        ),
        DataField(
            key = "phone",
            label = "Phone",
            required = true,
            fieldType = FieldType.PHONE,
        ),
        DataField(
            key = "email",
            label = "Email",
            required = false,
            fieldType = FieldType.EMAIL,
        ),
        DataField(
            key = "url",
            label = "URL",
            required = false,
            fieldType = FieldType.URL,
        ),
        DataField(
            key = "address",
            label = "Address",
            required = false,
            fieldType = FieldType.TEXT,
        ),
        DataField(
            key = "note",
            label = "Note",
            required = false,
            fieldType = FieldType.TEXT,
        ),
    )

    override fun encode(fields: Map<String, String>): String {
        val sb = StringBuilder("MECARD:")
        appendField(sb, "N", fields["name"])
        appendField(sb, "TEL", fields["phone"])
        appendField(sb, "EMAIL", fields["email"])
        appendField(sb, "URL", fields["url"])
        appendField(sb, "ADR", fields["address"])
        appendField(sb, "NOTE", fields["note"])
        sb.append(";")
        return sb.toString()
    }

    override fun decode(data: String): Map<String, String> {
        if (!data.startsWith("MECARD:")) return emptyMap()

        val result = mutableMapOf<String, String>()
        // Only strip the MECARD terminator ';;' — don't also strip a trailing ';'
        // as that would corrupt NOTE values ending with a semicolon
        val body = data.removePrefix("MECARD:").removeSuffix(";;")

        for (part in splitFields(body)) {
            when {
                part.startsWith("N:") -> result["name"] = part.removePrefix("N:")
                part.startsWith("TEL:") -> result["phone"] = part.removePrefix("TEL:")
                part.startsWith("EMAIL:") -> result["email"] = part.removePrefix("EMAIL:")
                part.startsWith("URL:") -> result["url"] = part.removePrefix("URL:")
                part.startsWith("ADR:") -> result["address"] = part.removePrefix("ADR:")
                part.startsWith("NOTE:") -> result["note"] = part.removePrefix("NOTE:")
            }
        }
        return result
    }

    private fun appendField(sb: StringBuilder, prefix: String, value: String?) {
        if (!value.isNullOrEmpty()) {
            sb.append("$prefix:$value;")
        }
    }

    private fun splitFields(body: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < body.length) {
            if (body[i] == ';' && (i == 0 || body[i - 1] != '\\')) {
                fields.add(current.toString())
                current.clear()
            } else {
                current.append(body[i])
            }
            i++
        }
        if (current.isNotEmpty()) fields.add(current.toString())
        return fields
    }
}
