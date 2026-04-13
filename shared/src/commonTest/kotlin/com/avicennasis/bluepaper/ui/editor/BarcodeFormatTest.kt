package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BarcodeFormatTest {

    @Test
    fun totalFormatCount() {
        assertEquals(16, BarcodeFormat.entries.size)
    }

    @Test
    fun qrCodeIs2D() {
        assertTrue(BarcodeFormat.QR_CODE.is2D)
        assertEquals("2D Codes", BarcodeFormat.QR_CODE.category)
        assertEquals(BarcodeLibrary.ZXING, BarcodeFormat.QR_CODE.library)
    }

    @Test
    fun code128Is1D() {
        assertFalse(BarcodeFormat.CODE_128.is2D)
        assertEquals("Linear Codes", BarcodeFormat.CODE_128.category)
    }

    @Test
    fun pdf417IsZxing() {
        assertEquals(BarcodeLibrary.ZXING, BarcodeFormat.PDF_417.library)
        assertTrue(BarcodeFormat.PDF_417.is2D)
    }

    @Test
    fun categoriesAreThree() {
        val categories = BarcodeFormat.entries.map { it.category }.distinct()
        assertEquals(3, categories.size)
    }

    @Test
    fun retailFormatsAreZxing() {
        val retail = BarcodeFormat.entries.filter { it.category == "Retail" }
        assertEquals(4, retail.size)
        assertTrue(retail.all { it.library == BarcodeLibrary.ZXING })
    }

    @Test
    fun errorCorrectionLevels() {
        assertEquals(4, ErrorCorrection.entries.size)
        assertEquals("Low (7%)", ErrorCorrection.L.displayName)
    }

    @Test
    fun dataStandardCount() {
        assertEquals(13, DataStandard.entries.size)
    }

    @Test
    fun dataStandardForQrCode() {
        val standards = DataStandard.forFormat(BarcodeFormat.QR_CODE)
        assertTrue(standards.size >= 9) // RAW_TEXT + 8 QR-specific
        assertTrue(standards.contains(DataStandard.VCARD))
        assertTrue(standards.contains(DataStandard.WIFI))
    }

    @Test
    fun dataStandardForPdf417() {
        val standards = DataStandard.forFormat(BarcodeFormat.PDF_417)
        assertTrue(standards.contains(DataStandard.RAW_TEXT))
        assertTrue(standards.contains(DataStandard.AAMVA))
    }
}
