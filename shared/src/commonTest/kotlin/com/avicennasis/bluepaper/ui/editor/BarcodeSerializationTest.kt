package com.avicennasis.bluepaper.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BarcodeSerializationTest {
    @Test fun barcodeDefaults() {
        val el = LabelElement.BarcodeElement(id = "b1")
        assertEquals(100f, el.width); assertEquals("", el.data)
        assertEquals(BarcodeFormat.QR_CODE, el.format)
        assertEquals(ErrorCorrection.M, el.errorCorrection)
        assertEquals(DataStandard.RAW_TEXT, el.dataStandard)
    }
    @Test fun barcodeRoundTrip() {
        val el = LabelElement.BarcodeElement(id = "b1", x = 10f, y = 20f, data = "https://example.com",
            format = BarcodeFormat.QR_CODE, errorCorrection = ErrorCorrection.H,
            dataStandard = DataStandard.URL, structuredData = mapOf("url" to "https://example.com"))
        val s = el.toSerializable()
        assertEquals("barcode", s.type); assertEquals("QR_CODE", s.barcodeFormat)
        assertEquals("H", s.errorCorrection); assertEquals("URL", s.dataStandard)
        val r = s.toLabelElement()
        assertIs<LabelElement.BarcodeElement>(r)
        assertEquals(el.data, (r as LabelElement.BarcodeElement).data)
        assertEquals(el.format, r.format); assertEquals(el.dataStandard, r.dataStandard)
    }
    @Test fun code128Serialization() {
        val el = LabelElement.BarcodeElement(id = "b2", data = "SHIP-123", format = BarcodeFormat.CODE_128)
        val r = el.toSerializable().toLabelElement() as LabelElement.BarcodeElement
        assertEquals(BarcodeFormat.CODE_128, r.format)
    }
}
