package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.text.font.FontFamily

data class FontEntry(
    val key: String,
    val displayName: String,
    val category: String,
    val family: FontFamily,
)

object FontRegistry {
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

    private val entries: List<FontEntry> by lazy { buildEntries() }

    private var customFonts: Map<String, FontFamily> = emptyMap()

    private fun buildEntries(): List<FontEntry> =
        defaultEntries.map { (key, name, category) ->
            FontEntry(key, name, category, customFonts[key] ?: FontFamily.Default)
        }

    fun init(fonts: Map<String, FontFamily> = emptyMap()) {
        customFonts = fonts
    }

    // Variant registry for future font addon packages
    private data class VariantKey(val fontKey: String, val weight: Int, val style: String)
    private val variants = mutableMapOf<VariantKey, FontFamily>()

    fun registerVariant(key: String, weight: Int, style: String, family: FontFamily) {
        variants[VariantKey(key, weight, style)] = family
    }

    fun get(key: String): FontFamily =
        entries.find { it.key == key }?.family ?: entries.first().family

    fun get(key: String, weight: Int, style: String): FontFamily {
        // Check for a registered real variant first
        variants[VariantKey(key, weight, style)]?.let { return it }
        // Fall back to the base font — Compose will apply synthetic weight/style
        return get(key)
    }

    fun allFonts(): List<FontEntry> = entries.toList()

    fun nameFor(key: String): String =
        entries.find { it.key == key }?.displayName ?: "Default"

    fun categories(): List<String> =
        entries.map { it.category }.distinct()

    fun fontsInCategory(category: String): List<FontEntry> =
        entries.filter { it.category == category }
}
