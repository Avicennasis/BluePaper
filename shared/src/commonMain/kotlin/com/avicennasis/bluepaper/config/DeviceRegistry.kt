package com.avicennasis.bluepaper.config

object DeviceRegistry {

    private const val V1_MAX_WIDTH = 240
    private const val V2_MAX_WIDTH = 384
    private const val DEFAULT_MAX_DENSITY = 3

    private fun sizes(dpi: Int, vararg dims: Pair<Double, Double>): List<LabelSize> =
        dims.map { (w, h) -> LabelSize(w, h, dpi) }

    private val configs: Map<String, DeviceConfig> = listOf(
        DeviceConfig(
            model = "d110", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d11", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d11_h", dpi = 300, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(300, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d101", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(203, 30.0 to 14.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "d110_m", dpi = 300, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            labelSizes = sizes(300, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5),
        ),
        DeviceConfig(
            model = "b18", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 40.0 to 14.0, 50.0 to 14.0, 120.0 to 14.0),
        ),
        DeviceConfig(
            model = "b21", dpi = 203, maxDensity = 5,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 50.0 to 30.0, 40.0 to 30.0, 50.0 to 15.0, 30.0 to 15.0),
        ),
        DeviceConfig(
            model = "b1", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = 0, isV2 = true, maxWidthPx = V2_MAX_WIDTH,
            labelSizes = sizes(203, 50.0 to 30.0, 50.0 to 15.0, 60.0 to 40.0, 40.0 to 30.0),
        ),
    ).associateBy { it.model }

    fun get(model: String): DeviceConfig? = configs[model.lowercase()]

    fun all(): List<DeviceConfig> = configs.values.toList()

    fun models(): List<String> = configs.keys.toList()

    /** Returns BLE advertised-name prefixes derived from registered models, for scan filtering.
     *  Underscores are stripped because real devices advertise without them (e.g. "D11H" not "D11_H"). */
    fun scanPrefixes(): List<String> =
        configs.keys.map { it.uppercase().replace("_", "") }.distinct()
}
