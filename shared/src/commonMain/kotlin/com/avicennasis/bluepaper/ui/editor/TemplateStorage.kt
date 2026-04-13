package com.avicennasis.bluepaper.ui.editor

expect object TemplateStorage {
    fun save(template: LabelTemplate)
    fun loadAll(): List<LabelTemplate>
    fun delete(name: String)
}
