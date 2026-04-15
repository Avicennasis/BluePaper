package com.avicennasis.bluepaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.avicennasis.bluepaper.ble.KableBleScanner
import com.avicennasis.bluepaper.ble.KableBleTransport
import com.avicennasis.bluepaper.ui.BlePermissionHandler
import com.avicennasis.bluepaper.ui.BluePaperApp
import com.avicennasis.bluepaper.ui.editor.ThemePreferences
import com.avicennasis.bluepaper.ui.editor.TemplateStorage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        TemplateStorage.init(this)
        val transport = KableBleTransport()
        val scanner = KableBleScanner(transport)
        setContent {
            BlePermissionHandler {
                BluePaperApp(scanner, transport, lifecycleScope)
            }
        }
    }
}
