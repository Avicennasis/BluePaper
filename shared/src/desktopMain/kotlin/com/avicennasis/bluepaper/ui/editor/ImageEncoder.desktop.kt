package com.avicennasis.bluepaper.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

actual object ImageEncoder {
    actual fun encode(bitmap: ImageBitmap, maxDimension: Int): String {
        var awtImage = bitmap.toAwtImage()

        // Downscale if needed
        val maxSide = maxOf(awtImage.width, awtImage.height)
        if (maxSide > maxDimension && maxDimension > 0) {
            val scale = maxDimension.toDouble() / maxSide
            val newW = (awtImage.width * scale).toInt().coerceAtLeast(1)
            val newH = (awtImage.height * scale).toInt().coerceAtLeast(1)
            val scaled = awtImage.getScaledInstance(newW, newH, Image.SCALE_SMOOTH)
            val buffered = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
            val g = buffered.createGraphics()
            g.drawImage(scaled, 0, 0, null)
            g.dispose()
            awtImage = buffered
        }

        val baos = ByteArrayOutputStream()
        ImageIO.write(awtImage, "png", baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    actual fun decode(base64: String): ImageBitmap? {
        return try {
            val bytes = Base64.getDecoder().decode(base64)
            val awtImage = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
            awtImage.toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
