package com.avicennasis.bluepaper.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

actual object ImageEncoder {
    actual fun encode(bitmap: ImageBitmap, maxDimension: Int): String {
        var androidBitmap = bitmap.asAndroidBitmap()

        // Downscale if needed
        val maxSide = maxOf(androidBitmap.width, androidBitmap.height)
        if (maxSide > maxDimension && maxDimension > 0) {
            val scale = maxDimension.toFloat() / maxSide
            val newW = (androidBitmap.width * scale).toInt().coerceAtLeast(1)
            val newH = (androidBitmap.height * scale).toInt().coerceAtLeast(1)
            androidBitmap = Bitmap.createScaledBitmap(androidBitmap, newW, newH, true)
        }

        val baos = ByteArrayOutputStream()
        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    actual fun decode(base64: String): ImageBitmap? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            bitmap.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
