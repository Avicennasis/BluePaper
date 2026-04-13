package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.json.Json
import java.io.File

actual object TemplateStorage {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dir = File(System.getProperty("user.home"), ".bluepaper/templates").also { it.mkdirs() }

    private fun slugify(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    actual fun save(template: LabelTemplate) {
        val file = File(dir, "${slugify(template.name)}.json")
        file.writeText(json.encodeToString(LabelTemplate.serializer(), template))
    }

    actual fun loadAll(): List<LabelTemplate> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }?.mapNotNull { file ->
            try {
                json.decodeFromString(LabelTemplate.serializer(), file.readText())
            } catch (_: Exception) { null }
        } ?: emptyList()
    }

    actual fun delete(name: String) {
        val file = File(dir, "${slugify(name)}.json")
        file.delete()
    }
}
