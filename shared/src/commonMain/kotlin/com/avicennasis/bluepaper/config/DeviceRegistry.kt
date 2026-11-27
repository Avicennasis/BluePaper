package com.avicennasis.bluepaper.config

object DeviceRegistry {

    private const val V1_MAX_WIDTH = 240
    private const val V2_MAX_WIDTH = 384
    private const val DEFAULT_MAX_DENSITY = 3

    private fun sizes(dpi: Int, vararg dims: Pair<Double, Double>, maxHeightPx: Int = Int.MAX_VALUE): List<LabelSize> =
        dims.map { (w, h) -> LabelSize(w, h, dpi, maxHeightPx) }

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
        // 203 dpi / 96-dot printhead / protocol v4: NiimBlue's model database (id 2320) and
        // its D110M v4 test, which read those values from the printer itself.
        DeviceConfig(
            model = "d110_m", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = V1_MAX_WIDTH,
            // Rows wider than the 96-dot head printed blank; NiimBlue clamps to the head too.
            labelSizes = sizes(203, 30.0 to 15.0, 40.0 to 12.0, 50.0 to 14.0, 75.0 to 12.0, 109.0 to 12.5, maxHeightPx = 96),
            printheadPixels = 96, v4WhenSupported = true,
        ),
        // Phomemo D30: 12 mm tape, 203 dpi, 96 dots across the head. Speaks Epson ESC/POS
        // "GS v 0" raster over BLE GATT ff00/ff02 (write-with-response); no RFID/consumable auth.
        DeviceConfig(
            model = "d30", dpi = 203, maxDensity = DEFAULT_MAX_DENSITY,
            rotation = -90, isV2 = false, maxWidthPx = 96,
            // Tape widths 6/12/14/15 mm; the 12 mm head prints at most 96 dots, so 14/15 mm cap at 96.
            labelSizes = sizes(
                203,
                30.0 to 6.0, 30.0 to 12.0, 30.0 to 14.0, 30.0 to 15.0,
                40.0 to 12.0, 50.0 to 12.0,
                maxHeightPx = 96,
            ),
            printheadPixels = 96,
            protocol = PrinterProtocol.ESC_POS_RASTER,
            gatt = GattProfile(
                serviceUuid = "0000ff00-0000-1000-8000-00805f9b34fb",
                writeCharUuid = "0000ff02-0000-1000-8000-00805f9b34fb",
                notifyCharUuid = "0000ff03-0000-1000-8000-00805f9b34fb",
                writeWithResponse = true,
            ),
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

    /** True if a Bluetooth device name belongs to a supported printer. Real devices are
     *  inconsistent about underscores ("D11H-…" vs. a paired D110_M's "D110_M-HC23072563"),
     *  so compare with underscores stripped from both sides. */
    fun isSupportedPrinterName(name: String): Boolean {
        val normalized = name.uppercase().replace("_", "")
        return scanPrefixes().any { normalized.startsWith(it) }
    }

    /** Registered model for a printer's Bluetooth name, preferring the most specific match
     *  ("D110_M-HC23072563" -> "d110_m", not "d110" or "d11"). */
    fun modelForName(name: String): String? {
        val normalized = name.uppercase().replace("_", "")
        return configs.keys
            .filter { normalized.startsWith(it.uppercase().replace("_", "")) }
            .maxByOrNull { it.replace("_", "").length }
    }

    /** Printer model ids (GetInfo key 8), from niimbluelib's printer database. */
    private val modelIds: Map<Int, String> = mapOf(
        2320 to "d110_m", 2304 to "d110", 2305 to "d110", 512 to "d11", 528 to "d11_h",
        2560 to "d101", 4096 to "b1", 3584 to "b18", 768 to "b21",
    )

    fun modelForModelId(id: Int): String? = modelIds[id]

    /** Model id from a GetInfo(DEVICE_TYPE) reply: 2 bytes big-endian, or 1 byte meaning id >> 8. */
    fun modelIdFromReply(data: ByteArray): Int? = when (data.size) {
        0 -> null
        1 -> (data[0].toInt() and 0xFF) shl 8
        else -> ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
    }
}
