package com.avicennasis.bluepaper.config

data class LabelSize(
    val widthMm: Double,
    val heightMm: Double,
    val dpi: Int,
) {
    val widthPx: Int get() = ((widthMm / 25.4) * dpi).toInt()
    val heightPx: Int get() = ((heightMm / 25.4) * dpi).toInt()

    val displayName: String
        get() {
            val w = if (widthMm == widthMm.toLong().toDouble()) widthMm.toLong().toString() else widthMm.toString()
            val h = if (heightMm == heightMm.toLong().toDouble()) heightMm.toLong().toString() else heightMm.toString()
            return "${w}x${h}mm"
        }
}
