package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class CommandBuilderTest {

    @Test
    fun getInfoPacket() {
        val pkt = CommandBuilder.getInfo(InfoEnum.BATTERY)
        assertEquals(RequestCode.GET_INFO.code, pkt.type)
        assertContentEquals(byteArrayOf(10), pkt.data)
    }

    @Test
    fun getRfidPacket() {
        val pkt = CommandBuilder.getRfid()
        assertEquals(RequestCode.GET_RFID.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun heartbeatPacket() {
        val pkt = CommandBuilder.heartbeat()
        assertEquals(RequestCode.HEARTBEAT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setLabelTypePacket() {
        val pkt = CommandBuilder.setLabelType(1)
        assertEquals(RequestCode.SET_LABEL_TYPE.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setLabelTypeRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelType(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelType(4) }
    }

    @Test
    fun setLabelDensityPacket() {
        val pkt = CommandBuilder.setLabelDensity(3)
        assertEquals(RequestCode.SET_LABEL_DENSITY.code, pkt.type)
        assertContentEquals(byteArrayOf(0x03), pkt.data)
    }

    @Test
    fun setLabelDensityRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelDensity(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setLabelDensity(6) }
    }

    @Test
    fun startPrintV1Packet() {
        val pkt = CommandBuilder.startPrint()
        assertEquals(RequestCode.START_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun startPrintV2Packet() {
        val pkt = CommandBuilder.startPrintV2(quantity = 3)
        assertEquals(RequestCode.START_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00), pkt.data)
    }

    @Test
    fun startPrintV2HighQuantity() {
        val pkt = CommandBuilder.startPrintV2(quantity = 500)
        assertContentEquals(byteArrayOf(0x00, 0x01, 0xF4.toByte(), 0x00, 0x00, 0x00, 0x00), pkt.data)
    }

    @Test
    fun startPrintV2RejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.startPrintV2(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.startPrintV2(65536) }
    }

    @Test
    fun endPrintPacket() {
        val pkt = CommandBuilder.endPrint()
        assertEquals(RequestCode.END_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun startPagePrintPacket() {
        val pkt = CommandBuilder.startPagePrint()
        assertEquals(RequestCode.START_PAGE_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun endPagePrintPacket() {
        val pkt = CommandBuilder.endPagePrint()
        assertEquals(RequestCode.END_PAGE_PRINT.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun setDimensionV1Packet() {
        val pkt = CommandBuilder.setDimension(height = 100, width = 240)
        assertEquals(RequestCode.SET_DIMENSION.code, pkt.type)
        assertContentEquals(byteArrayOf(0x00, 0x64, 0x00, 0xF0.toByte()), pkt.data)
    }

    @Test
    fun setDimensionV2Packet() {
        val pkt = CommandBuilder.setDimensionV2(height = 100, width = 384, copies = 2)
        assertEquals(RequestCode.SET_DIMENSION.code, pkt.type)
        assertContentEquals(byteArrayOf(0x00, 0x64, 0x01, 0x80.toByte(), 0x00, 0x02), pkt.data)
    }

    @Test
    fun setQuantityPacket() {
        val pkt = CommandBuilder.setQuantity(5)
        assertEquals(RequestCode.SET_QUANTITY.code, pkt.type)
        assertContentEquals(byteArrayOf(0x00, 0x05), pkt.data)
    }

    @Test
    fun setQuantityRejectsOutOfRange() {
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setQuantity(0) }
        assertFailsWith<IllegalArgumentException> { CommandBuilder.setQuantity(65536) }
    }

    @Test
    fun getPrintStatusPacket() {
        val pkt = CommandBuilder.getPrintStatus()
        assertEquals(RequestCode.GET_PRINT_STATUS.code, pkt.type)
        assertContentEquals(byteArrayOf(0x01), pkt.data)
    }

    @Test
    fun imageRowPacket() {
        val lineData = byteArrayOf(0xFF.toByte(), 0x00)
        val pkt = CommandBuilder.imageRow(y = 42, lineData = lineData)
        assertEquals(IMAGE_DATA_TYPE, pkt.type)
        val expectedData = byteArrayOf(0x00, 0x2A, 0x00, 0x00, 0x00, 0x01, 0xFF.toByte(), 0x00)
        assertContentEquals(expectedData, pkt.data)
    }
}
