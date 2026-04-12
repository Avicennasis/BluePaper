package com.avicennasis.bluepaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.avicennasis.bluepaper.ble.KableBleScanner
import com.avicennasis.bluepaper.ble.KableBleTransport
import com.avicennasis.bluepaper.ui.BluePaperApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = KableBleScanner()
        val transport = KableBleTransport()
        setContent {
            BluePaperApp(scanner, transport, lifecycleScope)
        }
    }
}
