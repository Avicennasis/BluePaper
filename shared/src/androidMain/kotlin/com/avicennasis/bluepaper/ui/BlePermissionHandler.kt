package com.avicennasis.bluepaper.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun BlePermissionHandler(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Re-check permissions when the user returns from Settings (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsGranted = requiredPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showRationale by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (!permissionsGranted) {
            // Check if we should show rationale (user denied but didn't check "don't ask again")
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity != null && requiredPermissions.any {
                activity.shouldShowRequestPermissionRationale(it)
            }
            if (shouldShowRationale) {
                showRationale = true
            } else {
                permanentlyDenied = true
            }
        }
    }

    if (permissionsGranted) {
        content()
    } else {
        // Permission request screen
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Bluetooth Permission Required",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "BluePaper needs Bluetooth access to discover and communicate with your Niimbot label printer.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(24.dp))

                if (permanentlyDenied) {
                    Text(
                        "Bluetooth permission was permanently denied. Please enable it in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                } else {
                    Button(onClick = {
                        launcher.launch(requiredPermissions)
                    }) {
                        Text("Grant Bluetooth Permission")
                    }
                }
            }
        }

        // Rationale dialog
        if (showRationale) {
            AlertDialog(
                onDismissRequest = { showRationale = false },
                title = { Text("Bluetooth Needed") },
                text = {
                    Text("BluePaper uses Bluetooth Low Energy (BLE) to find and connect to your Niimbot printer. Without this permission, the app cannot scan for or print to your device.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRationale = false
                        launcher.launch(requiredPermissions)
                    }) { Text("Grant Permission") }
                },
                dismissButton = {
                    TextButton(onClick = { showRationale = false }) { Text("Not Now") }
                },
            )
        }
    }
}
