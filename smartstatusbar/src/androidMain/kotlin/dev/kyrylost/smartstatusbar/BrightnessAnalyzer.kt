package dev.kyrylost.smartstatusbar

internal const val CAPTURE_WIDTH: Int = 64
internal const val CAPTURE_HEIGHT: Int = 4

/** Number of columns analyzed at each edge of a [width]-pixel strip: at least one, at most half. */
internal fun sideColumns(width: Int, sideFraction: Float): Int =
    (width * sideFraction).toInt().coerceIn(1, maxOf(1, width / 2))

/**
 * Share of light pixels (0..1) in the left and right zones of the strip,
 * where status bar icons are drawn. Fully transparent pixels (nothing is drawn there) are skipped.
 * Returns null if there is nothing to analyze.
 */
internal fun lightPixelRatio(
    px: IntArray,
    width: Int,
    sideFraction: Float,
    threshold: Float,
): Float? {
    if (width <= 0 || px.size < width) return null
    val height = px.size / width
    val side = sideColumns(width, sideFraction)
    val bound = (threshold * 255f).toInt()

    var light = 0
    var total = 0
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if (x in side until (width - side)) continue
            val p = px[row + x]
            if (p ushr 24 == 0) continue
            // Rec. 601 luma on gamma-encoded sRGB (no linearization): good enough for a light/dark decision.
            val lum = (((p shr 16) and 0xFF) * 77 + ((p shr 8) and 0xFF) * 150 + (p and 0xFF) * 29) shr 8
            if (lum > bound) light++
            total++
        }
    }
    return if (total == 0) null else light.toFloat() / total
}

/**
 * true = light background -> dark icons (isAppearanceLightStatusBars = true). [current] is what the
 * window shows now; it only changes once [ratio] leaves `0.5 ± hysteresis`.
 */
internal fun decide(ratio: Float, current: Boolean, hysteresis: Float): Boolean = when {
    current && ratio < 0.5f - hysteresis -> false
    !current && ratio > 0.5f + hysteresis -> true
    else -> current
}
