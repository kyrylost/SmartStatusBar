package dev.kyrylost.smartstatusbar

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Throttles automatic checks after repeated capture failures (for example DRM video): once
 * [maxFailures] captures in a row failed, further attempts are skipped until [pause] has passed
 * since the last failure. A success resets it.
 */
internal class CaptureBackoff(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val maxFailures: Int = 3,
    private val pause: Duration = 2.seconds,
) {
    private var failures = 0
    private var lastFailure: TimeMark = timeSource.markNow()

    /** True while checks should be skipped. */
    val isBackingOff: Boolean get() = failures >= maxFailures && lastFailure.elapsedNow() < pause

    fun recordFailure() {
        failures++
        lastFailure = timeSource.markNow()
    }

    fun recordSuccess() {
        failures = 0
    }
}
