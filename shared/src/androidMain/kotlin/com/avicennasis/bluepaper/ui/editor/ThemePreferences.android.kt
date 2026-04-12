package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode

actual object ThemePreferences {
    actual fun save(mode: ThemeMode) { }
    actual fun load(): ThemeMode = ThemeMode.System
}
