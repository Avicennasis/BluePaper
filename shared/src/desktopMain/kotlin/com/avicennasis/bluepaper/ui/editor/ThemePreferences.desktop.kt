package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ui.theme.ThemeMode
import java.io.File

actual object ThemePreferences {
    private val settingsDir = File(System.getProperty("user.home"), ".bluepaper")
    private val settingsFile = File(settingsDir, "settings.json")

    actual fun save(mode: ThemeMode) {
        settingsDir.mkdirs()
        settingsFile.writeText("""{"themeMode":"${mode.name}"}""")
    }

    actual fun load(): ThemeMode {
        if (!settingsFile.exists()) return ThemeMode.System
        return try {
            val content = settingsFile.readText()
            val modeStr = Regex(""""themeMode"\s*:\s*"(\w+)"""").find(content)?.groupValues?.get(1)
            ThemeMode.valueOf(modeStr ?: "System")
        } catch (_: Exception) {
            ThemeMode.System
        }
    }
}
