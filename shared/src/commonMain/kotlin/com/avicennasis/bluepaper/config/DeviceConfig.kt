package com.avicennasis.bluepaper.config

data class DeviceConfig(
    val model: String,
    val dpi: Int,
    val maxDensity: Int,
    val rotation: Int,
    val isV2: Boolean,
    val maxWidthPx: Int,
    val labelSizes: List<LabelSize>,
)
