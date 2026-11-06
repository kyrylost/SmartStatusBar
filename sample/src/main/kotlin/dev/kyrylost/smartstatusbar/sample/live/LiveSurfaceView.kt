package dev.kyrylost.smartstatusbar.sample.live

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stand-in for video or map content: renders on its own thread straight into a surface,
 * so the Compose hierarchy above it never redraws.
 */
class LiveSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private var renderThread: HandlerThread? = null

    // Touched only on the render thread (or while it is stopped).
    private var time = 0f
    private var lastFrameNanos = 0L

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val thread = HandlerThread("LiveSurfaceRender").apply { start() }
        renderThread = thread
        lastFrameNanos = 0L
        // Choreographer is per thread: frames are paced by vsync on the render thread itself.
        Handler(thread.looper).post { Choreographer.getInstance().postFrameCallback(this) }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val thread = renderThread ?: return
        renderThread = null
        Handler(thread.looper).post { Choreographer.getInstance().removeFrameCallback(this) }
        // The surface must not be touched after this returns: wait for the current frame to finish.
        thread.quitSafely()
        thread.join()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameNanos != 0L) time += (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = frameTimeNanos

        // A software canvas: with lockHardwareCanvas the emulator's EGL driver crashed when this surface
        // was destroyed while the library was copying it.
        holder.lockCanvas()?.let { canvas ->
            try {
                drawFrame(canvas, time)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun drawFrame(canvas: Canvas, time: Float) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()

        // Background slowly swings between cream and ink, lingering at both ends.
        val wave = (1 - cos(time * 2 * PI / CYCLE_SECONDS)).toFloat() / 2
        val darkness = wave * wave * (3 - 2 * wave)
        canvas.drawColor(ColorUtils.blendARGB(LIGHT, DARK, darkness))

        Blobs.forEachIndexed { i, color ->
            val t = time * 0.35f + i * 2.1f
            paint.color = color
            canvas.drawCircle(
                w * (0.5f + 0.32f * sin(t * 1.3f)),
                h * (0.45f + 0.25f * cos(t * 0.9f + i)),
                w * (0.26f + 0.04f * sin(t * 2f)),
                paint,
            )
        }
    }

    private companion object {
        const val CYCLE_SECONDS = 8.0
        const val LIGHT = 0xFFF3EEE4.toInt()
        const val DARK = 0xFF0E1116.toInt()
        val Blobs = intArrayOf(0xCCFF6B6B.toInt(), 0xCCFFC857.toInt(), 0xCC2EC4B6.toInt())
    }
}
