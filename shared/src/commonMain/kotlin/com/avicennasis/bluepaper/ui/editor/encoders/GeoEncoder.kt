package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

object GeoEncoder : DataEncoder {
    override val standard = DataStandard.GEO

    override fun fields(): List<DataField> = listOf(
        DataField("latitude", "Latitude", required = true, hint = "40.7128", fieldType = FieldType.NUMBER),
        DataField("longitude", "Longitude", required = true, hint = "-74.0060", fieldType = FieldType.NUMBER),
    )

    override fun encode(fields: Map<String, String>): String {
        val latitude = fields["latitude"].orEmpty()
        val longitude = fields["longitude"].orEmpty()
        return "geo:$latitude,$longitude"
    }

    override fun decode(data: String): Map<String, String> {
        val stripped = data.removePrefix("geo:")
        val parts = stripped.split(",", limit = 2)
        return mapOf(
            "latitude" to parts.getOrElse(0) { "" },
            "longitude" to parts.getOrElse(1) { "" },
        )
    }
}
