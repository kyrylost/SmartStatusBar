package io.github.kyrylost.smartstatusbar

/**
 * What the last check that changed anything saw and decided, for [SmartStatusBarDebugOverlay].
 * [pixels] is the strip as analyzed (with SurfaceView content composited in).
 */
internal class DebugSnapshot(
    val pixels: IntArray,
    val sideFraction: Float,
    val ratio: Float,
    val lightAppearance: Boolean,
    /** Time the main thread spent inside the `PixelCopy.request` calls of the check. */
    val blockedMillis: Float,
    /** Time from the start of the capture until its pixels were ready. */
    val captureMillis: Float,
) {
    /** Same picture and decision; timings are ignored so that they do not cause endless republishing. */
    fun sameAs(other: DebugSnapshot): Boolean =
        ratio == other.ratio &&
            lightAppearance == other.lightAppearance &&
            sideFraction == other.sideFraction &&
            pixels.contentEquals(other.pixels)
}
