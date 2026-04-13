package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class VCardEncoder : DataEncoder {

    override val standard = DataStandard.VCARD

    override fun fields(): List<DataField> = listOf(
        DataField("firstName", "First Name"),
        DataField("lastName", "Last Name", required = true),
        DataField("organization", "Organization"),
        DataField("title", "Title"),
        DataField("phone", "Phone", required = true, fieldType = FieldType.PHONE),
        DataField("email", "Email", required = true, fieldType = FieldType.EMAIL),
        DataField("street", "Street"),
        DataField("city", "City"),
        DataField("state", "State"),
        DataField("zip", "ZIP Code"),
        DataField("country", "Country"),
        DataField("url", "URL", fieldType = FieldType.URL),
        DataField("note", "Note"),
    )

    override fun encode(fields: Map<String, String>): String {
        val first = fields["firstName"].orEmpty()
        val last = fields["lastName"].orEmpty()
        val fullName = buildString {
            if (first.isNotEmpty()) append(first)
            if (first.isNotEmpty() && last.isNotEmpty()) append(" ")
            if (last.isNotEmpty()) append(last)
        }.ifEmpty { "Unknown" }

        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("N:$last;$first;;;")
            appendLine("FN:$fullName")
            fields["organization"]?.takeIf { it.isNotEmpty() }?.let { appendLine("ORG:$it") }
            fields["title"]?.takeIf { it.isNotEmpty() }?.let { appendLine("TITLE:$it") }
            fields["phone"]?.takeIf { it.isNotEmpty() }?.let { appendLine("TEL:$it") }
            fields["email"]?.takeIf { it.isNotEmpty() }?.let { appendLine("EMAIL:$it") }
            val street = fields["street"].orEmpty()
            val city = fields["city"].orEmpty()
            val state = fields["state"].orEmpty()
            val zip = fields["zip"].orEmpty()
            val country = fields["country"].orEmpty()
            if (listOf(street, city, state, zip, country).any { it.isNotEmpty() }) {
                appendLine("ADR:;;$street;$city;$state;$zip;$country")
            }
            fields["url"]?.takeIf { it.isNotEmpty() }?.let { appendLine("URL:$it") }
            fields["note"]?.takeIf { it.isNotEmpty() }?.let { appendLine("NOTE:$it") }
            append("END:VCARD")
        }
    }

    override fun decode(data: String): Map<String, String> {
        // Unfold RFC 6350 folded lines (continuation lines start with space or tab)
        val unfolded = data.replace("\r\n ", "").replace("\r\n\t", "").replace("\n ", "").replace("\n\t", "")
        val result = mutableMapOf<String, String>()
        for (line in unfolded.lines()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("N:") -> {
                    val parts = trimmed.removePrefix("N:").split(";")
                    if (parts.isNotEmpty()) result["lastName"] = parts[0]
                    if (parts.size > 1) result["firstName"] = parts[1]
                }
                trimmed.startsWith("ORG:") -> {
                    result["organization"] = trimmed.removePrefix("ORG:")
                }
                trimmed.startsWith("TITLE:") -> {
                    result["title"] = trimmed.removePrefix("TITLE:")
                }
                trimmed.startsWith("TEL:") || trimmed.startsWith("TEL;") -> {
                    result["phone"] = trimmed.substringAfter(":")
                }
                trimmed.startsWith("EMAIL:") || trimmed.startsWith("EMAIL;") -> {
                    result["email"] = trimmed.substringAfter(":")
                }
                trimmed.startsWith("ADR:") || trimmed.startsWith("ADR;") -> {
                    val parts = trimmed.substringAfter(":").split(";")
                    if (parts.size > 2) result["street"] = parts[2]
                    if (parts.size > 3) result["city"] = parts[3]
                    if (parts.size > 4) result["state"] = parts[4]
                    if (parts.size > 5) result["zip"] = parts[5]
                    if (parts.size > 6) result["country"] = parts[6]
                }
                trimmed.startsWith("URL:") -> {
                    result["url"] = trimmed.removePrefix("URL:")
                }
                trimmed.startsWith("NOTE:") -> {
                    result["note"] = trimmed.removePrefix("NOTE:")
                }
            }
        }
        return result
    }
}
