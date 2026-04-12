# BluePaper

Cross-platform Bluetooth label printer app built with Kotlin Multiplatform and Compose Multiplatform.

## Features

- **8 Niimbot printer models** — D11, D11-H, D101, D110, D110-M, B1, B18, B21
- **Text labels** — Multi-line text with adjustable font size, word wrapping
- **Image import** — Load PNG/JPG/BMP with position, scale, rotate, flip controls
- **Live preview** — Design preview + monochrome print preview showing exact printer output
- **Device rotation** — Automatic per-model rotation (D-series: -90, B-series: 0)
- **Save/load** — Label designs saved as `.bpl` JSON files
- **Print settings** — Density (1-5), copies (1-10), label size selection

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
Label Design → Compose Canvas → ImageBitmap.readPixels() → MonochromeEncoder
→ packed 1-bit rows → PrinterClient.print() → CommandBuilder → NiimbotPackets
→ BleTransport.sendCommand() / writeRaw() → Kable → BLE GATT → Printer
```

## Building

```bash
# Run tests (118 tests)
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
shared/src/desktopMain/    — JFileChooser image/file pickers
shared/src/iosMain/        — Stubs (planned)
androidApp/                — Android entry point
desktopApp/                — Desktop entry point
```

## License

MIT
