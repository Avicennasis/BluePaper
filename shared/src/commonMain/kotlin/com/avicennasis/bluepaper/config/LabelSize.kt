package com.avicennasis.bluepaper.config

data class LabelSize(
    val widthMm: Double,
    val heightMm: Double,
    val dpi: Int,
    /** Cap for [heightPx] — the printhead width when the label is wider than the head. */
    val maxHeightPx: Int = Int.MAX_VALUE,
) {
    val widthPx: Int get() = ((widthMm / 25.4) * dpi).toInt()

    /** Printable size across the printhead (D-series print rotated, so height runs across the head). */
    val heightPx: Int get() = minOf(((heightMm / 25.4) * dpi).toInt(), maxHeightPx)

    val displayName: String
        get() {
            val w = if (widthMm == widthMm.toLong().toDouble()) widthMm.toLong().toString() else widthMm.toString()
            val h = if (heightMm == heightMm.toLong().toDouble()) heightMm.toLong().toString() else heightMm.toString()
            return "${w}x${h}mm"
        }
}
