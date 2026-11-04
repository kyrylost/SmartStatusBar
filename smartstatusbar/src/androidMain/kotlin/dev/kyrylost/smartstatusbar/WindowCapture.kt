package dev.kyrylost.smartstatusbar

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.IntRect
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

private fun IntRect.toRect(): Rect = Rect(left, top, right, bottom)

private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

/** Time the main thread spent inside `PixelCopy.request` calls (the synchronous part), for the debug overlay. */
internal class CaptureTimer {
    var blockedNanos: Long = 0
}

internal fun createCaptureBitmap(): Bitmap = createBitmap(CAPTURE_WIDTH, CAPTURE_HEIGHT)

/**
 * Copies [src] of the window into [bmp], scaled down, and returns its pixels.
 *
 * Where a [SurfaceView] shows through, the window itself is transparent: those pixels come back
 * with alpha 0 and are filled by [fillFromSurfaceViews].
 */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun Window.capture(src: IntRect, bmp: Bitmap, timer: CaptureTimer? = null): IntArray? =
    pixelCopy(bmp, timer) { listener -> PixelCopy.request(this, src.toRect(), bmp, listener, mainHandler) }

/** Copies [src] of this surface (in surface buffer coordinates) into [bmp], scaled, and returns its pixels. */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun SurfaceView.capture(src: IntRect, bmp: Bitmap, timer: CaptureTimer? = null): IntArray? =
    pixelCopy(bmp, timer) { listener -> PixelCopy.request(this, src.toRect(), bmp, listener, mainHandler) }

/**
 * [bmp] may be reused across calls and is never recycled here: a cancelled request may still be in
 * flight, and PixelCopy requests complete in order, so only the active request's callback reads the pixels.
 */
private suspend fun pixelCopy(
    bmp: Bitmap,
    timer: CaptureTimer?,
    request: (PixelCopy.OnPixelCopyFinishedListener) -> Unit,
): IntArray? = suspendCancellableCoroutine { cont ->
    val start = System.nanoTime()
    try {
        request { result ->
            if (!cont.isActive) return@request
            val px = if (result == PixelCopy.SUCCESS) {
                IntArray(bmp.width * bmp.height).also {
                    bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                }
            } else null
            cont.resume(px)
        }
        timer?.blockedNanos += System.nanoTime() - start
    } catch (_: IllegalArgumentException) {
        if (cont.isActive) cont.resume(null)
    } catch (_: IllegalStateException) {
        if (cont.isActive) cont.resume(null)
    }
}

/** PixelCopy may fail on the first frame or on certain OEM devices; retries with delay. */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun Window.captureWithRetry(
    src: IntRect,
    bmp: Bitmap,
    attempts: Int,
    timer: CaptureTimer? = null,
): IntArray? {
    repeat(attempts) { i ->
        capture(src, bmp, timer)?.let { return it }
        if (i < attempts - 1) delay(50.milliseconds)
    }
    return null
}
