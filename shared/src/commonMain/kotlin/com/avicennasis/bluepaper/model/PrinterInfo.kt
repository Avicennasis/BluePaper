package com.avicennasis.bluepaper.model

data class PrinterInfo(
    val deviceSerial: String? = null,
    val softwareVersion: Double? = null,
    val hardwareVersion: Double? = null,
    val deviceType: Int? = null,
    val battery: Int? = null,
    val density: Int? = null,
    val printSpeed: Int? = null,
    val labelType: Int? = null,
)
