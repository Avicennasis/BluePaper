package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.text.font.FontFamily

data class FontEntry(
    val key: String,
    val displayName: String,
    val category: String,
    val family: FontFamily,
)

object FontRegistry {
    private val entries = mutableListOf<FontEntry>()
    private var initialized = false

    private val defaultEntries = listOf(
        Triple("default", "Default", "Sans-serif"),
        Triple("open_sans", "Open Sans", "Sans-serif"),
        Triple("inter", "Inter", "Sans-serif"),
        Triple("roboto_slab", "Roboto Slab", "Serif"),
        Triple("noto_serif", "Noto Serif", "Serif"),
        Triple("jetbrains_mono", "JetBrains Mono", "Monospace"),
        Triple("roboto_mono", "Roboto Mono", "Monospace"),
        Triple("oswald", "Oswald", "Display"),
        Triple("anton", "Anton", "Display"),
    )

    @Synchronized
    fun init(fonts: Map<String, FontFamily> = emptyMap()) {
        if (initialized) return
        entries.clear()
        for ((key, name, category) in defaultEntries) {
            entries.add(FontEntry(key, name, category, fonts[key] ?: FontFamily.Default))
        }
        initialized = true
    }

    // Variant registry for future font addon packages
    private data class VariantKey(val fontKey: String, val weight: Int, val style: String)
    private val variants = mutableMapOf<VariantKey, FontFamily>()

    fun registerVariant(key: String, weight: Int, style: String, family: FontFamily) {
        variants[VariantKey(key, weight, style)] = family
    }

    fun get(key: String): FontFamily {
        if (!initialized) init()
        return entries.find { it.key == key }?.family ?: entries.first().family
    }

    fun get(key: String, weight: Int, style: String): FontFamily {
        if (!initialized) init()
        // Check for a registered real variant first
        variants[VariantKey(key, weight, style)]?.let { return it }
        // Fall back to the base font — Compose will apply synthetic weight/style
        return get(key)
    }

    fun allFonts(): List<FontEntry> {
        if (!initialized) init()
        return entries.toList()
    }

    fun nameFor(key: String): String {
        if (!initialized) init()
        return entries.find { it.key == key }?.displayName ?: "Default"
    }

    fun categories(): List<String> {
        if (!initialized) init()
        return entries.map { it.category }.distinct()
    }

    fun fontsInCategory(category: String): List<FontEntry> {
        if (!initialized) init()
        return entries.filter { it.category == category }
    }
}
