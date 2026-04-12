package com.avicennasis.bluepaper.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrintSettings(
    density: Int,
    maxDensity: Int,
    quantity: Int,
    onDensityChange: (Int) -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Density: $density", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = density.toFloat(),
                onValueChange = { onDensityChange(it.toInt()) },
                valueRange = 1f..maxDensity.toFloat(),
                steps = maxDensity - 2,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Copies: $quantity", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = quantity.toFloat(),
                onValueChange = { onQuantityChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
            )
        }
    }
}
