package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode

expect object ThemePreferences {
    fun save(mode: ThemeMode)
    fun load(): ThemeMode
}
