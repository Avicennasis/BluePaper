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
- [ ] Narrow window fallback (< 800dp: left panel collapses, right panel becomes bottom sheet)

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

## Features (Backlog)
- [ ] Custom user-saved templates (save design as template)
- [ ] Image serialization in .bpl files (currently lost on save/load)
- [ ] Width clamping validation (240px V1, 384px V2)
- [ ] Pipeline integration tests (text → encode → packets end-to-end)
- [ ] Bold/italic font weight variants
- [ ] Multi-element z-ordering (bring to front / send to back)

## Android (Backlog)
- [ ] BLE runtime permissions flow (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION)
- [ ] Image picker via ActivityResult launcher (currently stubbed)
- [ ] File save/load via Storage Access Framework (currently stubbed)

## iOS (Backlog)
- [ ] Image picker via PHPicker (currently stubbed)
- [ ] File save/load via document picker (currently stubbed)
- [ ] CoreBluetooth BLE transport (Kable handles this, needs testing)

## Desktop BLE (Backlog)
- [ ] Linux BLE via BlueZ D-Bus (Kable doesn't support Linux JVM BLE)
- [ ] Windows BLE via WinRT (Kable doesn't support Windows JVM BLE)

## Code Quality (Backlog)
- [ ] Extract magic numbers to constants (retry counts, thresholds, timeouts)
- [ ] Add debug logging to PrinterClient and KableBleTransport
- [ ] Move scan prefixes from ScannerState to DeviceRegistry
- [ ] UI snapshot/interaction tests
- [ ] Extract PrinterClient polling into named methods (waitForPagePrintEnd, waitForPrintComplete)
- [ ] MonochromeEncoder: parameterize rotation instead of 3 separate code paths

## Hardware-Dependent (needs Niimbot printer)
- [ ] Real BLE testing with physical printer
- [ ] D110 dual-advertisement filtering validation
- [ ] End-to-end print verification
- [ ] Tune polling delays (endPagePrint retries, getPrintStatus interval)
