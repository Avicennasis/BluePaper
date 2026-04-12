# BluePaper — TODO

## v0.3.0 — UI Overhaul (Current Focus)
- [ ] LabelElement sealed class (Text, Image) — replaces flat state fields
- [ ] Three-panel layout: left tools/elements, center canvas, right properties
- [ ] Top toolbar: Save, Load, Undo, Redo, Theme toggle, Disconnect
- [ ] Click-to-select canvas hit-testing
- [ ] Drag-to-move elements on canvas
- [ ] Resize handles (8-point, aspect ratio lock with shift)
- [ ] Keyboard nudge (arrow 1px, shift+arrow 10px, delete to remove)
- [ ] Snap-to-grid (8px default, alt to disable, optional grid overlay)
- [ ] Dark/Light/System theme toggle with persisted preference
- [ ] Expanded Material3 color scheme (surface variants, panel differentiation)
- [ ] Font family picker — 9 bundled .ttf fonts with preview-in-picker
- [ ] FontRegistry for font key → FontFamily mapping
- [ ] Label templates — 6 starter templates (Simple Text, Two-Line, Image+Caption, Centered Image, Price Tag, Inventory)
- [ ] Templates as bundled JSON resource with proportional coordinates
- [ ] Undo/redo snapshot stack (max 50, coalesced mutations)
- [ ] Ctrl+Z / Ctrl+Shift+Z keyboard shortcuts
- [ ] .bpl v2 format with elements list (auto-migrate v1 on load)
- [ ] Numeric property inputs alongside sliders in right panel
- [ ] Narrow window fallback (< 800dp: left panel collapses, right panel becomes bottom sheet)

## Features (Backlog)
- [ ] QR code / barcode generation on labels
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
