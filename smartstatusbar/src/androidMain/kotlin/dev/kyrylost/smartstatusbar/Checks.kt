package dev.kyrylost.smartstatusbar

import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val FRAME_COMMIT_TIMEOUT: Duration = 500.milliseconds
private val FALLBACK_RENDER_WAIT: Duration = 50.milliseconds

/**
 * What the checks of one effect need from where it lives: a composition ([SmartStatusBarEffect]) or
 * a View hierarchy ([SmartStatusBar]).
 */
internal class CheckHost(
    val window: Window,
    /** A view of [window]; its observer delivers the window's draw events. */
    val view: View,
    val density: Density,
    /** Status bar height in pixels right now. */
    val statusBarTop: () -> Int,
    /** Emits when the status bar height changes to a non-zero value; not for the current height. */
    val insetsChanges: Flow<Unit>,
    /** Suspends until the next frame has been drawn. */
    val awaitFrame: suspend () -> Unit,
)

/**
 * Checks what is under the status bar of [host]'s window according to [config], while [lifecycle] is
 * at least STARTED, and sets the icons accordingly. Returns only when cancelled.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal suspend fun runChecks(host: CheckHost, state: WindowState, config: SmartStatusBarConfig, lifecycle: Lifecycle) {
    val window = host.window
    val insetsController = WindowCompat.getInsetsController(window, host.view)

    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        val bitmap = createCaptureBitmap()
        val backoff = CaptureBackoff()

        suspend fun check(captureAttempts: Int = 1): Boolean {
            val rect = stripRect(host.statusBarTop(), window.decorView.width, host.density) ?: return false
            val timer = CaptureTimer()
            val start = System.nanoTime()
            val px = window.captureWithRetry(rect, bitmap, captureAttempts, timer)
                ?.takeIf { fillFromSurfaceViews(window.decorView, rect, it, timer) }
            val captureNanos = System.nanoTime() - start
            if (px == null) {
                backoff.recordFailure()
                return false
            }
            backoff.recordSuccess()
            val ratio = lightPixelRatio(px, CAPTURE_WIDTH, config.sideFraction, config.lightThreshold)
                ?: return false
            // Hysteresis applies to what the window shows, so a restart (the host coming back to the
            // foreground) does not flip icons on content close to the threshold.
            val shown = insetsController.isAppearanceLightStatusBars
            val next = decide(ratio, shown, config.hysteresis)
            if (state.isDebugObserved) {
                state.publish(
                    DebugSnapshot(
                        pixels = px,
                        sideFraction = config.sideFraction,
                        ratio = ratio,
                        lightAppearance = next,
                        blockedMillis = timer.blockedNanos / 1e6f,
                        captureMillis = captureNanos / 1e6f,
                    ),
                )
            }
            if (shown != next) insetsController.isAppearanceLightStatusBars = next
            state.recordDecision(next)
            return true
        }

        // requestUpdate() usually follows a state change (a click, an image loader callback), and the
        // new content only reaches the screen in a later frame: let it render before capturing.
        suspend fun manualCheck() {
            host.awaitRenderedFrame()
            check(captureAttempts = 3)
        }

        // After several consecutive capture failures (DRM) checks are throttled.
        suspend fun autoCheck() {
            if (backoff.isBackingOff) return
            check()
        }

        // The window has nothing to copy before its first frame is drawn.
        host.awaitFrame()

        val triggers: Flow<Unit> = when (val strategy = config.strategy) {
            // Starting with a draw event makes the first check wait for the screen to settle too, so an
            // enter transition or a crossfade from other content does not flip the icons halfway.
            is UpdateStrategy.Debounce -> merge(
                host.view.drawEvents().onStart { emit(Unit) }.debounceWithMaxWait(strategy.timeout, strategy.maxWait),
                host.insetsChanges,
            )
            is UpdateStrategy.Periodic ->
                merge(ticker(strategy.interval), host.insetsChanges)
            UpdateStrategy.Manual -> emptyFlow()
        }

        merge(triggers.map { false }, state.requests.map { true })
            .collectLatest { manual ->
                if (manual) manualCheck() else autoCheck()
            }
    }
}

private fun ticker(interval: Duration): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(interval)
    }
}

/**
 * Suspends until content changed before this call has been rendered, so a PixelCopy made afterwards
 * sees it. Counting UI frames is not enough: the render thread can still be drawing a frame
 * (e.g. a large image) a few vsyncs after the UI thread has moved on, especially at high refresh rates.
 */
private suspend fun CheckHost.awaitRenderedFrame() {
    // Pending state changes are applied and drawn in this frame.
    awaitFrame()
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // No frame commit callback before API 29: give the render thread a few more vsyncs.
        awaitFrame()
        delay(FALLBACK_RENDER_WAIT)
        return
    }
    withTimeoutOrNull(FRAME_COMMIT_TIMEOUT) {
        suspendCancellableCoroutine { cont ->
            // One-shot: invoked once the next drawn frame has been rendered and submitted for display.
            view.viewTreeObserver.registerFrameCommitCallback { if (cont.isActive) cont.resume(Unit) }
            // Make sure that frame happens even if nothing else is invalidated.
            view.invalidate()
        }
    }
}
