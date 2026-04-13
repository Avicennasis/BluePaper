package com.avicennasis.bluepaper.ui.editor

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BarcodeSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

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

    @Test fun unknownBarcodeFormatDeserializesGracefully() {
        // Simulate a barcode element with an unknown format string.
        // The toLabelElement() function uses runCatching { BarcodeFormat.valueOf(it) }.getOrNull()
        // so an unrecognized format should fall back to QR_CODE.
        val raw = """{"type":"barcode","id":"b_1","x":10.0,"y":10.0,"width":100.0,"height":100.0,"barcodeFormat":"NONEXISTENT","barcodeData":"test"}"""
        val s = json.decodeFromString(SerializableLabelElement.serializer(), raw)
        assertEquals("NONEXISTENT", s.barcodeFormat)
        val el = s.toLabelElement()
        assertIs<LabelElement.BarcodeElement>(el)
        val barcode = el as LabelElement.BarcodeElement
        assertEquals(BarcodeFormat.QR_CODE, barcode.format) // falls back to default
        assertEquals("test", barcode.data)
    }

    @Test fun missingBarcodeFormatDefaultsGracefully() {
        // A barcode element with no barcodeFormat field should not crash
        // and should default to QR_CODE.
        val raw = """{"type":"barcode","id":"b_1","x":10.0,"y":10.0,"width":100.0,"height":100.0}"""
        val s = json.decodeFromString(SerializableLabelElement.serializer(), raw)
        assertEquals(null, s.barcodeFormat)
        val el = s.toLabelElement()
        assertIs<LabelElement.BarcodeElement>(el)
        val barcode = el as LabelElement.BarcodeElement
        assertEquals(BarcodeFormat.QR_CODE, barcode.format) // null falls back to default
        assertEquals("", barcode.data) // missing barcodeData defaults to ""
        assertEquals(ErrorCorrection.M, barcode.errorCorrection) // missing defaults to M
        assertEquals(DataStandard.RAW_TEXT, barcode.dataStandard) // missing defaults to RAW_TEXT
    }

    @Test fun unknownErrorCorrectionDefaultsGracefully() {
        // An unrecognized errorCorrection should fall back to M.
        val raw = """{"type":"barcode","id":"b_2","x":0.0,"y":0.0,"width":100.0,"height":100.0,"barcodeFormat":"QR_CODE","errorCorrection":"ULTRA","barcodeData":"abc"}"""
        val s = json.decodeFromString(SerializableLabelElement.serializer(), raw)
        val el = s.toLabelElement() as LabelElement.BarcodeElement
        assertEquals(ErrorCorrection.M, el.errorCorrection)
    }

    @Test fun unknownDataStandardDefaultsGracefully() {
        // An unrecognized dataStandard should fall back to RAW_TEXT.
        val raw = """{"type":"barcode","id":"b_3","x":0.0,"y":0.0,"width":100.0,"height":100.0,"barcodeFormat":"QR_CODE","dataStandard":"GALACTIC","barcodeData":"xyz"}"""
        val s = json.decodeFromString(SerializableLabelElement.serializer(), raw)
        val el = s.toLabelElement() as LabelElement.BarcodeElement
        assertEquals(DataStandard.RAW_TEXT, el.dataStandard)
    }

    @Test fun corruptBarcodeInLabelDesignDeserializesGracefully() {
        // Full LabelDesign JSON with a barcode element containing unknown format/correction/standard.
        // LabelDesign.fromJson uses ignoreUnknownKeys = true and the same fallback logic.
        val designJson = """
        {
            "version": 2,
            "model": "d110",
            "labelWidthMm": 30.0,
            "labelHeightMm": 15.0,
            "density": 3,
            "quantity": 1,
            "elements": [
                {
                    "type": "barcode",
                    "id": "b_corrupt",
                    "x": 5.0,
                    "y": 5.0,
                    "width": 80.0,
                    "height": 80.0,
                    "barcodeFormat": "BOGUS_FORMAT",
                    "errorCorrection": "BOGUS_EC",
                    "dataStandard": "BOGUS_DS",
                    "barcodeData": "hello"
                }
            ]
        }
        """.trimIndent()
        val design = LabelDesign.fromJson(designJson)
        assertEquals(1, design.elements.size)
        val el = design.elements[0].toLabelElement() as LabelElement.BarcodeElement
        assertEquals(BarcodeFormat.QR_CODE, el.format)
        assertEquals(ErrorCorrection.M, el.errorCorrection)
        assertEquals(DataStandard.RAW_TEXT, el.dataStandard)
        assertEquals("hello", el.data)
    }
}
