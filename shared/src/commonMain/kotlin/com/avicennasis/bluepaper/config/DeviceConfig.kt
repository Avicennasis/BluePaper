package com.avicennasis.bluepaper.config

data class DeviceConfig(
    val model: String,
    val dpi: Int,
    val maxDensity: Int,
    val rotation: Int,
    val isV2: Boolean,
    val maxWidthPx: Int,
    val labelSizes: List<LabelSize>,
    /** Printhead width in dots; the v4 protocol sends per-third pixel counts based on it. */
    val printheadPixels: Int = maxWidthPx,
    /** Print with the protocol-v4 sequence when the printer reports v4+ (NiimBlue's D110M_V4 task). */
    val v4WhenSupported: Boolean = false,
)
