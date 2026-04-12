package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.model.HeartbeatResponse
import com.avicennasis.bluepaper.model.PrintStatus
import com.avicennasis.bluepaper.model.RFIDResponse
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.delay

class PrinterClient(private val transport: BleTransport) {

    // ---- Info Commands ----

    suspend fun getInfo(key: InfoEnum): Long {
        val response = transport.sendCommand(CommandBuilder.getInfo(key))
        return response.dataToInt()
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getDeviceSerial(): String {
        val response = transport.sendCommand(CommandBuilder.getInfo(InfoEnum.DEVICE_SERIAL))
        return response.data.toHexString()
    }

    suspend fun getSoftwareVersion(): Double = getInfo(InfoEnum.SOFTWARE_VERSION) / 100.0

    suspend fun getHardwareVersion(): Double = getInfo(InfoEnum.HARDWARE_VERSION) / 100.0

    suspend fun heartbeat(): HeartbeatResponse {
        val response = transport.sendCommand(CommandBuilder.heartbeat())
        return HeartbeatResponse.fromData(response.data)
    }

    suspend fun getRfid(): RFIDResponse? {
        val response = transport.sendCommand(CommandBuilder.getRfid())
        return RFIDResponse.fromData(response.data)
    }

    // ---- Configuration Commands ----

    suspend fun setLabelType(n: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.setLabelType(n))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun setLabelDensity(n: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.setLabelDensity(n))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun startPrint(): Boolean {
        val response = transport.sendCommand(CommandBuilder.startPrint())
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun startPrintV2(quantity: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.startPrintV2(quantity))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun endPrint(): Boolean {
        val response = transport.sendCommand(CommandBuilder.endPrint())
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun startPagePrint(): Boolean {
        val response = transport.sendCommand(CommandBuilder.startPagePrint())
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun endPagePrint(): Boolean {
        val response = transport.sendCommand(CommandBuilder.endPagePrint())
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun setDimension(height: Int, width: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.setDimension(height, width))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun setDimensionV2(height: Int, width: Int, copies: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.setDimensionV2(height, width, copies))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun setQuantity(n: Int): Boolean {
        val response = transport.sendCommand(CommandBuilder.setQuantity(n))
        return response.data.isNotEmpty() && response.data[0] != 0.toByte()
    }

    suspend fun getPrintStatus(): PrintStatus {
        val response = transport.sendCommand(CommandBuilder.getPrintStatus())
        return PrintStatus.fromData(response.data)
    }

    // ---- Print Job Orchestration ----

    suspend fun print(
        imageRows: List<ByteArray>,
        width: Int,
        height: Int,
        density: Int,
        quantity: Int,
        isV2: Boolean,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        try {
            setLabelDensity(density)
            setLabelType(1)

            if (isV2) {
                startPrintV2(quantity)
            } else {
                startPrint()
            }

            startPagePrint()

            if (isV2) {
                setDimensionV2(height, width, quantity)
            } else {
                setDimension(height, width)
                setQuantity(quantity)
            }

            for ((y, lineData) in imageRows.withIndex()) {
                transport.writeRaw(CommandBuilder.imageRow(y, lineData))
            }

            run {
                repeat(200) {
                    if (endPagePrint()) return@run
                    delay(50)
                }
            }

            run {
                repeat(600) {
                    val status = getPrintStatus()
                    onProgress?.invoke(status.page, quantity)
                    if (status.page >= quantity) return@run
                    delay(100)
                }
            }

            endPrint()
        } catch (e: Exception) {
            try { endPrint() } catch (_: Exception) { }
            throw e
        }
    }
}
