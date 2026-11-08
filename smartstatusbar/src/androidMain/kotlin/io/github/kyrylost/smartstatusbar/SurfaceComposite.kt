package io.github.kyrylost.smartstatusbar

import android.os.Build
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntRect
import androidx.core.graphics.createBitmap
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Fills transparent pixels of a window capture with the content of the [SurfaceView]s behind them
 * (video, maps, camera), composited under whatever the window draws on top.
 *
 * [px] is a [CAPTURE_WIDTH] x [CAPTURE_HEIGHT] capture of [strip] (window coordinates) and is
 * modified in place. Returns false if a surface under the strip could not be copied
 * (protected content, surface not ready).
 */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun fillFromSurfaceViews(
    root: View,
    strip: IntRect,
    px: IntArray,
    timer: CaptureTimer? = null,
): Boolean {
    if (px.all { it ushr 24 == 0xFF }) return true

    // Topmost first: it fills the holes, and surfaces behind it only see what is left.
    for (surfaceView in root.findSurfaceViews(strip).asReversed()) {
        val bounds = surfaceView.windowBounds()
        val cells = gridCells(strip, bounds, CAPTURE_WIDTH, CAPTURE_HEIGHT) ?: continue
        val window = cellsToWindow(strip, cells, CAPTURE_WIDTH, CAPTURE_HEIGHT)
        if (!window.overlaps(bounds)) continue
        val visible = window.intersect(bounds)

        // Window coordinates -> surface buffer coordinates (the buffer can differ from the view size).
        val frame = surfaceView.holder.surfaceFrame
        val sx = frame.width().toFloat() / bounds.width
        val sy = frame.height().toFloat() / bounds.height
        val src = IntRect(
            ((visible.left - bounds.left) * sx).toInt(),
            ((visible.top - bounds.top) * sy).toInt(),
            ((visible.right - bounds.left) * sx).toInt(),
            ((visible.bottom - bounds.top) * sy).toInt(),
        )
        if (src.isEmpty) continue

        val surfacePx = surfaceView.capture(src, createBitmap(cells.width, cells.height), timer) ?: return false
        fillHoles(px, CAPTURE_WIDTH, surfacePx, cells)
    }
    return true
}

/**
 * Visible [SurfaceView]s with a valid surface that overlap [area] (window coordinates), in drawing order.
 * `setZOrderOnTop` and `setZOrderMediaOverlay` are not taken into account, so stacking of several
 * overlapping surfaces is a best guess.
 */
internal fun View.findSurfaceViews(area: IntRect): List<SurfaceView> {
    val found = mutableListOf<SurfaceView>()
    fun visit(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is SurfaceView) {
            if (view.holder.surface.isValid && view.windowBounds().overlaps(area)) found += view
            return
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
    }
    visit(this)
    return found
}

private fun View.windowBounds(): IntRect {
    val location = IntArray(2)
    getLocationInWindow(location)
    return IntRect(location[0], location[1], location[0] + width, location[1] + height)
}

/**
 * Cells of the [gridWidth] x [gridHeight] capture grid over [strip] that [area] touches,
 * or null if it does not overlap the strip. All rects are in window coordinates.
 */
internal fun gridCells(strip: IntRect, area: IntRect, gridWidth: Int, gridHeight: Int): IntRect? {
    if (strip.isEmpty || !strip.overlaps(area)) return null
    val overlap = strip.intersect(area)
    val cellW = strip.width.toFloat() / gridWidth
    val cellH = strip.height.toFloat() / gridHeight
    return IntRect(
        floor((overlap.left - strip.left) / cellW).toInt(),
        floor((overlap.top - strip.top) / cellH).toInt(),
        ceil((overlap.right - strip.left) / cellW).toInt().coerceAtMost(gridWidth),
        ceil((overlap.bottom - strip.top) / cellH).toInt().coerceAtMost(gridHeight),
    )
}

/** Window area covered by [cells] of the capture grid over [strip]. */
internal fun cellsToWindow(strip: IntRect, cells: IntRect, gridWidth: Int, gridHeight: Int): IntRect {
    val cellW = strip.width.toFloat() / gridWidth
    val cellH = strip.height.toFloat() / gridHeight
    return IntRect(
        strip.left + (cells.left * cellW).toInt(),
        strip.top + (cells.top * cellH).toInt(),
        strip.left + ceil(cells.right * cellW).toInt(),
        strip.top + ceil(cells.bottom * cellH).toInt(),
    )
}

/**
 * Composites [window] pixels over [surface] pixels inside [cells] (grid coordinates; [surface] is
 * `cells.width x cells.height`). Opaque window pixels are kept, transparent ones show the surface.
 */
internal fun fillHoles(window: IntArray, width: Int, surface: IntArray, cells: IntRect) {
    for (y in cells.top until cells.bottom) {
        for (x in cells.left until cells.right) {
            val i = y * width + x
            val w = window[i]
            val alpha = w ushr 24
            if (alpha == 0xFF) continue
            val s = surface[(y - cells.top) * cells.width + (x - cells.left)]
            window[i] = (0xFF shl 24) or
                (blend(w shr 16, s shr 16, alpha) shl 16) or
                (blend(w shr 8, s shr 8, alpha) shl 8) or
                blend(w, s, alpha)
        }
    }
}

private fun blend(top: Int, bottom: Int, alpha: Int): Int =
    ((top and 0xFF) * alpha + (bottom and 0xFF) * (0xFF - alpha)) / 0xFF
