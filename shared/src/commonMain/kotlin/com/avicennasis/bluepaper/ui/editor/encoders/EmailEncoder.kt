package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

object EmailEncoder : DataEncoder {
    override val standard = DataStandard.EMAIL

    override fun fields(): List<DataField> = listOf(
        DataField("address", "Email Address", required = true, hint = "user@example.com", fieldType = FieldType.EMAIL),
        DataField("subject", "Subject", required = false, hint = "Subject line"),
        DataField("body", "Body", required = false, hint = "Message body"),
    )

    override fun encode(fields: Map<String, String>): String {
        val address = fields["address"].orEmpty()
        val subject = fields["subject"].orEmpty()
        val body = fields["body"].orEmpty()
        val params = buildList {
            if (subject.isNotEmpty()) add("subject=${percentEncode(subject)}")
            if (body.isNotEmpty()) add("body=${percentEncode(body)}")
        }
        return if (params.isEmpty()) {
            "mailto:$address"
        } else {
            "mailto:$address?${params.joinToString("&")}"
        }
    }

    override fun decode(data: String): Map<String, String> {
        val stripped = data.removePrefix("mailto:")
        val questionIndex = stripped.indexOf('?')
        if (questionIndex < 0) {
            return mapOf("address" to stripped, "subject" to "", "body" to "")
        }
        val address = stripped.substring(0, questionIndex)
        val query = stripped.substring(questionIndex + 1)
        val params = query.split("&").associate { part ->
            val eqIndex = part.indexOf('=')
            if (eqIndex >= 0) {
                part.substring(0, eqIndex) to percentDecode(part.substring(eqIndex + 1))
            } else {
                part to ""
            }
        }
        return mapOf(
            "address" to address,
            "subject" to params.getOrDefault("subject", ""),
            "body" to params.getOrDefault("body", ""),
        )
    }

    private fun percentEncode(value: String): String =
        value.replace("%", "%25").replace("&", "%26").replace("=", "%3D").replace(" ", "%20")

    private fun percentDecode(value: String): String =
        value.replace("%26", "&").replace("%3D", "=").replace("%20", " ").replace("%25", "%")
}
