package com.avicennasis.bluepaper

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.avicennasis.bluepaper.ble.DesktopBleFactory
import com.avicennasis.bluepaper.ui.BluePaperApp
import com.avicennasis.bluepaper.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Log platform detection
    println("[BluePaper] Platform: ${System.getProperty("os.name")}")
    println("[BluePaper] Linux BLE: ${DesktopBleFactory.isLinux}")

    Window(
        onCloseRequest = ::exitApplication,
        title = "BluePaper",
        state = rememberWindowState(width = 1200.dp, height = 800.dp),
    ) {
        BluePaperApp(
            scanner = DesktopBleFactory.createScanner(),
            transport = DesktopBleFactory.createTransport(),
            scope = scope,
            startScreen = if (DesktopBleFactory.isLinux) Screen.Scanner else Screen.Editor,
        )
    }
}
