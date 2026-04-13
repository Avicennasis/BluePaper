package com.avicennasis.bluepaper.ui

import androidx.compose.runtime.*
import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ui.editor.EditorScreen
import com.avicennasis.bluepaper.ui.editor.EditorState
import com.avicennasis.bluepaper.ui.editor.ThemePreferences
import com.avicennasis.bluepaper.ui.scanner.ScannerScreen
import com.avicennasis.bluepaper.ui.scanner.ScannerState
import com.avicennasis.bluepaper.ui.theme.BluePaperTheme
import com.avicennasis.bluepaper.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Screen { Scanner, Editor }

@Composable
fun BluePaperApp(
    scanner: BleScanner,
    transport: BleTransport,
    scope: CoroutineScope,
    startScreen: Screen = Screen.Scanner,
) {
    var currentScreen by remember { mutableStateOf(startScreen) }
    var themeMode by remember { mutableStateOf(ThemeMode.System) }

    // Load persisted theme preference asynchronously to avoid synchronous file I/O
    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { ThemePreferences.load() }
        themeMode = saved
    }

    val scannerState = remember { ScannerState(scanner, transport, scope) }
    val editorState = remember { EditorState(transport, scope) }

    BluePaperTheme(themeMode = themeMode) {
        when (currentScreen) {
            Screen.Scanner -> ScannerScreen(
                state = scannerState,
                onConnected = { currentScreen = Screen.Editor },
            )
            Screen.Editor -> EditorScreen(
                state = editorState,
                themeMode = themeMode,
                onThemeToggle = {
                    themeMode = when (themeMode) {
                        ThemeMode.System -> ThemeMode.Light
                        ThemeMode.Light -> ThemeMode.Dark
                        ThemeMode.Dark -> ThemeMode.System
                    }
                    ThemePreferences.save(themeMode)
                },
                onDisconnect = {
                    scannerState.disconnect()
                    currentScreen = Screen.Scanner
                },
            )
        }
    }
}
