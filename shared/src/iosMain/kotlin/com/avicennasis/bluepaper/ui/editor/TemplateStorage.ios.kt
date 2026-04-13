package com.avicennasis.bluepaper.ui.editor

actual object TemplateStorage {
    actual fun save(template: LabelTemplate) { }
    actual fun loadAll(): List<LabelTemplate> = emptyList()
    actual fun delete(name: String) { }
}
