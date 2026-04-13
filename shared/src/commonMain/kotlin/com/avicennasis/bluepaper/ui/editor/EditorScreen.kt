package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.image.LabelRenderer
import com.avicennasis.bluepaper.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorState,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeToggle: () -> Unit = {},
    onDisconnect: () -> Unit,
) {
    val elements by state.elements.collectAsState()
    val selectedElementId by state.selectedElementId.collectAsState()
    val selectedModel by state.selectedModel.collectAsState()
    val selectedLabelSize by state.selectedLabelSize.collectAsState()
    val density by state.density.collectAsState()
    val quantity by state.quantity.collectAsState()
    val printProgress by state.printProgress.collectAsState()
    val canUndo by state.canUndo.collectAsState()
    val canRedo by state.canRedo.collectAsState()
    val showGrid by state.showGrid.collectAsState()
    val gridSize by state.gridSize.collectAsState()

    val textMeasurer = rememberTextMeasurer()
    var showPrintDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showBarcodePicker by remember { mutableStateOf(false) }
    var saveRequested by remember { mutableStateOf(false) }
    var loadRequested by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) }

    val selectedElement = elements.find { it.id == selectedElementId }
    val focusRequester = remember { FocusRequester() }

    val monochromeRows by produceState<List<ByteArray>>(
        initialValue = emptyList(),
        key1 = elements,
        key2 = selectedLabelSize,
        key3 = selectedModel,
    ) {
        value = withContext(Dispatchers.Default) {
            val w = selectedLabelSize.widthPx
            val h = selectedLabelSize.heightPx
            if (w <= 0 || h <= 0) emptyList()
            else LabelRenderer.render(w, h, rotationDegrees = selectedModel.rotation) { scope ->
                drawElementsForPrint(scope, elements, textMeasurer)
            }
        }
    }
    val previewWidth = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.heightPx else selectedLabelSize.widthPx
    }
    val previewHeight = remember(selectedLabelSize, selectedModel) {
        val rot = selectedModel.rotation
        if (rot % 180 != 0) selectedLabelSize.widthPx else selectedLabelSize.heightPx
    }

    var showPropertiesSheet by remember { mutableStateOf(false) }
    val propertiesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show the bottom sheet automatically when an element is selected in compact mode
    LaunchedEffect(selectedElementId) {
        showPropertiesSheet = selectedElementId != null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopToolbar(
            canUndo = canUndo,
            canRedo = canRedo,
            themeMode = themeMode,
            showGrid = showGrid,
            onSave = { saveRequested = true },
            onLoad = { loadRequested = true },
            onUndo = { state.undo() },
            onRedo = { state.redo() },
            onTemplates = { showTemplateDialog = true },
            onSaveTemplate = { showSaveTemplateDialog = true },
            onThemeToggle = onThemeToggle,
            onGridToggle = { state.toggleGrid() },
            onDisconnect = onDisconnect,
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 800.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel: CompactToolbox (narrow) or full ToolboxPanel (wide)
                if (isCompact) {
                    CompactToolbox(
                        onAddText = { state.addTextElement() },
                        onAddImage = { state.addImageElement(it) },
                        onAddBarcode = { showBarcodePicker = true },
                    )
                } else {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                        ToolboxPanel(
                            elements = elements,
                            selectedElementId = selectedElementId,
                            selectedModel = selectedModel,
                            selectedLabelSize = selectedLabelSize,
                            density = density,
                            quantity = quantity,
                            onSelectElement = { state.selectElement(it) },
                            onDeleteElement = { state.removeElement(it) },
                            onAddText = { state.addTextElement() },
                            onAddImage = { state.addImageElement(it) },
                            onAddBarcode = { showBarcodePicker = true },
                            onSelectModel = { state.selectModel(it) },
                            onSelectLabelSize = { state.selectLabelSize(it) },
                            onDensityChange = { state.setDensity(it) },
                            onQuantityChange = { state.setQuantity(it) },
                        )
                    }
                }

                VerticalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && selectedElementId != null) {
                                val id = selectedElementId!!
                                val el = elements.find { it.id == id } ?: return@onKeyEvent false
                                val step = if (event.isShiftPressed) 10f else 1f
                                when (event.key) {
                                    Key.DirectionLeft -> { state.moveElement(id, el.x - step, el.y); state.moveElementDone(id); true }
                                    Key.DirectionRight -> { state.moveElement(id, el.x + step, el.y); state.moveElementDone(id); true }
                                    Key.DirectionUp -> { state.moveElement(id, el.x, el.y - step); state.moveElementDone(id); true }
                                    Key.DirectionDown -> { state.moveElement(id, el.x, el.y + step); state.moveElementDone(id); true }
                                    Key.Delete, Key.Backspace -> { state.removeElement(id); true }
                                    else -> false
                                }
                            } else false
                        },
                ) {
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                            Text("Design", modifier = Modifier.padding(12.dp))
                        }
                        Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                            Text("Print Preview", modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when (activeTab) {
                        0 -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(elements, selectedElementId) {
                                        detectTapGestures { offset ->
                                            val (lx, ly) = screenToLabel(
                                                offset.x, offset.y,
                                                size.width.toFloat(), size.height.toFloat(),
                                                selectedLabelSize.widthPx, selectedLabelSize.heightPx,
                                            )
                                            val hit = hitTest(elements, lx, ly)
                                            state.selectElement(hit?.id)
                                            focusRequester.requestFocus()
                                        }
                                    }
                                    .pointerInput(elements, selectedElementId) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                selectedElementId?.let { state.moveElementDone(it) }
                                            },
                                        ) { _, dragAmount ->
                                            val id = selectedElementId ?: return@detectDragGestures
                                            val el = elements.find { it.id == id } ?: return@detectDragGestures
                                            val (dx, dy) = screenDeltaToLabel(
                                                dragAmount.x, dragAmount.y,
                                                size.width.toFloat(), size.height.toFloat(),
                                                selectedLabelSize.widthPx, selectedLabelSize.heightPx,
                                            )
                                            val newX = if (gridSize > 0f) snapToGrid(el.x + dx, gridSize) else el.x + dx
                                            val newY = if (gridSize > 0f) snapToGrid(el.y + dy, gridSize) else el.y + dy
                                            state.moveElement(id, newX, newY)
                                        }
                                    },
                            ) {
                                LabelCanvas(
                                    elements = elements,
                                    selectedElementId = selectedElementId,
                                    widthPx = selectedLabelSize.widthPx,
                                    heightPx = selectedLabelSize.heightPx,
                                    textMeasurer = textMeasurer,
                                    showGrid = showGrid,
                                    gridSize = gridSize,
                                )
                            }
                        }
                        1 -> {
                            MonochromePreview(rows = monochromeRows, width = previewWidth)
                        }
                    }
                }

                // Right panel: PropertiesPanel only in wide mode
                if (!isCompact) {
                    VerticalDivider()

                    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                        PropertiesPanel(
                            selectedElement = selectedElement,
                            onPositionChange = { id, x, y, w, h -> state.setElementPosition(id, x, y, w, h) },
                            onRotationChange = { id, deg -> state.setElementRotation(id, deg) },
                            onTextChange = { id, text -> state.setTextContent(id, text) },
                            onTextChangeDone = { id -> state.setTextContentDone(id) },
                            onFontSizeChange = { id, size -> state.setFontSize(id, size) },
                            onFontSizeChangeDone = { id -> state.setFontSizeDone(id) },
                            onFontFamilyChange = { id, family -> state.setFontFamily(id, family) },
                            onImageScaleChange = { id, scale -> state.setImageScale(id, scale) },
                            onImageScaleChangeDone = { id -> state.setImageScaleDone(id) },
                            onFlipH = { id -> state.toggleImageFlipH(id) },
                            onFlipV = { id -> state.toggleImageFlipV(id) },
                            onRotateImage90 = { id -> state.rotateImage90(id) },
                            onBarcodeFormatChange = { id, fmt -> state.setBarcodeFormat(id, fmt) },
                            onBarcodeDataChange = { id, data -> state.setBarcodeData(id, data) },
                            onBarcodeDataChangeDone = { id -> state.setBarcodeDataDone(id) },
                            onBarcodeErrorCorrectionChange = { id, ec -> state.setBarcodeErrorCorrection(id, ec) },
                            onBarcodeDataStandardChange = { id, std -> state.setBarcodeDataStandard(id, std) },
                            onBarcodeStructuredDataChange = { id, fields -> state.setBarcodeStructuredData(id, fields) },
                            onBarcodeStructuredDataChangeDone = { id -> state.setBarcodeStructuredDataDone(id) },
                            onToggleBold = { id -> state.toggleBold(id) },
                            onToggleItalic = { id -> state.toggleItalic(id) },
                            onBringToFront = { id -> state.bringToFront(id) },
                            onSendToBack = { id -> state.sendToBack(id) },
                            onMoveUp = { id -> state.moveUp(id) },
                            onMoveDown = { id -> state.moveDown(id) },
                            elementCount = elements.size,
                            elementIndex = elements.indexOfFirst { it.id == selectedElementId },
                            onPrint = {
                                state.print(monochromeRows, previewWidth, previewHeight)
                                showPrintDialog = true
                            },
                        )
                    }
                }
            }

            // Compact mode: floating properties button when an element is selected
            if (isCompact && selectedElement != null && !showPropertiesSheet) {
                FloatingActionButton(
                    onClick = { showPropertiesSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text("Props")
                }
            }
        }
    }

    // Compact mode: ModalBottomSheet for properties
    if (showPropertiesSheet && selectedElement != null) {
        ModalBottomSheet(
            onDismissRequest = { showPropertiesSheet = false },
            sheetState = propertiesSheetState,
        ) {
            PropertiesPanel(
                selectedElement = selectedElement,
                onPositionChange = { id, x, y, w, h -> state.setElementPosition(id, x, y, w, h) },
                onRotationChange = { id, deg -> state.setElementRotation(id, deg) },
                onTextChange = { id, text -> state.setTextContent(id, text) },
                onTextChangeDone = { id -> state.setTextContentDone(id) },
                onFontSizeChange = { id, size -> state.setFontSize(id, size) },
                onFontSizeChangeDone = { id -> state.setFontSizeDone(id) },
                onFontFamilyChange = { id, family -> state.setFontFamily(id, family) },
                onImageScaleChange = { id, scale -> state.setImageScale(id, scale) },
                onImageScaleChangeDone = { id -> state.setImageScaleDone(id) },
                onFlipH = { id -> state.toggleImageFlipH(id) },
                onFlipV = { id -> state.toggleImageFlipV(id) },
                onRotateImage90 = { id -> state.rotateImage90(id) },
                onBarcodeFormatChange = { id, fmt -> state.setBarcodeFormat(id, fmt) },
                onBarcodeDataChange = { id, data -> state.setBarcodeData(id, data) },
                onBarcodeDataChangeDone = { id -> state.setBarcodeDataDone(id) },
                onBarcodeErrorCorrectionChange = { id, ec -> state.setBarcodeErrorCorrection(id, ec) },
                onBarcodeDataStandardChange = { id, std -> state.setBarcodeDataStandard(id, std) },
                onBarcodeStructuredDataChange = { id, fields -> state.setBarcodeStructuredData(id, fields) },
                onBarcodeStructuredDataChangeDone = { id -> state.setBarcodeStructuredDataDone(id) },
                onToggleBold = { id -> state.toggleBold(id) },
                onToggleItalic = { id -> state.toggleItalic(id) },
                onBringToFront = { id -> state.bringToFront(id) },
                onSendToBack = { id -> state.sendToBack(id) },
                onMoveUp = { id -> state.moveUp(id) },
                onMoveDown = { id -> state.moveDown(id) },
                elementCount = elements.size,
                elementIndex = elements.indexOfFirst { it.id == selectedElementId },
                onPrint = {
                    state.print(monochromeRows, previewWidth, previewHeight)
                    showPrintDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    FileSaveEffect(
        trigger = saveRequested,
        defaultName = "label.bpl",
        content = if (saveRequested) LabelDesign.toJson(state.toDesign()) else "",
        onDone = { saveRequested = false },
    )

    FileLoadEffect(
        trigger = loadRequested,
        onLoaded = { json ->
            try { state.loadDesign(LabelDesign.fromJson(json)) } catch (_: Exception) { }
        },
        onDone = { loadRequested = false },
    )

    if (showPrintDialog) {
        PrintDialog(progress = printProgress, onDismiss = { showPrintDialog = false })
    }

    if (showBarcodePicker) {
        BarcodeFormatPicker(
            onAdd = { format, data, standard, structuredData ->
                state.addBarcodeElement(format, data, standard, structuredData)
                showBarcodePicker = false
            },
            onDismiss = { showBarcodePicker = false },
        )
    }

    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            onSave = { name, desc ->
                state.saveAsTemplate(name, desc)
                showSaveTemplateDialog = false
            },
            onDismiss = { showSaveTemplateDialog = false },
        )
    }

    if (showTemplateDialog) {
        TemplatePickerDialog(
            templates = TemplateManager.builtInTemplates(),
            hasExistingElements = elements.isNotEmpty(),
            onSelect = { template ->
                state.applyTemplate(template)
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false },
        )
    }
}
