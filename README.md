# BluePaper

Cross-platform Bluetooth label printer app built with Kotlin Multiplatform and Compose Multiplatform.

## Supported Platforms

- Android
- iOS
- Linux
- macOS
- Windows

## Supported Printers

| Model | DPI | Protocol |
|-------|-----|----------|
| D11 | 203 | V1 |
| D11-H | 300 | V1 |
| D101 | 203 | V1 |
| D110 | 203 | V1 |
| D110-M | 300 | V1 |
| B1 | 203 | V2 |
| B18 | 203 | V2 |
| B21 | 203 | V2 |

## Building

```bash
# Run tests
./gradlew :shared:desktopTest

# Build desktop app
./gradlew :desktopApp:run

# Build Android APK
./gradlew :androidApp:assembleDebug
```

## License

MIT
