package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class UrlEncoder : DataEncoder {

    override val standard = DataStandard.URL

    override fun fields(): List<DataField> = listOf(
        DataField("url", "URL", required = true, fieldType = FieldType.URL),
    )

    override fun encode(fields: Map<String, String>): String =
        fields["url"].orEmpty()

    override fun decode(data: String): Map<String, String> =
        mapOf("url" to data)
}
