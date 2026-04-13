package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.model.HeartbeatResponse
import com.avicennasis.bluepaper.model.PrintStatus
import com.avicennasis.bluepaper.model.RFIDResponse
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import kotlinx.coroutines.delay

private const val END_PAGE_MAX_RETRIES = 20
private const val END_PAGE_RETRY_DELAY_MS = 50L
private const val END_PAGE_TIMEOUT_MS = 1_000L
private const val STATUS_POLL_MAX_RETRIES = 1200
private const val STATUS_POLL_DELAY_MS = 50L
private const val DEFAULT_DENSITY = 3
private const val DEFAULT_LABEL_TYPE = 1
private const val VERSION_DIVISOR = 100.0

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

    suspend fun getSoftwareVersion(): Double = getInfo(InfoEnum.SOFTWARE_VERSION) / VERSION_DIVISOR

    suspend fun getHardwareVersion(): Double = getInfo(InfoEnum.HARDWARE_VERSION) / VERSION_DIVISOR

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

    suspend fun endPagePrint(timeoutMs: Long = 10_000L): Boolean {
        val response = transport.sendCommand(CommandBuilder.endPagePrint(), timeoutMs)
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

    // ---- Polling Helpers ----

    private suspend fun waitForPagePrintEnd() {
        repeat(END_PAGE_MAX_RETRIES) {
            if (endPagePrint(timeoutMs = END_PAGE_TIMEOUT_MS)) {
                println("[PrinterClient] endPagePrint succeeded on attempt ${it + 1}")
                return
            }
            delay(END_PAGE_RETRY_DELAY_MS)
        }
        throw PrinterException("endPagePrint timed out after ${END_PAGE_MAX_RETRIES} retries")
    }

    private suspend fun waitForPrintComplete(
        quantity: Int,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        repeat(STATUS_POLL_MAX_RETRIES) {
            val status = getPrintStatus()
            onProgress?.invoke(status.page, quantity)
            if (status.page >= quantity) {
                println("[PrinterClient] Print complete: ${status.page}/$quantity pages")
                return
            }
            delay(STATUS_POLL_DELAY_MS)
        }
        throw PrinterException("Print status polling timed out after 60s")
    }

    // ---- Print Job Orchestration ----

    suspend fun print(
        imageRows: List<ByteArray>,
        width: Int,
        height: Int,
        density: Int = DEFAULT_DENSITY,
        quantity: Int,
        isV2: Boolean,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        var printStarted = false
        var pageStarted = false
        try {
            setLabelDensity(density)
            setLabelType(DEFAULT_LABEL_TYPE)

            if (isV2) {
                startPrintV2(quantity)
            } else {
                startPrint()
            }
            printStarted = true
            println("[PrinterClient] startPrint (v2=$isV2, quantity=$quantity)")

            startPagePrint()
            pageStarted = true
            println("[PrinterClient] startPagePrint")

            if (isV2) {
                setDimensionV2(height, width, quantity)
            } else {
                setDimension(height, width)
                setQuantity(quantity)
            }

            for ((y, lineData) in imageRows.withIndex()) {
                transport.writeRaw(CommandBuilder.imageRow(y, lineData))
                if ((y + 1) % 50 == 0 || y == imageRows.lastIndex) {
                    println("[PrinterClient] Sent imageRow batch: ${y + 1}/${imageRows.size}")
                }
            }

            waitForPagePrintEnd()

            waitForPrintComplete(quantity, onProgress)

            endPrint()
            println("[PrinterClient] endPrint")
        } catch (e: Exception) {
            if (pageStarted) {
                try {
                    endPagePrint()
                } catch (_: Exception) { }
            }
            if (printStarted) {
                try { endPrint() } catch (_: Exception) { }
            }
            throw e
        }
    }
}
