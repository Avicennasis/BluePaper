# BluePaper — TODO

## v0.3.0 — UI Overhaul (Complete)
- [x] LabelElement sealed class (Text, Image) — replaces flat state fields
- [x] Three-panel layout: left tools/elements, center canvas, right properties
- [x] Top toolbar: Save, Load, Undo, Redo, Theme toggle, Disconnect
- [x] Click-to-select canvas hit-testing
- [x] Drag-to-move elements on canvas
- [x] Resize handles (8-point, aspect ratio lock with shift)
- [x] Keyboard nudge (arrow 1px, shift+arrow 10px, delete to remove)
- [x] Snap-to-grid (8px default, alt to disable, optional grid overlay)
- [x] Dark/Light/System theme toggle with persisted preference
- [x] Expanded Material3 color scheme (surface variants, panel differentiation)
- [x] Font family picker — 9 bundled .ttf fonts with preview-in-picker
- [x] FontRegistry for font key → FontFamily mapping
- [x] Label templates — 6 starter templates (Simple Text, Two-Line, Image+Caption, Centered Image, Price Tag, Inventory)
- [x] Templates as bundled JSON resource with proportional coordinates
- [x] Undo/redo snapshot stack (max 50, coalesced mutations)
- [x] Ctrl+Z / Ctrl+Shift+Z keyboard shortcuts
- [x] .bpl v2 format with elements list (auto-migrate v1 on load)
- [x] Numeric property inputs alongside sliders in right panel
- [x] Narrow window fallback (< 800dp: CompactToolbox + ModalBottomSheet properties)

## v0.4.0 — QR/Barcode Generation (Complete)
- [x] BarcodeElement type — third LabelElement variant
- [x] 16 barcode formats (QR, PDF417, DataMatrix, Aztec, MaxiCode, Code 128/39/93, EAN-13/8, UPC-A/E, Codabar, ITF, RSS-14, RSS Expanded)
- [x] 13 data standards (Raw Text, vCard, URL, WiFi, MeCard, SMS, Email, Phone, Geo, AAMVA, GS1-128, GS1 DataMatrix, HIBC)
- [x] 12 data encoders with structured field entry and auto-encoding
- [x] BarcodeValidator with check digits (EAN/UPC mod-10) and auto-fix
- [x] BarcodeRenderer with ZXing (desktop+Android), LRU cache
- [x] Categorized format picker dialog (2D Codes / Linear / Retail)
- [x] Data standard picker with structured field forms
- [x] QR error correction (L/M/Q/H) exposed in properties panel
- [x] Live validation status in properties panel
- [x] Canvas rendering with placeholder for invalid data

## v0.5.0 — Android Platform (Complete)
- [x] BLE runtime permissions with rationale dialog (SDK-aware: BLUETOOTH_SCAN+CONNECT on 12+, FINE_LOCATION on 8-11)
- [x] Image picker via SAF OpenDocument (replaces disabled stub)
- [x] File save/load via SAF CreateDocument/OpenDocument (composable effect pattern)
- [x] ThemePreferences via SharedPreferences (persists theme choice)
- [x] BlePermissionHandler wraps app in MainActivity

## v0.6.0 — Feature Sweep (Complete)
- [x] Image serialization in .bpl (base64 PNG, downscaled to label size)
- [x] Custom user-saved templates (save/load/delete with TemplateStorage)
- [x] Z-ordering (bring to front, send to back, move up, move down)
- [x] Bold/italic (synthetic + FontRegistry addon hook for real variants)
- [x] Width clamping validation (elements stay within label bounds)
- [x] Responsive layout: CompactToolbox for narrow windows
- [x] SaveTemplateDialog + TemplatePickerDialog with saved section

## v0.7.0 — Code Quality Sweep (Complete)
- [x] Extract magic numbers to constants (PrinterClient retry counts, thresholds, timeouts)
- [x] Add debug logging to PrinterClient and KableBleTransport
- [x] Move scan prefixes from ScannerState to DeviceRegistry.models()
- [x] Extract PrinterClient polling into named methods (waitForPagePrintEnd, waitForPrintComplete)
- [x] PrinterClient: throw on timeout instead of silent fallthrough
- [x] PrinterClient: fix catch block to call endPagePrint() before endPrint()
- [x] MonochromeEncoder: parameterize rotation (3 paths → 1 parameterized function)
- [x] BLE scan: fix serial Flow.collect blocking (merge all prefix flows)
- [x] KableBleTransport: fix connect(ScannedDevice) — cache advertisements, skip standard GATT services
- [x] RFIDResponse: add bounds checking on all field accesses
- [x] BarcodeValidator: fix UPC-E check digit (expand to UPC-A first)
- [x] BarcodeValidator: add length validation for all fixed-length formats
- [x] BarcodeValidator: add RSS-14 autoFix, fix ITF empty string guard
- [x] WifiEncoder: escape special chars (`;`, `\`, `,`, `"`) for roundtrip safety
- [x] EmailEncoder: percent-encode subject/body in mailto URIs
- [x] MeCardEncoder: fix decode strip that corrupted NOTE values
- [x] HibcEncoder: validate and normalize LIC to exactly 4 chars
- [x] VCardEncoder: handle RFC 6350 line folding in decode
- [x] GS1-128 Encoder: widen AI regex to 2-4 digit Application Identifiers
- [x] AAMVA Encoder: document fabricated header limitation
- [x] EditorState: fix undo to capture pre-change state (snapshot at drag start, not end)
- [x] EditorState: thread-safety comment on nextId (main-thread confined)
- [x] EditorState: saveAsTemplate preserves barcode/image/style fields
- [x] EditorState: div-by-zero guard on label dimensions
- [x] TemplateElement: add barcode, image, and style fields
- [x] TemplateManager: handle "barcode" type in scaleToLabel (was falling to "Unknown")
- [x] LabelDesign: fix migrateToV2 false-positive guard (remove elements.isNotEmpty check)
- [x] TemplateStorage: fix slugify name collision (numeric suffix disambiguation)
- [x] TemplateStorage (Android): lateinit instead of silent null no-op
- [x] TemplatePickerDialog: fix stale remember after delete (mutableStateOf)
- [x] BarcodeRenderer: thread-safe cache (@Synchronized on LinkedHashMap)
- [x] FontRegistry: @Synchronized init (prevent double-init race)
- [x] DeviceRegistry: const val for primitive constants, add scanPrefixes()
- [x] ScannerScreen: wrap onConnected in LaunchedEffect (fix composition side-effect)
- [x] FileSaveLoad (desktop): withContext(Dispatchers.IO) for JFileChooser
- [x] FileSaveLoad (desktop): log errors instead of silent catch
- [x] PropertiesPanel: fix NumericField decimal input (focus-aware sync)
- [x] Remove dead FilePicker expect/actual declarations
- [x] Wire CompactToolbox for narrow windows (< 800dp)
- [x] Add ModalBottomSheet for properties panel on narrow windows
- [x] CanvasInteraction: document hitTest rotation limitation + hitTestHandle status
- [x] Pipeline integration tests (pixels → MonochromeEncoder → CommandBuilder → packets)
- [x] BarcodeValidator tests: length, check digit, UPC-E autoFix, RSS-14, ITF empty
- [x] Encoder roundtrip tests: Phone, URL, WiFi escaping, Email encoding, MeCard
- [x] Protocol tests: non-coincidental checksum, HeartbeatResponse null assertions
- [x] Serialization tests: corrupt/unknown barcode format deserialization
- [x] EditorState tests: undo/redo behavior, template save
- [x] Build: version catalog for androidx deps, jvmToolchain(17) for desktopApp
- [x] 314 tests passing (up from 272)

## Features (Backlog)
- [ ] Resize handles interactive (hitTestHandle is defined, needs pointer input wiring)

## iOS (Backlog)
- [ ] Image picker via PHPicker (currently stubbed)
- [ ] File save/load via document picker (currently stubbed)
- [ ] CoreBluetooth BLE transport (Kable handles this, needs testing)

## Desktop BLE (Backlog)
- [ ] Linux BLE via BlueZ D-Bus (Kable doesn't support Linux JVM BLE)
- [ ] Windows BLE via WinRT (Kable doesn't support Windows JVM BLE)

## Hardware-Dependent (needs Niimbot printer)
- [ ] Real BLE testing with physical printer
- [ ] D110 dual-advertisement filtering validation
- [ ] End-to-end print verification
- [ ] Tune polling delays (endPagePrint retries, getPrintStatus interval)
