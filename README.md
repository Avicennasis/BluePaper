# BluePaper

Cross-platform Bluetooth label printer app built with Kotlin Multiplatform and Compose Multiplatform.

**Quick start (Linux)**:
```bash
sudo apt install openjdk-17-jdk
git clone https://github.com/avicennasis/BluePaper.git && cd BluePaper
./gradlew :desktopApp:run
```

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

| Platform | UI | BLE Printing | Notes |
|----------|-----|--------------|-------|
| Android | Full | Full | Kable BLE, requires Android 8.0+ |
| macOS | Full | Full | Kable BLE (CoreBluetooth) |
| Linux | Full | Full | BlueZ D-Bus (requires BlueZ 5.x) |
| Windows | Full | Not yet | Kable lacks WinRT support |
| iOS | Full | Planned | Kable ready, needs UI integration |

**Workaround for Windows**: Design labels on desktop, then transfer the `.bpl` file to an Android device for printing.

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

## Installation (Linux)

### Prerequisites

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel

# Arch
sudo pacman -S jdk17-openjdk

# Verify
java -version  # Should show 17.x
```

### Run the Desktop App (UI only)

```bash
git clone https://github.com/avicennasis/BluePaper.git
cd BluePaper
./gradlew :desktopApp:run
```

First run downloads Gradle and dependencies (~500MB). Subsequent runs are fast.

**Linux BLE requirements:**
- BlueZ 5.x (pre-installed on most distros)
- D-Bus system bus access
- User must be in `bluetooth` group: `sudo usermod -aG bluetooth $USER` (then re-login)

### Print via Android

For Bluetooth printing, build and install the Android APK:

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# APK location
ls -la androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Install via ADB (if device connected)
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

**Android requirements**:
- Android 8.0+ (API 26)
- Bluetooth LE support
- Location permission (required for BLE scanning on Android 8-11)
- Bluetooth permissions (Android 12+)

The app requests permissions on first launch with rationale dialogs.

### Development Setup

```bash
# Run all tests (348 tests)
./gradlew :shared:desktopTest

# Run with debug output
./gradlew :desktopApp:run --info

# Clean build
./gradlew clean :desktopApp:run

# Check compilation without running
./gradlew :shared:compileKotlinDesktop
```

### Troubleshooting

| Issue | Fix |
|-------|-----|
| `JAVA_HOME not set` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` |
| Gradle download fails | Check internet, retry — Gradle wrapper handles versioning |
| App won't start | Ensure X11/Wayland display is available |
| Fonts look wrong | Install `fonts-noto` for full Unicode coverage |
| BLE scan finds nothing | Ensure BlueZ is running: `systemctl status bluetooth` |
| D-Bus permission denied | Add user to bluetooth group, re-login |

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
