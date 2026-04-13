package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

object SmsEncoder : DataEncoder {
    override val standard = DataStandard.SMS

    override fun fields(): List<DataField> = listOf(
        DataField("phone", "Phone Number", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
        DataField("message", "Message", required = false, hint = "Hello!"),
    )

    override fun encode(fields: Map<String, String>): String {
        val phone = fields["phone"].orEmpty()
        val message = fields["message"].orEmpty()
        return "smsto:$phone:$message"
    }

    override fun decode(data: String): Map<String, String> {
        val stripped = if (data.startsWith("smsto:", ignoreCase = true)) data.substring(6) else data
        val colonIndex = stripped.indexOf(':')
        return if (colonIndex >= 0) {
            mapOf(
                "phone" to stripped.substring(0, colonIndex),
                "message" to stripped.substring(colonIndex + 1),
            )
        } else {
            mapOf("phone" to stripped, "message" to "")
        }
    }
}
