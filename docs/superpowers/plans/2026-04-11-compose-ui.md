# BluePaper Compose UI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Compose Multiplatform UI — a scanner screen to find printers, a label editor with live preview, and a print dialog with progress — wiring everything to the PrinterClient, MonochromeEncoder, and BLE transport layers.

**Architecture:** All UI lives in `commonMain` using Compose Multiplatform. A `BluePaperApp` composable hosts a simple two-screen navigation: ScannerScreen (discover + connect to a printer) and EditorScreen (design label + print). State management uses Compose `ViewModel`-style classes with `StateFlow`. The editor renders label content in a Compose `Canvas`, captures it via `LabelRenderer`, and sends it to `PrinterClient.print()`.

**Tech Stack:** Compose Multiplatform (Material3), Compose Navigation (manual stack), kotlinx-coroutines

**Depends on:** Plans 1-3 (protocol, BLE transport, image processing)

---

## File Structure

```
shared/src/
  commonMain/kotlin/com/avicennasis/bluepaper/
    ui/
      BluePaperApp.kt               # Root composable with screen navigation
      theme/
        Theme.kt                    # Material3 theme + colors
      scanner/
        ScannerScreen.kt            # BLE scanner UI — list printers, connect
        ScannerState.kt             # State holder for scanner screen
      editor/
        EditorScreen.kt             # Label editor — text input, canvas preview, print
        EditorState.kt              # State holder for editor screen
        LabelCanvas.kt              # Compose Canvas preview of label
        PrintDialog.kt              # Print settings + progress dialog

androidApp/src/main/kotlin/com/avicennasis/bluepaper/
  MainActivity.kt                   # Updated: hosts BluePaperApp

desktopApp/src/main/kotlin/com/avicennasis/bluepaper/
  Main.kt                           # Updated: hosts BluePaperApp
```

---

### Task 1: Material3 Theme

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/theme/Theme.kt`

- [ ] **Step 1: Create theme**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/theme/Theme.kt
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
```

- [ ] **Step 2: Verify compilation**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop`

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/theme/Theme.kt
git commit -m "feat: BluePaper Material3 theme with light/dark color schemes"
```

---

### Task 2: Scanner State + Screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/scanner/ScannerState.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/scanner/ScannerScreen.kt`

- [ ] **Step 1: Create ScannerState**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/scanner/ScannerState.kt
package com.avicennasis.bluepaper.ui.scanner

import com.avicennasis.bluepaper.ble.BleScanner
import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.ble.ConnectionState
import com.avicennasis.bluepaper.ble.ScannedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScannerState(
    private val scanner: BleScanner,
    private val transport: BleTransport,
    private val scope: CoroutineScope,
) {
    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    val connectionState: StateFlow<ConnectionState> = transport.connectionState

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var scanJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _devices.value = emptyList()
        _error.value = null

        scanJob = scope.launch {
            try {
                // Scan for all known model prefixes
                val prefixes = listOf("d110", "d11", "d101", "d110_m", "b18", "b21", "b1")
                for (prefix in prefixes) {
                    scanner.scan(prefix).collect { device ->
                        val current = _devices.value
                        if (current.none { it.address == device.address }) {
                            _devices.value = current + device
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false
    }

    fun connectTo(device: ScannedDevice) {
        scope.launch {
            try {
                _error.value = null
                transport.connect(device)
            } catch (e: Exception) {
                _error.value = "Connection failed: ${e.message}"
            }
        }
    }

    fun disconnect() {
        scope.launch {
            transport.disconnect()
        }
    }
}
```

- [ ] **Step 2: Create ScannerScreen**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/scanner/ScannerScreen.kt
package com.avicennasis.bluepaper.ui.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.ble.ConnectionState

@Composable
fun ScannerScreen(
    state: ScannerState,
    onConnected: () -> Unit,
) {
    val devices by state.devices.collectAsState()
    val isScanning by state.isScanning.collectAsState()
    val connectionState by state.connectionState.collectAsState()
    val error by state.error.collectAsState()

    // Navigate when connected
    if (connectionState == ConnectionState.CONNECTED) {
        onConnected()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text(
            text = "BluePaper",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Find your Niimbot printer",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        // Scan button
        Button(
            onClick = { if (isScanning) state.stopScan() else state.startScan() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Scanning...")
            } else {
                Text("Scan for Printers")
            }
        }

        // Error
        error?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        // Connecting indicator
        if (connectionState == ConnectionState.CONNECTING) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Connecting...", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        // Device list
        if (devices.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No printers found. Tap Scan to search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { state.connectTo(device) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(device.address, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop`

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/scanner/
git commit -m "feat: ScannerScreen — BLE device discovery UI with connect"
```

---

### Task 3: Editor State + Label Canvas

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorState.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelCanvas.kt`

- [ ] **Step 1: Create EditorState**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorState.kt
package com.avicennasis.bluepaper.ui.editor

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.config.LabelSize
import com.avicennasis.bluepaper.image.LabelRenderer
import com.avicennasis.bluepaper.printer.PrinterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PrintProgress(
    val completed: Int,
    val total: Int,
    val isPrinting: Boolean,
    val error: String? = null,
)

class EditorState(
    private val transport: BleTransport,
    private val scope: CoroutineScope,
) {
    private val client = PrinterClient(transport)

    private val _labelText = MutableStateFlow("Hello")
    val labelText: StateFlow<String> = _labelText

    private val _fontSize = MutableStateFlow(24f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _selectedModel = MutableStateFlow(DeviceRegistry.get("d110")!!)
    val selectedModel: StateFlow<DeviceConfig> = _selectedModel

    private val _selectedLabelSize = MutableStateFlow(_selectedModel.value.labelSizes.first())
    val selectedLabelSize: StateFlow<LabelSize> = _selectedLabelSize

    private val _density = MutableStateFlow(3)
    val density: StateFlow<Int> = _density

    private val _quantity = MutableStateFlow(1)
    val quantity: StateFlow<Int> = _quantity

    private val _printProgress = MutableStateFlow(PrintProgress(0, 0, false))
    val printProgress: StateFlow<PrintProgress> = _printProgress

    fun setLabelText(text: String) { _labelText.value = text }
    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setDensity(d: Int) { _density.value = d.coerceIn(1, _selectedModel.value.maxDensity) }
    fun setQuantity(q: Int) { _quantity.value = q.coerceIn(1, 100) }

    fun selectModel(modelName: String) {
        val config = DeviceRegistry.get(modelName) ?: return
        _selectedModel.value = config
        _selectedLabelSize.value = config.labelSizes.first()
        _density.value = _density.value.coerceAtMost(config.maxDensity)
    }

    fun selectLabelSize(size: LabelSize) {
        _selectedLabelSize.value = size
    }

    fun print(imageRows: List<ByteArray>, width: Int, height: Int) {
        val config = _selectedModel.value
        val qty = _quantity.value
        val dens = _density.value

        _printProgress.value = PrintProgress(0, qty, true)

        scope.launch {
            try {
                client.print(
                    imageRows = imageRows,
                    width = width,
                    height = height,
                    density = dens,
                    quantity = qty,
                    isV2 = config.isV2,
                    onProgress = { completed, total ->
                        _printProgress.value = PrintProgress(completed, total, true)
                    },
                )
                _printProgress.value = PrintProgress(qty, qty, false)
            } catch (e: Exception) {
                _printProgress.value = PrintProgress(0, 0, false, error = e.message)
            }
        }
    }
}
```

- [ ] **Step 2: Create LabelCanvas**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/LabelCanvas.kt
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Live preview of the label content. Draws the same content
 * that will be rendered to the printer (white background, black text).
 */
@Composable
fun LabelCanvas(
    text: String,
    fontSize: Float,
    widthPx: Int,
    heightPx: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val ratio = widthPx.toFloat() / heightPx.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        // White background
        drawRect(Color.White)

        // Draw text
        if (text.isNotEmpty()) {
            val style = TextStyle(
                fontSize = fontSize.sp,
                color = Color.Black,
            )
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(8f, 8f),
                style = style,
            )
        }

        // Border
        drawRect(Color.LightGray, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    }
}

/**
 * Draw the same label content into a DrawScope (used by LabelRenderer for printing).
 */
fun drawLabelContent(
    scope: DrawScope,
    text: String,
    fontSize: Float,
    textMeasurer: TextMeasurer,
) {
    scope.drawRect(Color.White) // background
    if (text.isNotEmpty()) {
        scope.drawText(
            textMeasurer = textMeasurer,
            text = text,
            topLeft = Offset(8f, 8f),
            style = TextStyle(fontSize = fontSize.sp, color = Color.Black),
        )
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop`

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/
git commit -m "feat: EditorState + LabelCanvas — label editor state management and canvas preview"
```

---

### Task 4: Editor Screen + Print Dialog

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PrintDialog.kt`
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt`

- [ ] **Step 1: Create PrintDialog**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PrintDialog.kt
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrintDialog(
    progress: PrintProgress,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!progress.isPrinting) onDismiss() },
        title = {
            Text(
                when {
                    progress.isPrinting -> "Printing..."
                    progress.error != null -> "Print Error"
                    else -> "Print Complete"
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                when {
                    progress.isPrinting -> {
                        LinearProgressIndicator(
                            progress = { if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Page ${progress.completed} of ${progress.total}")
                    }
                    progress.error != null -> {
                        Text(progress.error, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        Text("All ${progress.total} labels printed successfully.")
                    }
                }
            }
        },
        confirmButton = {
            if (!progress.isPrinting) {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
    )
}
```

- [ ] **Step 2: Create EditorScreen**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt
package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.config.DeviceRegistry
import com.avicennasis.bluepaper.image.LabelRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onDisconnect: () -> Unit,
) {
    val labelText by state.labelText.collectAsState()
    val fontSize by state.fontSize.collectAsState()
    val selectedModel by state.selectedModel.collectAsState()
    val selectedLabelSize by state.selectedLabelSize.collectAsState()
    val density by state.density.collectAsState()
    val quantity by state.quantity.collectAsState()
    val printProgress by state.printProgress.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    var showPrintDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Label Editor", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = onDisconnect) { Text("Disconnect") }
        }

        Spacer(Modifier.height(16.dp))

        // Model selector
        Text("Printer Model", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        var modelExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { modelExpanded = it }) {
            OutlinedTextField(
                value = selectedModel.model.uppercase(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
            )
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                DeviceRegistry.models().forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.uppercase()) },
                        onClick = { state.selectModel(model); modelExpanded = false },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Label size selector
        Text("Label Size", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedModel.labelSizes.forEach { size ->
                FilterChip(
                    selected = size == selectedLabelSize,
                    onClick = { state.selectLabelSize(size) },
                    label = { Text(size.displayName) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Label preview
        Text("Preview", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        LabelCanvas(
            text = labelText,
            fontSize = fontSize,
            widthPx = selectedLabelSize.widthPx,
            heightPx = selectedLabelSize.heightPx,
            textMeasurer = textMeasurer,
        )

        Spacer(Modifier.height(16.dp))

        // Text input
        OutlinedTextField(
            value = labelText,
            onValueChange = { state.setLabelText(it) },
            label = { Text("Label Text") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        // Font size slider
        Text("Font Size: ${fontSize.toInt()}sp", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = fontSize,
            onValueChange = { state.setFontSize(it) },
            valueRange = 8f..72f,
        )

        Spacer(Modifier.height(8.dp))

        // Density + Quantity
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Density: $density", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = density.toFloat(),
                    onValueChange = { state.setDensity(it.toInt()) },
                    valueRange = 1f..selectedModel.maxDensity.toFloat(),
                    steps = selectedModel.maxDensity - 2,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Copies: $quantity", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = quantity.toFloat(),
                    onValueChange = { state.setQuantity(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Print button
        Button(
            onClick = {
                val w = selectedLabelSize.widthPx
                val h = selectedLabelSize.heightPx
                val rows = LabelRenderer.render(w, h) { drawScope ->
                    drawLabelContent(drawScope, labelText, fontSize, textMeasurer)
                }
                state.print(rows, w, h)
                showPrintDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Print", style = MaterialTheme.typography.titleMedium)
        }
    }

    // Print dialog
    if (showPrintDialog) {
        PrintDialog(
            progress = printProgress,
            onDismiss = { showPrintDialog = false },
        )
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop`

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/PrintDialog.kt \
       shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/editor/EditorScreen.kt
git commit -m "feat: EditorScreen + PrintDialog — label editor with model/size selection and print"
```

---

### Task 5: BluePaperApp Root + Update App Entry Points

**Files:**
- Create: `shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt`
- Modify: `androidApp/src/main/kotlin/com/avicennasis/bluepaper/MainActivity.kt`
- Modify: `desktopApp/src/main/kotlin/com/avicennasis/bluepaper/Main.kt`

- [ ] **Step 1: Create BluePaperApp root composable**

```kotlin
// shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt
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
```

- [ ] **Step 2: Update Android MainActivity**

```kotlin
// androidApp/src/main/kotlin/com/avicennasis/bluepaper/MainActivity.kt
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
```

- [ ] **Step 3: Update Desktop Main.kt**

For desktop, we need a placeholder scanner/transport since Kable doesn't support desktop JVM BLE yet. Create stub implementations.

```kotlin
// desktopApp/src/main/kotlin/com/avicennasis/bluepaper/Main.kt
package com.avicennasis.bluepaper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.avicennasis.bluepaper.ble.*
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import com.avicennasis.bluepaper.ui.BluePaperApp

/** Stub scanner for desktop — BLE not yet supported. */
class DesktopBleScanner : BleScanner {
    override fun scan(namePrefix: String): Flow<ScannedDevice> = emptyFlow()
}

/** Stub transport for desktop — BLE not yet supported. */
class DesktopBleTransport : BleTransport {
    override val connectionState: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.DISCONNECTED)
    override suspend fun connect(device: ScannedDevice) { }
    override suspend fun disconnect() { }
    override suspend fun sendCommand(packet: NiimbotPacket, timeoutMs: Long): NiimbotPacket =
        NiimbotPacket(packet.type, byteArrayOf(0x01))
    override suspend fun writeRaw(packet: NiimbotPacket) { }
}

fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    Window(onCloseRequest = ::exitApplication, title = "BluePaper") {
        BluePaperApp(DesktopBleScanner(), DesktopBleTransport(), scope)
    }
}
```

- [ ] **Step 4: Verify compilation (both targets)**

Run: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:compileKotlinDesktop :desktopApp:compileKotlinDesktop`
Then: `cd ~/github/BluePaper && ANDROID_HOME=~/Android/Sdk ./gradlew :shared:desktopTest`
Expected: all 93+ tests still pass

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/avicennasis/bluepaper/ui/BluePaperApp.kt \
       androidApp/src/main/kotlin/com/avicennasis/bluepaper/MainActivity.kt \
       desktopApp/src/main/kotlin/com/avicennasis/bluepaper/Main.kt
git commit -m "feat: BluePaperApp root composable + updated Android/Desktop entry points"
```

---

## Self-Review Checklist

**1. Spec coverage:**
- [x] Material3 theme (Task 1)
- [x] Scanner screen — discover BLE devices, show list, connect (Task 2)
- [x] Editor screen — model selector, label size, text input, font size (Tasks 3-4)
- [x] Label preview canvas with live text rendering (Task 3)
- [x] Print button with LabelRenderer pipeline (Task 4)
- [x] Print dialog with progress bar and error display (Task 4)
- [x] BluePaperApp root with Scanner→Editor navigation (Task 5)
- [x] Android entry point with KableBleScanner/Transport (Task 5)
- [x] Desktop entry point with stub scanner/transport (Task 5)

**2. Placeholder scan:** No TBDs. Desktop stubs are documented as intentional ("BLE not yet supported").

**3. Type consistency:**
- `ScannerState(scanner: BleScanner, transport: BleTransport, scope: CoroutineScope)` — consistent
- `EditorState(transport: BleTransport, scope: CoroutineScope)` — consistent
- `BluePaperApp(scanner, transport, scope)` — matches both state constructors
- `LabelRenderer.render()` returns `List<ByteArray>` → `EditorState.print(imageRows: List<ByteArray>)` → `PrinterClient.print(imageRows: List<ByteArray>)` — consistent chain
