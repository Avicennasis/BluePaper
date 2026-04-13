package com.avicennasis.bluepaper.ui.editor.encoders

import com.avicennasis.bluepaper.ui.editor.DataEncoder
import com.avicennasis.bluepaper.ui.editor.DataField
import com.avicennasis.bluepaper.ui.editor.DataStandard
import com.avicennasis.bluepaper.ui.editor.FieldType

class VCardEncoder : DataEncoder {

    override val standard = DataStandard.VCARD

    /** RFC 6350 property-value escaping. */
    private fun escapeVCard(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")
        .replace("\r", "")

    /** Reverse of [escapeVCard]. */
    private fun unescapeVCard(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")

    /**
     * Split a VCard structured value on unescaped semicolons.
     * Escaped semicolons (`\;`) are preserved within components.
     */
    private fun splitOnSemicolon(value: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                current.append(value[i])
                current.append(value[i + 1])
                i += 2
            } else if (value[i] == ';') {
                parts.add(current.toString())
                current.clear()
                i++
            } else {
                current.append(value[i])
                i++
            }
        }
        parts.add(current.toString())
        return parts
    }

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
            appendLine("N:${escapeVCard(last)};${escapeVCard(first)};;;")
            appendLine("FN:${escapeVCard(fullName)}")
            fields["organization"]?.takeIf { it.isNotEmpty() }?.let { appendLine("ORG:${escapeVCard(it)}") }
            fields["title"]?.takeIf { it.isNotEmpty() }?.let { appendLine("TITLE:${escapeVCard(it)}") }
            fields["phone"]?.takeIf { it.isNotEmpty() }?.let { appendLine("TEL:${escapeVCard(it)}") }
            fields["email"]?.takeIf { it.isNotEmpty() }?.let { appendLine("EMAIL:${escapeVCard(it)}") }
            val street = fields["street"].orEmpty()
            val city = fields["city"].orEmpty()
            val state = fields["state"].orEmpty()
            val zip = fields["zip"].orEmpty()
            val country = fields["country"].orEmpty()
            if (listOf(street, city, state, zip, country).any { it.isNotEmpty() }) {
                appendLine("ADR:;;${escapeVCard(street)};${escapeVCard(city)};${escapeVCard(state)};${escapeVCard(zip)};${escapeVCard(country)}")
            }
            fields["url"]?.takeIf { it.isNotEmpty() }?.let { appendLine("URL:${escapeVCard(it)}") }
            fields["note"]?.takeIf { it.isNotEmpty() }?.let { appendLine("NOTE:${escapeVCard(it)}") }
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
                    val parts = splitOnSemicolon(trimmed.removePrefix("N:"))
                    if (parts.isNotEmpty()) result["lastName"] = unescapeVCard(parts[0])
                    if (parts.size > 1) result["firstName"] = unescapeVCard(parts[1])
                }
                trimmed.startsWith("ORG:") -> {
                    result["organization"] = unescapeVCard(trimmed.removePrefix("ORG:"))
                }
                trimmed.startsWith("TITLE:") -> {
                    result["title"] = unescapeVCard(trimmed.removePrefix("TITLE:"))
                }
                trimmed.startsWith("TEL:") || trimmed.startsWith("TEL;") -> {
                    result["phone"] = unescapeVCard(trimmed.substringAfter(":"))
                }
                trimmed.startsWith("EMAIL:") || trimmed.startsWith("EMAIL;") -> {
                    result["email"] = unescapeVCard(trimmed.substringAfter(":"))
                }
                trimmed.startsWith("ADR:") || trimmed.startsWith("ADR;") -> {
                    val parts = splitOnSemicolon(trimmed.substringAfter(":"))
                    if (parts.size > 2) result["street"] = unescapeVCard(parts[2])
                    if (parts.size > 3) result["city"] = unescapeVCard(parts[3])
                    if (parts.size > 4) result["state"] = unescapeVCard(parts[4])
                    if (parts.size > 5) result["zip"] = unescapeVCard(parts[5])
                    if (parts.size > 6) result["country"] = unescapeVCard(parts[6])
                }
                trimmed.startsWith("URL:") -> {
                    result["url"] = unescapeVCard(trimmed.removePrefix("URL:"))
                }
                trimmed.startsWith("NOTE:") -> {
                    result["note"] = unescapeVCard(trimmed.removePrefix("NOTE:"))
                }
            }
        }
        return result
    }
}
