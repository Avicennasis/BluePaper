package com.avicennasis.bluepaper.protocol

/**
 * Epson ESC/POS "GS v 0" raster framing for the Phomemo D30.
 *
 * The D30 has a 12 mm / 203 dpi head (96 dots wide = 12 bytes per row) and takes 1-bpp raster
 * rows, MSB first, 1 = dark. Confirmed against a real unit by the Vibe-D30 / odensc projects.
 *
 * Wire layout (written to GATT characteristic ff02, write-with-response):
 * ```
 *   1B 40                ESC @  - initialize
 *   1D 76 30 00          GS v 0, m=0 - raster bit image
 *   wLo wHi hLo hHi      width in bytes, height in rows
 *   <rows>               1-bpp raster, width bytes per row
 *   1B 64 00             ESC d 0 - print and feed
 * ```
 */
object PhomemoD30Protocol {
    /** Bytes per BLE write. Transport-friendliness, not a protocol constraint. */
    const val CHUNK_SIZE = 128

    val FOOTER: ByteArray = byteArrayOf(0x1B, 0x64, 0x00)

    fun header(widthBytes: Int, heightRows: Int): ByteArray = byteArrayOf(
        0x1B, 0x40,                                             // ESC @
        0x1D, 0x76, 0x30, 0x00,                                 // GS v 0 m=0
        (widthBytes and 0xFF).toByte(), ((widthBytes shr 8) and 0xFF).toByte(),
        (heightRows and 0xFF).toByte(), ((heightRows shr 8) and 0xFF).toByte(),
    )

    /** Concatenated 1-bpp raster payload (no header/footer). */
    fun payload(imageRows: List<ByteArray>): ByteArray {
        require(imageRows.isNotEmpty()) { "no raster rows" }
        val widthBytes = imageRows.first().size
        require(imageRows.all { it.size == widthBytes }) { "ragged raster rows" }
        val out = ByteArray(widthBytes * imageRows.size)
        imageRows.forEachIndexed { i, row -> row.copyInto(out, i * widthBytes) }
        return out
    }

    /** Full single-copy job: header + payload + footer. */
    fun frame(imageRows: List<ByteArray>): ByteArray {
        val header = header(imageRows.first().size, imageRows.size)
        val body = payload(imageRows)
        return header + body + FOOTER
    }

    /** Split a byte stream into BLE writes of at most [chunkSize] bytes. */
    fun chunks(data: ByteArray, chunkSize: Int = CHUNK_SIZE): List<ByteArray> {
        if (data.isEmpty()) return emptyList()
        val out = ArrayList<ByteArray>((data.size + chunkSize - 1) / chunkSize)
        var i = 0
        while (i < data.size) {
            val end = minOf(i + chunkSize, data.size)
            out.add(data.copyOfRange(i, end))
            i = end
        }
        return out
    }
}
