package com.avicennasis.bluepaper.ui.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avicennasis.bluepaper.ble.ConnectionState

@Composable
fun ScannerScreen(
    state: ScannerState,
    onConnected: () -> Unit,
) {
    val devices by state.devices.collectAsState()
    val isScanning by state.isScanning.collectAsState()
    val connectionState by state.connectionState.collectAsState()
    val error by state.error.collectAsState()

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text(
            text = "BluePaper",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Find your Niimbot printer",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { if (isScanning) state.stopScan() else state.startScan() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Scanning...")
            } else {
                Text("Scan for Printers")
            }
        }

        error?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(msg, color = MaterialTheme.colorScheme.error)
        }

        if (connectionState == ConnectionState.CONNECTING) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Connecting...", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No printers found. Tap Scan to search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices, key = { it.address }) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { state.connectTo(device) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                Text(device.address, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
