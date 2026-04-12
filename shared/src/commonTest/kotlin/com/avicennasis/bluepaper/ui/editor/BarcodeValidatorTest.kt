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
}
