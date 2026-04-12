package com.avicennasis.bluepaper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BluePaperBlue = Color(0xFF1565C0)
private val BluePaperLightBlue = Color(0xFF42A5F5)

private val LightColors = lightColorScheme(
    primary = BluePaperBlue,
    secondary = BluePaperLightBlue,
)

private val DarkColors = darkColorScheme(
    primary = BluePaperLightBlue,
    secondary = BluePaperBlue,
)

@Composable
fun BluePaperTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
