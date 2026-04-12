package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.editor.encoders.*

interface DataEncoder {
    val standard: DataStandard
    fun fields(): List<DataField>
    fun encode(fields: Map<String, String>): String
    fun decode(data: String): Map<String, String>
}

object DataEncoderRegistry {
    private val encoders = mutableMapOf<DataStandard, DataEncoder>()

    init {
        register(VCardEncoder())
        register(UrlEncoder())
        register(WifiEncoder())
        register(MeCardEncoder())
        register(SmsEncoder)
        register(EmailEncoder)
        register(PhoneEncoder)
        register(GeoEncoder)
        register(AamvaEncoder())
        register(Gs1128Encoder())
        register(Gs1DataMatrixEncoder())
        register(HibcEncoder())
    }

    fun register(encoder: DataEncoder) {
        encoders[encoder.standard] = encoder
    }

    fun get(standard: DataStandard): DataEncoder? = encoders[standard]

    fun encode(standard: DataStandard, fields: Map<String, String>): String =
        encoders[standard]?.encode(fields) ?: fields.values.joinToString(" ")

    fun decode(standard: DataStandard, data: String): Map<String, String> =
        encoders[standard]?.decode(data) ?: mapOf("data" to data)

    fun fieldsFor(standard: DataStandard): List<DataField> =
        encoders[standard]?.fields() ?: listOf(DataField("data", "Data", required = true))
}
