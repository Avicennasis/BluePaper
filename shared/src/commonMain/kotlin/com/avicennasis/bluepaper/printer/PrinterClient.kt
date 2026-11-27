package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.BleTransport
import com.avicennasis.bluepaper.config.DeviceConfig
import com.avicennasis.bluepaper.model.HeartbeatResponse
import com.avicennasis.bluepaper.model.HeartbeatStatus
import com.avicennasis.bluepaper.model.PrintStatus
import com.avicennasis.bluepaper.model.RFIDResponse
import com.avicennasis.bluepaper.protocol.CommandBuilder
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.protocol.ProtocolVersion
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

    // ---- Protocol v4 ----

    /** Connect handshake; returns the printer's protocol version (0 when it can't say). */
    suspend fun negotiateProtocolVersion(): Int {
        val reply = transport.sendCommand(CommandBuilder.connect())
        return when (reply.data.firstOrNull()?.toInt()) {
            2 -> 1
            3 -> ProtocolVersion.fromStatusData(transport.sendCommand(CommandBuilder.printerStatusData()).data)
            else -> 0
        }
    }

    /**
     * Prints with the sequence [config]'s printer needs. A D110_M on firmware 4.x acks every
     * legacy command and reports 100% progress but never feeds or burns a label — it only
     * prints with the protocol-v4 sequence.
     */
    suspend fun printJob(
        config: DeviceConfig,
        imageRows: List<ByteArray>,
        width: Int,
        height: Int,
        density: Int = DEFAULT_DENSITY,
        quantity: Int,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        val version = if (config.v4WhenSupported) {
            try {
                negotiateProtocolVersion()
            } catch (e: PrinterException) {
                println("[PrinterClient] Protocol negotiation failed (${e.message}); using legacy sequence")
                0
            }
        } else {
            0
        }
        println("[PrinterClient] ${config.model}: protocol v$version")
        if (version >= 4) {
            // Check like the official app does: a D110_M with no readable RFID tag accepts the
            // job, reports success and feeds a blank label. Undecodable replies don't block.
            val status = try {
                heartbeatStatus()
            } catch (e: PrinterException) {
                println("[PrinterClient] Status check failed (${e.message}); printing anyway")
                null
            }
            println("[PrinterClient] status: $status")
            status?.let { notReadyReason(it) }?.let { throw PrinterNotReadyException(it) }
            printV4(imageRows, width, density, quantity, config.printheadPixels, onProgress)
        } else {
            print(imageRows, width, height, density, quantity, config.isV2, onProgress)
        }
    }

    /** Lid / labels / RFID state from heartbeat type 4. */
    suspend fun heartbeatStatus(): HeartbeatStatus =
        HeartbeatStatus.fromReply(transport.sendCommand(CommandBuilder.heartbeat(type = 4)))

    internal fun notReadyReason(status: HeartbeatStatus): String? = when {
        status.lidClosed == false -> "The printer lid is open — close it and try again."
        status.paperInserted == false -> "No labels detected — load a label roll and try again."
        status.rfidRead == false ->
            "The printer can't read the label roll's RFID tag, so it won't print (it would only feed " +
                "a blank label). Use a NIIMBOT roll whose tag the printer can read."
        else -> null
    }

    /** NiimBlue's D110M_V4 print task. [width] is the row width in pixels (padded to 8 here). */
    suspend fun printV4(
        imageRows: List<ByteArray>,
        width: Int,
        density: Int = DEFAULT_DENSITY,
        quantity: Int,
        printheadPixels: Int,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        var printStarted = false
        try {
            setLabelType(DEFAULT_LABEL_TYPE)
            setLabelDensity(density)
            transport.sendCommand(CommandBuilder.startPrintV4(quantity))
            printStarted = true
            // The official app sends a status request right after PrintStart without waiting for it.
            transport.writeRaw(CommandBuilder.getPrintStatus())

            val cols = (width + 7) / 8 * 8
            transport.sendCommand(CommandBuilder.setPageSizeV4(rows = imageRows.size, cols = cols, copies = quantity))
            println("[PrinterClient] v4 page ${imageRows.size} rows x $cols cols, quantity=$quantity")

            for ((y, row) in imageRows.withIndex()) {
                val packet = if (row.all { it == 0.toByte() }) {
                    CommandBuilder.emptyRow(y, repeats = 1)
                } else {
                    CommandBuilder.bitmapRowV4(y, row, printheadPixels)
                }
                transport.writeRaw(packet)
            }

            waitForPagePrintEnd()
            waitForPrintComplete(quantity, onProgress)
            endPrint()
            println("[PrinterClient] v4 endPrint")
        } catch (e: Exception) {
            if (printStarted) {
                try { endPrint() } catch (_: Exception) { }
            }
            throw e
        }
    }
}
