package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplyMatchingTest {
    private fun p(type: Int) = NiimbotPacket(type, byteArrayOf(1))

    @Test
    fun `expected types only`() {
        assertTrue(ResponseCodes.isReply(p(0x14), setOf(0x14)))
        assertFalse(ResponseCodes.isReply(p(0xB3), setOf(0x14)), "late status reply is not the page-size ack")
    }

    @Test
    fun `printer error always counts`() {
        assertTrue(ResponseCodes.isReply(p(0xDB), setOf(0xE4)))
    }

    @Test
    fun `without expectations only check-line notices are skipped`() {
        assertFalse(ResponseCodes.isReply(p(0xD3), null))
        assertTrue(ResponseCodes.isReply(p(0xE4), null))
    }
}
