package com.avicennasis.bluepaper.printer

import com.avicennasis.bluepaper.ble.MockBleTransport
import com.avicennasis.bluepaper.protocol.InfoEnum
import com.avicennasis.bluepaper.protocol.NiimbotPacket
import com.avicennasis.bluepaper.protocol.RequestCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class PrinterClientInfoTest {

    private fun connectedClient(): Pair<MockBleTransport, PrinterClient> {
        val transport = MockBleTransport()
        transport.connectMock()
        return transport to PrinterClient(transport)
    }

    @Test
    fun getInfoBattery() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(85.toByte())))

        val battery = client.getInfo(InfoEnum.BATTERY)

        assertEquals(85L, battery)
        assertEquals(1, transport.sentCommands.size)
        assertEquals(RequestCode.GET_INFO.code, transport.sentCommands[0].type)
        assertEquals(InfoEnum.BATTERY.key.toByte(), transport.sentCommands[0].data[0])
    }

    @Test
    fun getInfoDensity() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(3)))

        val density = client.getInfo(InfoEnum.DENSITY)

        assertEquals(3L, density)
    }

    @Test
    fun getDeviceSerial() = runTest {
        val (transport, client) = connectedClient()
        val serialBytes = byteArrayOf(0x01, 0x02, 0xAB.toByte(), 0xCD.toByte())
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_INFO.code, serialBytes))

        val serial = client.getDeviceSerial()

        assertEquals("0102abcd", serial)
    }

    @Test
    fun getSoftwareVersion() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(0x00, 0xFA.toByte())))

        val version = client.getSoftwareVersion()

        assertEquals(2.5, version)
    }

    @Test
    fun getHardwareVersion() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_INFO.code, byteArrayOf(0x64)))

        val version = client.getHardwareVersion()

        assertEquals(1.0, version)
    }

    @Test
    fun heartbeat() = runTest {
        val (transport, client) = connectedClient()
        val data = ByteArray(10).also { it[8] = 0; it[9] = 75 }
        transport.enqueueResponse(NiimbotPacket(RequestCode.HEARTBEAT.code, data))

        val hb = client.heartbeat()

        assertEquals(0, hb.closingState)
        assertEquals(75, hb.powerLevel)
        assertEquals(RequestCode.HEARTBEAT.code, transport.sentCommands[0].type)
    }

    @Test
    fun getRfidWithData() = runTest {
        val (transport, client) = connectedClient()
        val rfidData = byteArrayOf(
            0x01,
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
            0x03, 0x41, 0x42, 0x43,
            0x02, 0x58, 0x59,
            0x00, 0x64, 0x00, 0x0A, 0x01,
        )
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_RFID.code, rfidData))

        val rfid = client.getRfid()

        assertNotNull(rfid)
        assertEquals("ABC", rfid.barcode)
    }

    @Test
    fun getRfidEmpty() = runTest {
        val (transport, client) = connectedClient()
        transport.enqueueResponse(NiimbotPacket(RequestCode.GET_RFID.code, byteArrayOf(0x00)))

        val rfid = client.getRfid()

        assertNull(rfid)
    }
}
