package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.config.PrinterProtocol
import com.avicennasis.bluepaper.protocol.PhomemoD30Protocol
import kotlinx.coroutines.delay

/**
 * Prints ESC/POS raster jobs (Phomemo D30) over an already-connected [BleTransport].
 *
 * [imageRows] are 1-bpp rows as produced by the renderer (width = the tape's 12 mm = 12 bytes).
 * The transport must have been connected with the D30's [com.avicennasis.bluepaper.config.GattProfile].
 */
class PhomemoD30Client(private val transport: BleTransport) {

    suspend fun print(
        config: DeviceConfig,
        imageRows: List<ByteArray>,
        quantity: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
    ) {
        require(config.protocol == PrinterProtocol.ESC_POS_RASTER) {
            "PhomemoD30Client requires an ESC_POS_RASTER device, got ${config.protocol}"
        }
        if (imageRows.isEmpty()) throw PrinterException("Nothing to print")
        val withResponse = config.gatt?.writeWithResponse ?: true
        val header = PhomemoD30Protocol.header(imageRows.first().size, imageRows.size)
        val chunks = PhomemoD30Protocol.chunks(PhomemoD30Protocol.payload(imageRows))
        println("[PhomemoD30Client] ${imageRows.size} rows, ${chunks.size} data chunks x$quantity")

        for (copy in 1..quantity) {
            transport.writeBytes(header, withResponse = withResponse)
            for (chunk in chunks) transport.writeBytes(chunk, withResponse = withResponse)
            transport.writeBytes(PhomemoD30Protocol.FOOTER, withResponse = withResponse)
            onProgress?.invoke(copy, quantity)
            // Let the mechanical feed finish before the next copy.
            if (copy < quantity) delay(1_500)
        }
    }
}
