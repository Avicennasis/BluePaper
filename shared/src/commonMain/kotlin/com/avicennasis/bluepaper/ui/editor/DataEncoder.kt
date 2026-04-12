package com.avicennasis.bluepaper.ui.editor

interface DataEncoder {
    val standard: DataStandard
    fun fields(): List<DataField>
    fun encode(fields: Map<String, String>): String
    fun decode(data: String): Map<String, String>
}

object DataEncoderRegistry {
    private val encoders = mutableMapOf<DataStandard, DataEncoder>()

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
