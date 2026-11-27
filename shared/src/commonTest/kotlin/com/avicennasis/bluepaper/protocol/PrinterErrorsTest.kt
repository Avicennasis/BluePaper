package com.avicennasis.bluepaper.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrinterErrorsTest {
    @Test
    fun `rfid write failure explains the label roll`() {
        // A D110_M answered PrintStart with 0xDB 0x14 once the roll's RFID tag read used 187 / total 186.
        assertTrue("RFID" in PrinterErrors.describe(0x14))
    }

    @Test
    fun `common codes have readable names`() {
        assertEquals("Lid is open", PrinterErrors.describe(0x01))
        assertEquals("Out of labels", PrinterErrors.describe(0x02))
    }

    @Test
    fun `unknown codes keep the number`() {
        assertTrue("0x63" in PrinterErrors.describe(0x63))
    }
}
