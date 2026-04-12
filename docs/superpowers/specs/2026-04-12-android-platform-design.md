# BluePaper v0.5.0 — Android Platform Completion Design Spec

**Date:** 2026-04-12
**Version:** 0.4.0 → 0.5.0
**Scope:** Android BLE permissions, image picker, file save/load, theme persistence

## Overview

Fill in all Android platform stubs to make BluePaper a fully functional Android app. BLE permissions with rationale dialog, SAF-based image import, SAF-based .bpl file save/load, and SharedPreferences theme persistence.

## 1. BLE Runtime Permissions

### Permission Flow

When user taps "Start Scan" on ScannerScreen:
1. Check if required permissions are granted
2. If granted → proceed with scan
3. If not granted → request via `RequestMultiplePermissions` launcher
4. If denied once → show rationale `AlertDialog` explaining why BLE needs permissions
5. If permanently denied → show dialog with "Open Settings" button

### SDK-Dependent Permission Set

```kotlin
val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}
```

Android 12+ (SDK 31): `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`
Android 8-11 (SDK 26-30): `ACCESS_FINE_LOCATION` (required for BLE scanning)

### BlePermissionHandler Composable

New file in androidMain:

```kotlin
@Composable
fun BlePermissionHandler(
    onPermissionsGranted: @Composable () -> Unit,
    onPermissionsDenied: @Composable () -> Unit,
)
```

Wraps the entire app content in `MainActivity`. If permissions are granted, renders the app. If not, renders a permission request screen.

**Rationale dialog:** Standard Material3 AlertDialog explaining "BluePaper needs Bluetooth access to discover and communicate with your Niimbot printer."

**Settings link:** `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` with app package URI.

### Integration Point

`MainActivity` wraps `BluePaperApp` with `BlePermissionHandler`. The permission state is checked before the scanner can operate. `ScannerState.startScan()` doesn't need to change — it just won't be called until permissions are granted.

## 2. Image Picker (SAF)

### Implementation

Replace the disabled stub `ImagePickerButton` with a real SAF-based implementation.

```kotlin
@Composable
actual fun ImagePickerButton(onImageLoaded: (ImageBitmap) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                bitmap?.asImageBitmap()?.let(onImageLoaded)
            }
        }
    }
    OutlinedButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
        Text("+ Image")
    }
}
```

**MIME filter:** `image/*` — covers PNG, JPEG, BMP, WebP.
**Error handling:** `BitmapFactory.decodeStream` returns null for unsupported formats — silently ignored (no crash).
**Memory:** For label-sized images (240-384px wide), memory is negligible. No downsampling needed.

## 3. File Save/Load (SAF)

### Problem: Synchronous vs Async

Current expect declarations are synchronous:
```kotlin
expect fun pickSaveFile(defaultName: String): String?
expect fun pickOpenFile(): String?
expect fun writeTextFile(path: String, content: String)
```

Desktop (JFileChooser) is synchronous — these work. Android (SAF) is async — launches an Activity, returns via callback. Need a new pattern.

### Solution: Composable Effect-Based File Operations

New expect/actual composables that handle the async nature:

```kotlin
// commonMain
@Composable
expect fun FileSaveEffect(
    trigger: Boolean,
    defaultName: String,
    content: String,
    onDone: () -> Unit,
)

@Composable
expect fun FileLoadEffect(
    trigger: Boolean,
    onLoaded: (String) -> Unit,
    onDone: () -> Unit,
)
```

**How it works:** EditorScreen sets `trigger = true` when the user clicks Save/Load. The platform-specific composable launches the appropriate picker. When complete, calls `onDone()` to reset the trigger.

**Desktop actual:** Uses `LaunchedEffect(trigger)` + `JFileChooser` (synchronous in the effect).

**Android actual:** Uses `rememberLauncherForActivityResult` + SAF contracts:
- Save: `ActivityResultContracts.CreateDocument("application/json")` → writes content to the returned URI
- Load: `ActivityResultContracts.OpenDocument()` with `arrayOf("application/json", "*/*")` → reads content from URI

**iOS actual:** No-op stubs (returns without action).

### EditorScreen Changes

Replace direct `pickSaveFile`/`pickOpenFile` calls with state-driven triggers:

```kotlin
var saveRequested by remember { mutableStateOf(false) }
var loadRequested by remember { mutableStateOf(false) }

FileSaveEffect(
    trigger = saveRequested,
    defaultName = "label.bpl",
    content = LabelDesign.toJson(state.toDesign()),
    onDone = { saveRequested = false },
)

FileLoadEffect(
    trigger = loadRequested,
    onLoaded = { json ->
        try { state.loadDesign(LabelDesign.fromJson(json)) } catch (_: Exception) { }
    },
    onDone = { loadRequested = false },
)
```

Toolbar Save/Load buttons set `saveRequested = true` / `loadRequested = true`.

### Backward Compatibility

The old `pickSaveFile`/`pickOpenFile`/`writeTextFile` functions remain in their files but are no longer called from EditorScreen. They can be removed in a future cleanup.

## 4. ThemePreferences (SharedPreferences)

### Implementation

```kotlin
actual object ThemePreferences {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("bluepaper_prefs", Context.MODE_PRIVATE)
    }

    actual fun save(mode: ThemeMode) {
        prefs?.edit()?.putString("themeMode", mode.name)?.apply()
    }

    actual fun load(): ThemeMode {
        val name = prefs?.getString("themeMode", null) ?: return ThemeMode.System
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.System)
    }
}
```

### Integration

`MainActivity.onCreate()` calls `ThemePreferences.init(this)` before `setContent`.

## 5. File Changes Summary

### New Files
- `androidMain/.../BlePermissionHandler.kt` — Permission composable with rationale dialog
- `commonMain/.../FileSaveLoad.kt` — expect `FileSaveEffect` + `FileLoadEffect`
- `desktopMain/.../FileSaveLoad.desktop.kt` — JFileChooser-based actuals
- `androidMain/.../FileSaveLoad.android.kt` — SAF-based actuals
- `iosMain/.../FileSaveLoad.ios.kt` — stub actuals

### Modified Files
- `androidMain/.../ImagePicker.android.kt` — Real SAF image picker
- `androidMain/.../ThemePreferences.android.kt` — SharedPreferences
- `androidApp/.../MainActivity.kt` — Permission wrapper + ThemePreferences.init
- `commonMain/.../EditorScreen.kt` — FileSaveEffect/FileLoadEffect instead of pickSaveFile/pickOpenFile
- `androidApp/build.gradle.kts` — Bump versionName to 0.5.0

### Test Strategy
- BLE permissions: manual testing on device (can't unit test runtime permissions)
- Image picker: manual testing on device
- File save/load: manual testing on device (SAF requires real Activity context)
- ThemePreferences: can unit test the SharedPreferences logic with Robolectric, but manual testing is sufficient for a single preference

## 6. Out of Scope (Next Versions)
- Image serialization in .bpl (v0.6.0)
- Custom templates (v0.6.0)
- Z-ordering (v0.6.0)
- Bold/italic fonts (v0.6.0)
- Width clamping (v0.6.0)
- Responsive layout (v0.6.0)
- Code quality sweep (v0.7.0)
