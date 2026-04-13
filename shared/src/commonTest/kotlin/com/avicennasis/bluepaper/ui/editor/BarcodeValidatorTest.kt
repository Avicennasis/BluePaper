package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BarcodeValidatorTest {
    @Test fun ean13CheckDigit() { assertEquals("5901234123457", BarcodeValidator.autoFix(BarcodeFormat.EAN_13, "590123412345")) }
    @Test fun ean13PadsShort() { val r = BarcodeValidator.autoFix(BarcodeFormat.EAN_13, "123"); assertEquals(13, r.length) }
    @Test fun ean8CheckDigit() { assertEquals("12345670", BarcodeValidator.autoFix(BarcodeFormat.EAN_8, "1234567")) }
    @Test fun upcAPads() { val r = BarcodeValidator.autoFix(BarcodeFormat.UPC_A, "03600029145"); assertEquals(12, r.length); assertEquals("036000291452", r) }
    @Test fun code39Uppercases() { assertEquals("HELLO WORLD", BarcodeValidator.autoFix(BarcodeFormat.CODE_39, "hello world")) }
    @Test fun itfPadsOdd() { assertEquals("0123", BarcodeValidator.autoFix(BarcodeFormat.ITF, "123")) }
    @Test fun itfEvenUnchanged() { assertEquals("1234", BarcodeValidator.autoFix(BarcodeFormat.ITF, "1234")) }
    @Test fun qrAcceptsAnything() { assertTrue(BarcodeValidator.validate(BarcodeFormat.QR_CODE, "anything!").isValid) }
    @Test fun ean13RejectsNonNumeric() { assertFalse(BarcodeValidator.validate(BarcodeFormat.EAN_13, "abcdef").isValid) }
    @Test fun code39RejectsInvalid() { assertFalse(BarcodeValidator.validate(BarcodeFormat.CODE_39, "hello@world").isValid) }
    @Test fun codabarFix() { val r = BarcodeValidator.autoFix(BarcodeFormat.CODABAR, "12345"); assertTrue(r.startsWith("A") && r.endsWith("A")) }
    @Test fun emptyIsInvalid() { assertFalse(BarcodeValidator.validate(BarcodeFormat.CODE_128, "").isValid) }

    // Length validation tests
    @Test fun ean13RejectsWrongLength() {
        assertFalse(BarcodeValidator.validate(BarcodeFormat.EAN_13, "12345").isValid)
    }
    @Test fun ean8RejectsWrongLength() {
        assertFalse(BarcodeValidator.validate(BarcodeFormat.EAN_8, "1234").isValid)
    }
    @Test fun upcARejectsWrongLength() {
        assertFalse(BarcodeValidator.validate(BarcodeFormat.UPC_A, "12345").isValid)
    }
    @Test fun upcERejectsWrongLength() {
        assertFalse(BarcodeValidator.validate(BarcodeFormat.UPC_E, "1234").isValid)
    }
    @Test fun itfRejectsOddLength() {
        assertFalse(BarcodeValidator.validate(BarcodeFormat.ITF, "123").isValid)
    }

    // UPC-E autoFix tests
    @Test fun upcEAutoFixProducesValidCheckDigit() {
        val fixed = BarcodeValidator.autoFix(BarcodeFormat.UPC_E, "0123456")
        assertEquals(8, fixed.length)
        assertTrue(BarcodeValidator.validate(BarcodeFormat.UPC_E, fixed).isValid)
    }

    // RSS-14 autoFix test
    @Test fun rss14AutoFixProduces14Digits() {
        val fixed = BarcodeValidator.autoFix(BarcodeFormat.RSS_14, "123456789012")
        assertEquals(14, fixed.length)
        assertTrue(fixed.all { it.isDigit() })
    }

    // ITF empty input test
    @Test fun itfAutoFixEmptyInput() {
        val fixed = BarcodeValidator.autoFix(BarcodeFormat.ITF, "")
        assertTrue(fixed.length >= 2)
        assertTrue(fixed.length % 2 == 0)
    }

    // Check digit validation
    @Test fun ean13AcceptsCorrectCheckDigit() {
        assertTrue(BarcodeValidator.validate(BarcodeFormat.EAN_13, "5901234123457").isValid)
    }

    @Test fun ean13RejectsWrongCheckDigit() {
        val result = BarcodeValidator.validate(BarcodeFormat.EAN_13, "5901234123458")
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("Invalid check digit"))
    }

    @Test fun upcAAcceptsCorrectCheckDigit() {
        assertTrue(BarcodeValidator.validate(BarcodeFormat.UPC_A, "036000291452").isValid)
    }

    @Test fun upcARejectsWrongCheckDigit() {
        val result = BarcodeValidator.validate(BarcodeFormat.UPC_A, "036000291453")
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("Invalid check digit"))
    }

    @Test fun upcERejectsInvalidNumberSystem() {
        val result = BarcodeValidator.validate(BarcodeFormat.UPC_E, "50123456")
        assertFalse(result.isValid)
        assertTrue(result.error!!.contains("number system must be 0 or 1"))
    }
}
