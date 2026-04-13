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
        val header = ByteArray(6)
        header[0] = (y shr 8).toByte()
        header[1] = (y and 0xFF).toByte()
        header[2] = 0x00
        header[3] = 0x00
        header[4] = 0x00
        header[5] = 0x01
        return NiimbotPacket(IMAGE_DATA_TYPE, header + lineData)
    }
}
