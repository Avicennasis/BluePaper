package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PhomemoD30ProtocolTest {

    @Test
    fun headerEncodesWidthAndHeightLittleEndian() {
        assertContentEquals(
            byteArrayOf(0x1B, 0x40, 0x1D, 0x76, 0x30, 0x00, 12, 0, 2, 0),
            PhomemoD30Protocol.header(widthBytes = 12, heightRows = 2),
        )
    }

    @Test
    fun headerEncodesLargeHeight() {
        // 300 rows -> lo = 0x2C, hi = 0x01
        val h = PhomemoD30Protocol.header(widthBytes = 12, heightRows = 300)
        assertEquals(0x2C.toByte(), h[8])
        assertEquals(0x01.toByte(), h[9])
    }

    @Test
    fun payloadIsRowMajor() {
        val rows = listOf(byteArrayOf(1, 2), byteArrayOf(3, 4))
        assertContentEquals(byteArrayOf(1, 2, 3, 4), PhomemoD30Protocol.payload(rows))
    }

    @Test
    fun frameIsHeaderPayloadFooter() {
        val rows = listOf(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        val frame = PhomemoD30Protocol.frame(rows)
        assertEquals(10 + 2 + 3, frame.size)
        assertEquals(0x1B.toByte(), frame[0])
        assertContentEquals(byteArrayOf(0x1B, 0x64, 0x00), frame.takeLast(3).toByteArray())
    }

    @Test
    fun chunksSplitAt128() {
        val data = ByteArray(300) { it.toByte() }
        val chunks = PhomemoD30Protocol.chunks(data)
        assertEquals(listOf(128, 128, 44), chunks.map { it.size })
        assertContentEquals(data, chunks.reduce { a, b -> a + b })
    }

    @Test
    fun raggedRowsRejected() {
        assertFailsWith<IllegalArgumentException> {
            PhomemoD30Protocol.payload(listOf(byteArrayOf(1, 2), byteArrayOf(3)))
        }
    }
}
