package com.avicennasis.bluepaper.ui.editor

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

actual object TemplateStorage {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private lateinit var dir: File

    fun init(context: Context) {
        dir = File(context.filesDir, "templates").also { it.mkdirs() }
    }

    private fun checkInitialized() {
        check(::dir.isInitialized) {
            "TemplateStorage.init(context) must be called before using template operations"
        }
    }

    private fun slugify(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun resolveFile(slug: String, templateName: String): File {
        val base = File(dir, "$slug.json")
        if (!base.exists()) return base
        try {
            val existing = json.decodeFromString(LabelTemplate.serializer(), base.readText())
            if (existing.name == templateName) return base
        } catch (_: Exception) { }
        var suffix = 2
        while (true) {
            val candidate = File(dir, "$slug-$suffix.json")
            if (!candidate.exists()) return candidate
            try {
                val existing = json.decodeFromString(LabelTemplate.serializer(), candidate.readText())
                if (existing.name == templateName) return candidate
            } catch (_: Exception) { }
            suffix++
        }
    }

    actual fun save(template: LabelTemplate) {
        checkInitialized()
        val file = resolveFile(slugify(template.name), template.name)
        file.writeText(json.encodeToString(LabelTemplate.serializer(), template))
    }

    actual fun loadAll(): List<LabelTemplate> {
        checkInitialized()
        return dir.listFiles { f -> f.extension == "json" }?.mapNotNull { file ->
            try {
                json.decodeFromString(LabelTemplate.serializer(), file.readText())
            } catch (_: Exception) { null }
        } ?: emptyList()
    }

    actual fun delete(name: String) {
        checkInitialized()
        val files = dir.listFiles { f -> f.extension == "json" } ?: return
        for (file in files) {
            try {
                val template = json.decodeFromString(LabelTemplate.serializer(), file.readText())
                if (template.name == name) {
                    file.delete()
                    return
                }
            } catch (_: Exception) { }
        }
    }
}
