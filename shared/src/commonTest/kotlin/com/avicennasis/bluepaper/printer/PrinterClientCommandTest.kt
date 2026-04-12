package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.MockBleTransport
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.protocol.RequestCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrinterClientCommandTest {

    private fun connectedClient(): Pair<MockBleTransport, PrinterClient> {
        val transport = MockBleTransport()
        transport.connectMock()
        return transport to PrinterClient(transport)
    }

    private fun successResponse(code: RequestCode) =
        NiimbotPacket(code.code, byteArrayOf(0x01))

    private fun failResponse(code: RequestCode) =
        NiimbotPacket(code.code, byteArrayOf(0x00))

    @Test
    fun setLabelTypeSuccess() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_TYPE))
        assertTrue(client.setLabelType(1))
        assertEquals(RequestCode.SET_LABEL_TYPE.code, transport.sentCommands[0].type)
    }

    @Test
    fun setLabelTypeFail() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(failResponse(RequestCode.SET_LABEL_TYPE))
        assertFalse(client.setLabelType(1))
    }

    @Test
    fun setLabelDensitySuccess() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_LABEL_DENSITY))
        assertTrue(client.setLabelDensity(3))
        assertEquals(RequestCode.SET_LABEL_DENSITY.code, transport.sentCommands[0].type)
    }

    @Test
    fun startPrintV1() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.START_PRINT))
        assertTrue(client.startPrint())
        assertEquals(RequestCode.START_PRINT.code, transport.sentCommands[0].type)
        assertEquals(1, transport.sentCommands[0].data.size)
    }

    @Test
    fun startPrintV2() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.START_PRINT))
        assertTrue(client.startPrintV2(3))
        assertEquals(RequestCode.START_PRINT.code, transport.sentCommands[0].type)
        assertEquals(7, transport.sentCommands[0].data.size)
    }

    @Test
    fun endPrintSuccess() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.END_PRINT))
        assertTrue(client.endPrint())
    }

    @Test
    fun setDimensionV1() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_DIMENSION))
        assertTrue(client.setDimension(100, 240))
        assertEquals(4, transport.sentCommands[0].data.size)
    }

    @Test
    fun setDimensionV2() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_DIMENSION))
        assertTrue(client.setDimensionV2(100, 384, 2))
        assertEquals(6, transport.sentCommands[0].data.size)
    }

    @Test
    fun setQuantity() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.SET_QUANTITY))
        assertTrue(client.setQuantity(5))
        assertEquals(RequestCode.SET_QUANTITY.code, transport.sentCommands[0].type)
    }

    @Test
    fun getPrintStatus() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_PRINT_STATUS.code, byteArrayOf(0x00, 0x01, 0x32, 0x4B)))
        val status = client.getPrintStatus()
        assertEquals(1, status.page)
        assertEquals(50, status.progress1)
        assertEquals(75, status.progress2)
    }

    @Test
    fun startPagePrintSuccess() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.START_PAGE_PRINT))
        assertTrue(client.startPagePrint())
    }

    @Test
    fun endPagePrintSuccess() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(successResponse(RequestCode.END_PAGE_PRINT))
        assertTrue(client.endPagePrint())
    }

    @Test
    fun endPagePrintNotReady() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(failResponse(RequestCode.END_PAGE_PRINT))
        assertFalse(client.endPagePrint())
    }
}
