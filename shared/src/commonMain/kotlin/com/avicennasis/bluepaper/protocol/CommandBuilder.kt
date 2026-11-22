package com.avicennasis.bluepaper.protocol

object CommandBuilder {

    fun getInfo(key: InfoEnum): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(key.key.toByte()))

    fun getRfid(): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_RFID.code, byteArrayOf(0x01))

    fun heartbeat(): NiimbotPacket =
        NiimbotPacket(RequestCode.HEARTBEAT.code, byteArrayOf(0x01))

    fun setLabelType(n: Int): NiimbotPacket {
        require(n in 1..3) { "Label type must be 1-3, got $n" }
        return NiimbotPacket(RequestCode.SET_LABEL_TYPE.code, byteArrayOf(n.toByte()))
    }

    fun setLabelDensity(n: Int): NiimbotPacket {
        require(n in 1..5) { "Label density must be 1-5, got $n" }
        return NiimbotPacket(RequestCode.SET_LABEL_DENSITY.code, byteArrayOf(n.toByte()))
    }

    fun startPrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.START_PRINT.code, byteArrayOf(0x01))

    fun startPrintV2(quantity: Int): NiimbotPacket {
        require(quantity in 1..65535) { "Quantity must be 1-65535, got $quantity" }
        val data = ByteArray(7)
        data[0] = 0x00                          // Reserved
        data[1] = (quantity shr 8).toByte()     // Quantity high byte (big-endian)
        data[2] = (quantity and 0xFF).toByte()  // Quantity low byte
        // data[3..6] = 0x00 — paper type, page mode, padding (unused)
        return NiimbotPacket(RequestCode.START_PRINT.code, data)
    }

    fun endPrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.END_PRINT.code, byteArrayOf(0x01))

    fun startPagePrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.START_PAGE_PRINT.code, byteArrayOf(0x01))

    fun endPagePrint(): NiimbotPacket =
        NiimbotPacket(RequestCode.END_PAGE_PRINT.code, byteArrayOf(0x01))

    fun setDimension(height: Int, width: Int): NiimbotPacket {
        val data = ByteArray(4)
        data[0] = (height shr 8).toByte()
        data[1] = (height and 0xFF).toByte()
        data[2] = (width shr 8).toByte()
        data[3] = (width and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_DIMENSION.code, data)
    }

    fun setDimensionV2(height: Int, width: Int, copies: Int): NiimbotPacket {
        val data = ByteArray(6)
        data[0] = (height shr 8).toByte()
        data[1] = (height and 0xFF).toByte()
        data[2] = (width shr 8).toByte()
        data[3] = (width and 0xFF).toByte()
        data[4] = (copies shr 8).toByte()
        data[5] = (copies and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_DIMENSION.code, data)
    }

    fun setQuantity(n: Int): NiimbotPacket {
        require(n in 1..65535) { "Quantity must be 1-65535, got $n" }
        val data = ByteArray(2)
        data[0] = (n shr 8).toByte()
        data[1] = (n and 0xFF).toByte()
        return NiimbotPacket(RequestCode.SET_QUANTITY.code, data)
    }

    fun getPrintStatus(): NiimbotPacket =
        NiimbotPacket(RequestCode.GET_PRINT_STATUS.code, byteArrayOf(0x01))

    fun imageRow(y: Int, lineData: ByteArray): NiimbotPacket {
        require(lineData.size <= 249) { "Image row data too large: ${lineData.size} bytes (max 249 for packet payload limit)" }
        val header = ByteArray(6)
        header[0] = (y shr 8).toByte()
        header[1] = (y and 0xFF).toByte()
        header[2] = 0x00
        header[3] = 0x00
        header[4] = 0x00
        header[5] = 0x01
        return NiimbotPacket(IMAGE_DATA_TYPE, header + lineData)
    }

    // ---- Protocol v4 (NiimBlue's D110M_V4 print task; layouts follow niimbluelib's PacketGenerator) ----

    /** Connect handshake; the reply's first byte is 3 when the printer can report its protocol version. */
    fun connect(): NiimbotPacket = NiimbotPacket(RequestCode.CONNECT.code, byteArrayOf(0x01))

    fun printerStatusData(): NiimbotPacket = NiimbotPacket(RequestCode.PRINTER_STATUS_DATA.code, byteArrayOf(0x01))

    /** printStart9b: total pages, 4 reserved, page colour (0 = single), speed, flag. */
    fun startPrintV4(totalPages: Int): NiimbotPacket {
        require(totalPages in 1..65535) { "Total pages must be 1-65535, got $totalPages" }
        return NiimbotPacket(RequestCode.START_PRINT.code, u16(totalPages) + ByteArray(7))
    }

    /** setPageSize13b: rows, cols, copies, cut height, cut type, 0, send-all, part height. */
    fun setPageSizeV4(rows: Int, cols: Int, copies: Int): NiimbotPacket =
        NiimbotPacket(RequestCode.SET_DIMENSION.code, u16(rows) + u16(cols) + u16(copies) + ByteArray(7))

    /** One bitmap row: y, black-pixel counts (see [pixelCounts]), repeat count, row bits. */
    fun bitmapRowV4(y: Int, lineData: ByteArray, printheadPixels: Int): NiimbotPacket {
        require(lineData.size <= 249) { "Image row data too large: ${lineData.size} bytes" }
        return NiimbotPacket(
            RequestCode.PRINT_BITMAP_ROW.code,
            u16(y) + pixelCounts(lineData, printheadPixels) + byteArrayOf(0x01) + lineData,
        )
    }

    /** A run of blank rows starting at [y]. */
    fun emptyRow(y: Int, repeats: Int): NiimbotPacket =
        NiimbotPacket(RequestCode.PRINT_EMPTY_ROW.code, u16(y) + byteArrayOf(repeats.toByte()))

    /**
     * Black-pixel counts, as niimbluelib's countPixelsForBitmapPacket ("auto" mode): when the row
     * fits in the printhead, one count per printhead third; otherwise the total as [0, low, high].
     */
    internal fun pixelCounts(lineData: ByteArray, printheadPixels: Int): ByteArray {
        val chunkSize = printheadPixels / 8 / 3
        val split = chunkSize > 0 && lineData.size <= chunkSize * 3
        val parts = IntArray(3)
        var total = 0
        lineData.forEachIndexed { index, byte ->
            val black = (byte.toInt() and 0xFF).countOneBits()
            total += black
            if (split) parts[index / chunkSize] += black
        }
        return if (split) {
            byteArrayOf(parts[0].toByte(), parts[1].toByte(), parts[2].toByte())
        } else {
            byteArrayOf(0x00, (total and 0xFF).toByte(), (total shr 8).toByte())
        }
    }

    private fun u16(n: Int) = byteArrayOf((n shr 8).toByte(), (n and 0xFF).toByte())
}
