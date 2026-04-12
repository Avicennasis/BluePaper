package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class WifiEncoder : DataEncoder {

    override val standard: DataStandard = DataStandard.WIFI

    override fun fields(): List<DataField> = listOf(
        DataField(
            key = "ssid",
            label = "SSID",
            required = true,
            fieldType = FieldType.TEXT,
        ),
        DataField(
            key = "password",
            label = "Password",
            required = false,
            fieldType = FieldType.TEXT,
        ),
        DataField(
            key = "encryption",
            label = "Encryption",
            required = true,
            hint = "WPA",
            fieldType = FieldType.TEXT,
        ),
    )

    override fun encode(fields: Map<String, String>): String {
        val encryption = fields["encryption"].orEmpty()
        val ssid = fields["ssid"].orEmpty()
        val password = fields["password"].orEmpty()
        return "WIFI:T:$encryption;S:$ssid;P:$password;;"
    }

    override fun decode(data: String): Map<String, String> {
        if (!data.startsWith("WIFI:")) return emptyMap()

        val result = mutableMapOf<String, String>()
        val body = data.removePrefix("WIFI:").removeSuffix(";;").removeSuffix(";")

        for (part in splitFields(body)) {
            when {
                part.startsWith("T:") -> result["encryption"] = part.removePrefix("T:")
                part.startsWith("S:") -> result["ssid"] = part.removePrefix("S:")
                part.startsWith("P:") -> result["password"] = part.removePrefix("P:")
            }
        }
        return result
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
