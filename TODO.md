# BluePaper — TODO

## Hardware-Dependent (needs Niimbot printer)
- [ ] Real BLE testing with physical printer
- [ ] D110 dual-advertisement filtering validation
- [ ] End-to-end print verification
- [ ] Tune polling delays (endPagePrint retries, getPrintStatus interval)

## Android
- [ ] BLE runtime permissions flow (BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION)
- [ ] Image picker via ActivityResult launcher (currently stubbed)
- [ ] File save/load via Storage Access Framework (currently stubbed)

## iOS
- [ ] Image picker via PHPicker (currently stubbed)
- [ ] File save/load via document picker (currently stubbed)
- [ ] CoreBluetooth BLE transport (Kable handles this, needs testing)

## Desktop
- [ ] Linux BLE via BlueZ D-Bus (Kable doesn't support Linux JVM BLE)
- [ ] Windows BLE via WinRT (Kable doesn't support Windows JVM BLE)

## Features
- [ ] Font family picker (currently uses system default)
- [ ] Label templates (pre-built designs)
- [ ] QR code / barcode generation on labels
- [ ] Drag-and-drop image positioning (currently slider-based)
- [ ] Undo/redo
- [ ] Dark theme toggle

## Code Quality
- [ ] Increase test coverage (currently 118 tests, UI untested)
- [ ] Pipeline integration tests (text → encode → packets end-to-end)
- [ ] Width clamping validation (240px V1, 384px V2)
- [ ] bleak 0.22→3.0+ migration in NiimPrintX (reference codebase)
