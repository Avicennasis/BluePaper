package com.avicennasis.bluepaper.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrintStatusTest {

    @Test
    fun parseNormalStatus() {
        val data = byteArrayOf(0x00, 0x01, 0x32, 0x4B)
        val status = PrintStatus.fromData(data)
        assertEquals(1, status.page)
        assertEquals(50, status.progress1)
        assertEquals(75, status.progress2)
    }

    @Test
    fun parseHighPageCount() {
        val data = byteArrayOf(0x01, 0x00, 0x64, 0x64)
        val status = PrintStatus.fromData(data)
        assertEquals(256, status.page)
        assertEquals(100, status.progress1)
        assertEquals(100, status.progress2)
    }

    @Test
    fun rejectsTooShortData() {
        assertFailsWith<IllegalArgumentException> {
            PrintStatus.fromData(byteArrayOf(0x00, 0x01, 0x32))
        }
    }
}
