package com.avicennasis.bluepaper.ui

import androidx.compose.runtime.*
import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ui.editor.EditorScreen
import com.avicennasis.bluepaper.ui.editor.EditorState
import com.avicennasis.bluepaper.ui.scanner.ScannerScreen
import com.avicennasis.bluepaper.ui.scanner.ScannerState
import com.avicennasis.bluepaper.ui.theme.BluePaperTheme
import kotlinx.coroutines.CoroutineScope

enum class Screen { Scanner, Editor }

@Composable
fun BluePaperApp(
    scanner: BleScanner,
    transport: BleTransport,
    scope: CoroutineScope,
) {
    var currentScreen by remember { mutableStateOf(Screen.Scanner) }

    val scannerState = remember { ScannerState(scanner, transport, scope) }
    val editorState = remember { EditorState(transport, scope) }

    BluePaperTheme {
        when (currentScreen) {
            Screen.Scanner -> ScannerScreen(
                state = scannerState,
                onConnected = { currentScreen = Screen.Editor },
            )
            Screen.Editor -> EditorScreen(
                state = editorState,
                onDisconnect = {
                    scannerState.disconnect()
                    currentScreen = Screen.Scanner
                },
            )
        }
    }
}
