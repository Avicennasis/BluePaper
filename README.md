# BluePaper

Cross-platform Bluetooth label printer app built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- **8 Niimbot printer models** — D11, D11-H, D101, D110, D110-M, B1, B18, B21
- **Three-panel editor** — Pro-tool layout: toolbox, interactive canvas, properties panel
- **Element-based design** — Text, image, and barcode elements with click-to-select, drag-to-move, resize handles
- **16 barcode formats** — QR Code, PDF417, Data Matrix, Aztec, MaxiCode, Code 128/39/93, EAN-13/8, UPC-A/E, Codabar, ITF, RSS
- **13 data standards** — vCard, URL, WiFi, MeCard, SMS, Email, Phone, Geo, AAMVA, GS1-128, GS1 DataMatrix, HIBC
- **Structured barcode data** — Categorized format picker, structured field entry, auto-encoding per standard
- **QR error correction** — L/M/Q/H levels with auto-fix validation for EAN/UPC check digits
- **9 bundled fonts** — Sans-serif, serif, monospace, and display families with preview-in-picker
- **6 label templates** — Simple Text, Two-Line, Image+Caption, Centered Image, Price Tag, Inventory
- **Undo/redo** — Snapshot-based with Ctrl+Z / Ctrl+Shift+Z keyboard shortcuts
- **Dark/Light/System theme** — Material3 with expanded color schemes, persisted preference
- **Image import** — Load PNG/JPG/BMP with position, scale, rotate, flip controls
- **Live preview** — Tabbed Design preview + monochrome Print Preview showing exact printer output
- **Device rotation** — Automatic per-model rotation (D-series: -90, B-series: 0)
- **Save/load** — Label designs saved as `.bpl` JSON files (v2 format with v1 migration)
- **Print settings** — Density (1-5), copies (1-10), label size selection
- **Snap-to-grid** — 8px grid with optional overlay, Alt to temporarily disable
- **Keyboard shortcuts** — Arrow keys nudge 1px (Shift+Arrow = 10px), Delete removes element

## Supported Platforms

- Android (BLE via Kable)
- iOS (planned)
- Linux (desktop UI, BLE pending)
- macOS (desktop UI, BLE via Kable)
- Windows (desktop UI, BLE pending)

## Supported Printers

| Model | DPI | Protocol | Max Width |
|-------|-----|----------|-----------|
| D11 | 203 | V1 | 240px |
| D11-H | 300 | V1 | 240px |
| D101 | 203 | V1 | 240px |
| D110 | 203 | V1 | 240px |
| D110-M | 300 | V1 | 240px |
| B1 | 203 | V2 | 384px |
| B18 | 203 | V2 | 384px |
| B21 | 203 | V2 | 384px |

## Architecture

```
Label Elements → Compose Canvas → ImageBitmap.readPixels() → MonochromeEncoder
→ packed 1-bit rows → PrinterClient.print() → CommandBuilder → NiimbotPackets
→ BleTransport.sendCommand() / writeRaw() → Kable → BLE GATT → Printer
```

## Building

```bash
# Run tests
./gradlew :shared:desktopTest

# Run desktop app
./gradlew :desktopApp:run

# Build Android APK
./gradlew :androidApp:assembleDebug

# Compile Android (no device needed)
./gradlew :shared:compileDebugKotlinAndroid
```

## Project Structure

```
shared/src/commonMain/     — Protocol, BLE interfaces, image processing, UI (all platforms)
shared/src/androidMain/    — Kable BLE scanner + transport
shared/src/desktopMain/    — JFileChooser image/file pickers, theme persistence
shared/src/iosMain/        — Stubs (planned)
androidApp/                — Android entry point
desktopApp/                — Desktop entry point
```

## License

MIT
