package com.avicennasis.bluepaper.ui.editor

import android.content.Context
import android.content.SharedPreferences
import com.avicennasis.bluepaper.ui.theme.ThemeMode

actual object ThemePreferences {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("bluepaper_prefs", Context.MODE_PRIVATE)
    }

    actual fun save(mode: ThemeMode) {
        prefs?.edit()?.putString("themeMode", mode.name)?.apply()
    }

    actual fun load(): ThemeMode {
        val name = prefs?.getString("themeMode", null) ?: return ThemeMode.System
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.System)
    }
}
