package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.MockBleTransport
import com.avicennasis.bluepaper.protocol.IMAGE_DATA_TYPE
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.protocol.RequestCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrinterClientPrintTest {

    private fun connectedClient(): Pair<MockBleTransport, PrinterClient> {
        val transport = MockBleTransport()
        transport.connectMock()
        return transport to PrinterClient(transport)
    }

    private fun successResponse(code: RequestCode) =
        NiimbotPacket(code.code, byteArrayOf(0x01))

    private fun statusDone(page: Int) =
        NiimbotPacket(RequestCode.GET_PRINT_STATUS.code, byteArrayOf(
            (page shr 8).toByte(), (page and 0xFF).toByte(), 100.toByte(), 100.toByte()
        ))

    private fun MockBleTransport.enqueueV1PrintResponses() {
        enqueueResponse(successResponse(RequestCode.SET_LABEL_DENSITY))
        enqueueResponse(successResponse(RequestCode.SET_LABEL_TYPE))
        enqueueResponse(successResponse(RequestCode.START_PRINT))
        enqueueResponse(successResponse(RequestCode.START_PAGE_PRINT))
        enqueueResponse(successResponse(RequestCode.SET_DIMENSION))
        enqueueResponse(successResponse(RequestCode.SET_QUANTITY))
        enqueueResponse(successResponse(RequestCode.END_PAGE_PRINT))
        enqueueResponse(statusDone(1))
        enqueueResponse(successResponse(RequestCode.END_PRINT))
    }

    private fun MockBleTransport.enqueueV2PrintResponses() {
        enqueueResponse(successResponse(RequestCode.SET_LABEL_DENSITY))
        enqueueResponse(successResponse(RequestCode.SET_LABEL_TYPE))
        enqueueResponse(successResponse(RequestCode.START_PRINT))
        enqueueResponse(successResponse(RequestCode.START_PAGE_PRINT))
        enqueueResponse(successResponse(RequestCode.SET_DIMENSION))
        enqueueResponse(successResponse(RequestCode.END_PAGE_PRINT))
        enqueueResponse(statusDone(1))
        enqueueResponse(successResponse(RequestCode.END_PRINT))
    }

    @Test
    fun v1PrintJobCommandSequence() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV1PrintResponses()

        client.print(
            imageRows = listOf(byteArrayOf(0xFF.toByte(), 0x00), byteArrayOf(0x00, 0xFF.toByte())),
            width = 16, height = 2, density = 3, quantity = 1, isV2 = false,
        )

        val cmdTypes = transport.sentCommands.map { it.type }
        assertEquals(
            listOf(
                RequestCode.SET_LABEL_DENSITY.code,
                RequestCode.SET_LABEL_TYPE.code,
                RequestCode.START_PRINT.code,
                RequestCode.START_PAGE_PRINT.code,
                RequestCode.SET_DIMENSION.code,
                RequestCode.SET_QUANTITY.code,
                RequestCode.END_PAGE_PRINT.code,
                RequestCode.GET_PRINT_STATUS.code,
                RequestCode.END_PRINT.code,
            ),
            cmdTypes,
        )

        assertEquals(2, transport.sentRaw.size)
        assertEquals(IMAGE_DATA_TYPE, transport.sentRaw[0].type)
        assertEquals(IMAGE_DATA_TYPE, transport.sentRaw[1].type)
    }

    @Test
    fun v2PrintJobCommandSequence() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV2PrintResponses()

        client.print(
            imageRows = listOf(byteArrayOf(0xFF.toByte(), 0x00)),
            width = 16, height = 1, density = 5, quantity = 1, isV2 = true,
        )

        val cmdTypes = transport.sentCommands.map { it.type }
        assertEquals(
            listOf(
                RequestCode.SET_LABEL_DENSITY.code,
                RequestCode.SET_LABEL_TYPE.code,
                RequestCode.START_PRINT.code,
                RequestCode.START_PAGE_PRINT.code,
                RequestCode.SET_DIMENSION.code,
                RequestCode.END_PAGE_PRINT.code,
                RequestCode.GET_PRINT_STATUS.code,
                RequestCode.END_PRINT.code,
            ),
            cmdTypes,
        )
    }

    @Test
    fun v1PrintSendsCorrectStartPrintPayload() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV1PrintResponses()

        client.print(listOf(byteArrayOf(0x00)), 8, 1, 3, 1, isV2 = false)

        val startPrintCmd = transport.sentCommands.first { it.type == RequestCode.START_PRINT.code }
        assertEquals(1, startPrintCmd.data.size)
        assertEquals(0x01.toByte(), startPrintCmd.data[0])
    }

    @Test
    fun v2PrintSendsCorrectStartPrintPayload() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV2PrintResponses()

        client.print(listOf(byteArrayOf(0x00)), 8, 1, 3, 1, isV2 = true)

        val startPrintCmd = transport.sentCommands.first { it.type == RequestCode.START_PRINT.code }
        assertEquals(7, startPrintCmd.data.size)
    }

    @Test
    fun v1PrintSendsSetQuantity() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV1PrintResponses()

        client.print(listOf(byteArrayOf(0x00)), 8, 1, 3, 1, isV2 = false)

        val hasSetQuantity = transport.sentCommands.any { it.type == RequestCode.SET_QUANTITY.code }
        assertEquals(true, hasSetQuantity)
    }

    @Test
    fun v2PrintDoesNotSendSetQuantity() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV2PrintResponses()

        client.print(listOf(byteArrayOf(0x00)), 8, 1, 3, 1, isV2 = true)

        val hasSetQuantity = transport.sentCommands.any { it.type == RequestCode.SET_QUANTITY.code }
        assertEquals(false, hasSetQuantity)
    }

    @Test
    fun imageRowPacketsContainRowIndex() = runTest {
        val (transport, client) = connectedClient()
        // Need fresh responses for 3-row image
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_DENSITY))
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_TYPE))
        transport.enqueueResponse(successResponse(RequestCode.START_PRINT))
        transport.enqueueResponse(successResponse(RequestCode.START_PAGE_PRINT))
        transport.enqueueResponse(successResponse(RequestCode.SET_DIMENSION))
        transport.enqueueResponse(successResponse(RequestCode.SET_QUANTITY))
        transport.enqueueResponse(successResponse(RequestCode.END_PAGE_PRINT))
        transport.enqueueResponse(statusDone(1))
        transport.enqueueResponse(successResponse(RequestCode.END_PRINT))

        client.print(
            imageRows = listOf(byteArrayOf(0xFF.toByte()), byteArrayOf(0x00), byteArrayOf(0xAA.toByte())),
            width = 8, height = 3, density = 3, quantity = 1, isV2 = false,
        )

        assertEquals(3, transport.sentRaw.size)
        // Row 0: y=0
        assertEquals(0x00.toByte(), transport.sentRaw[0].data[0])
        assertEquals(0x00.toByte(), transport.sentRaw[0].data[1])
        // Row 1: y=1
        assertEquals(0x00.toByte(), transport.sentRaw[1].data[0])
        assertEquals(0x01.toByte(), transport.sentRaw[1].data[1])
        // Row 2: y=2
        assertEquals(0x00.toByte(), transport.sentRaw[2].data[0])
        assertEquals(0x02.toByte(), transport.sentRaw[2].data[1])
    }

    @Test
    fun printCallsEndPrintOnError() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_DENSITY))
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_TYPE))
        // startPrint will throw — no response enqueued
        // But endPrint cleanup needs a response
        transport.enqueueResponse(successResponse(RequestCode.END_PRINT))

        assertFailsWith<NoSuchElementException> {
            client.print(listOf(byteArrayOf(0x00)), 8, 1, 3, 1, isV2 = false)
        }

        val lastCmd = transport.sentCommands.last()
        assertEquals(RequestCode.END_PRINT.code, lastCmd.type)
    }

    @Test
    fun progressCallbackInvoked() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueV1PrintResponses()

        var progressCalled = false
        client.print(
            imageRows = listOf(byteArrayOf(0x00)),
            width = 8, height = 1, density = 3, quantity = 1, isV2 = false,
            onProgress = { completed, total ->
                progressCalled = true
                assertEquals(1, completed)
                assertEquals(1, total)
            },
        )

        assertEquals(true, progressCalled)
    }
}
