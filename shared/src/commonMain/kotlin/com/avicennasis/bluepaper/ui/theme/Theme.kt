package com.avicennasis.bluepaper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { System, Light, Dark }

private val BluePaperBlue = Color(0xFF1565C0)
private val BluePaperLightBlue = Color(0xFF42A5F5)

private val LightColors = lightColorScheme(
    primary = BluePaperBlue,
    secondary = BluePaperLightBlue,
    surface = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    onSurface = Color(0xFF212121),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
)

private val DarkColors = darkColorScheme(
    primary = BluePaperLightBlue,
    secondary = BluePaperBlue,
    surface = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF333333),
    onSurface = Color(0xFFE0E0E0),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF444444),
)

@Composable
fun BluePaperTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content,
    )
}
