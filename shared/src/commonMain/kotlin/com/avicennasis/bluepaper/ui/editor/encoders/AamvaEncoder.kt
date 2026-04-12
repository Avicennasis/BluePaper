package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class AamvaEncoder : DataEncoder {

    override val standard = DataStandard.AAMVA

    override fun fields(): List<DataField> = listOf(
        DataField("firstName", "First Name", required = true),
        DataField("lastName", "Last Name", required = true),
        DataField("dob", "Date of Birth", required = true, hint = "MMDDYYYY"),
        DataField("licenseNumber", "License Number", required = true),
        DataField("street", "Street"),
        DataField("city", "City", required = true),
        DataField("state", "State", required = true, hint = "PA"),
        DataField("zip", "ZIP Code", required = true),
        DataField("country", "Country", hint = "USA"),
        DataField("sex", "Sex"),
        DataField("height", "Height"),
        DataField("weight", "Weight"),
        DataField("eyeColor", "Eye Color"),
        DataField("hairColor", "Hair Color"),
        DataField("expirationDate", "Expiration Date"),
    )

    override fun encode(fields: Map<String, String>): String {
        return buildString {
            appendLine("@")
            appendLine()
            appendLine("ANSI 636000080002DL00410278ZZ03080015DLDAA")
            for ((key, tag) in FIELD_TAGS) {
                val value = fields[key]
                if (!value.isNullOrEmpty()) {
                    appendLine("$tag$value")
                }
            }
        }.trimEnd()
    }

    override fun decode(data: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val tagToKey = FIELD_TAGS.entries.associate { (key, tag) -> tag to key }

        for (line in data.lines()) {
            val trimmed = line.trim()
            if (trimmed.length < 3) continue
            val tag = trimmed.take(3)
            val key = tagToKey[tag]
            if (key != null) {
                val value = trimmed.drop(3)
                if (value.isNotEmpty()) {
                    result[key] = value
                }
            }
        }
        return result
    }

    companion object {
        private val FIELD_TAGS = linkedMapOf(
            "lastName" to "DCS",
            "firstName" to "DAC",
            "dob" to "DBB",
            "licenseNumber" to "DAQ",
            "street" to "DAG",
            "city" to "DAI",
            "state" to "DAJ",
            "zip" to "DAK",
            "country" to "DCG",
            "sex" to "DBC",
            "height" to "DAU",
            "weight" to "DAW",
            "eyeColor" to "DAY",
            "hairColor" to "DAZ",
            "expirationDate" to "DBA",
        )
    }
}
