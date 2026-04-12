package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.BarcodeFormat as ZxingFormat

actual object BarcodeRenderer {
    private val cache = LinkedHashMap<String, ImageBitmap>(20, 0.75f, true)

    actual fun render(
        format: BarcodeFormat,
        data: String,
        width: Int,
        height: Int,
        errorCorrection: ErrorCorrection,
    ): ImageBitmap? {
        if (data.isEmpty() || width <= 0 || height <= 0) return null
        val key = "${format.name}|$data|$width|$height|${errorCorrection.name}"
        cache[key]?.let { return it }

        val bitmap = try {
            val zxingFormat = mapFormat(format)
            val hints = mutableMapOf<com.google.zxing.EncodeHintType, Any>()
            if (format == BarcodeFormat.QR_CODE) {
                hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = when (errorCorrection) {
                    ErrorCorrection.L -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
                    ErrorCorrection.M -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
                    ErrorCorrection.Q -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q
                    ErrorCorrection.H -> com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                }
            }
            val matrix = MultiFormatWriter().encode(data, zxingFormat, width, height, hints)
            matrix.toImageBitmap()
        } catch (_: Exception) { null }

        if (bitmap != null) {
            if (cache.size >= 20) cache.remove(cache.keys.first())
            cache[key] = bitmap
        }
        return bitmap
    }

    actual fun isFormatAvailable(format: BarcodeFormat): Boolean = true

    private fun mapFormat(format: BarcodeFormat): ZxingFormat = when (format) {
        BarcodeFormat.QR_CODE -> ZxingFormat.QR_CODE
        BarcodeFormat.PDF_417 -> ZxingFormat.PDF_417
        BarcodeFormat.DATA_MATRIX -> ZxingFormat.DATA_MATRIX
        BarcodeFormat.AZTEC -> ZxingFormat.AZTEC
        BarcodeFormat.MAXICODE -> ZxingFormat.MAXICODE
        BarcodeFormat.RSS_EXPANDED -> ZxingFormat.RSS_EXPANDED
        BarcodeFormat.CODE_128 -> ZxingFormat.CODE_128
        BarcodeFormat.CODE_39 -> ZxingFormat.CODE_39
        BarcodeFormat.CODE_93 -> ZxingFormat.CODE_93
        BarcodeFormat.CODABAR -> ZxingFormat.CODABAR
        BarcodeFormat.ITF -> ZxingFormat.ITF
        BarcodeFormat.RSS_14 -> ZxingFormat.RSS_14
        BarcodeFormat.EAN_13 -> ZxingFormat.EAN_13
        BarcodeFormat.EAN_8 -> ZxingFormat.EAN_8
        BarcodeFormat.UPC_A -> ZxingFormat.UPC_A
        BarcodeFormat.UPC_E -> ZxingFormat.UPC_E
    }

    private fun BitMatrix.toImageBitmap(): ImageBitmap {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = if (this[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp.asImageBitmap()
    }
}
