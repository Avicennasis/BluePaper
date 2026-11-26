package com.avicennasis.bluepaper.config

/** The wire protocol a supported printer speaks. */
enum class PrinterProtocol {
    /** Niimbot's packet protocol (0x55 0x55 framing, replies matched by type). */
    NIIMBOT,

    /** Epson ESC/POS "GS v 0" raster image, as spoken by the Phomemo D30. */
    ESC_POS_RASTER,
}
