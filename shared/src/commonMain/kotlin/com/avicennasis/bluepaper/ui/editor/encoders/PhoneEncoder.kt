package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

object PhoneEncoder : DataEncoder {
    override val standard = DataStandard.PHONE

    override fun fields(): List<DataField> = listOf(
        DataField("number", "Phone Number", required = true, hint = "+1234567890", fieldType = FieldType.PHONE),
    )

    override fun encode(fields: Map<String, String>): String {
        val number = fields["number"].orEmpty()
        return "tel:$number"
    }

    override fun decode(data: String): Map<String, String> {
        val number = data.removePrefix("tel:")
        return mapOf("number" to number)
    }
}
