package com.avicennasis.bluepaper.ui.editor

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

actual object TemplateStorage {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private var dir: File? = null

    fun init(context: Context) {
        dir = File(context.filesDir, "templates").also { it.mkdirs() }
    }

    private fun slugify(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    actual fun save(template: LabelTemplate) {
        val d = dir ?: return
        val file = File(d, "${slugify(template.name)}.json")
        file.writeText(json.encodeToString(LabelTemplate.serializer(), template))
    }

    actual fun loadAll(): List<LabelTemplate> {
        val d = dir ?: return emptyList()
        return d.listFiles { f -> f.extension == "json" }?.mapNotNull { file ->
            try {
                json.decodeFromString(LabelTemplate.serializer(), file.readText())
            } catch (_: Exception) { null }
        } ?: emptyList()
    }

    actual fun delete(name: String) {
        val d = dir ?: return
        File(d, "${slugify(name)}.json").delete()
    }
}
