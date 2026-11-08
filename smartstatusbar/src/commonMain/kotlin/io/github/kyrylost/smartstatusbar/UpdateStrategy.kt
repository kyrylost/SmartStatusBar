package io.github.kyrylost.smartstatusbar

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Defines the strategy for triggering status bar color checks.
 */
public sealed interface UpdateStrategy {
    /**
     * Check after draw events settle for [timeout], so icons never change while the user is still
     * scrolling or interacting. Default strategy; best for scrolling, static and animated content.
     *
     * Content that redraws continuously (shimmer, progress indicators, a running animation) never
     * settles, so a check is forced at least once per [maxWait] while draw events keep coming.
     * Pass [Duration.INFINITE] to wait for the content to settle however long it takes.
     */
    public class Debounce(
        public val timeout: Duration = 200.milliseconds,
        public val maxWait: Duration = 1.seconds,
    ) : UpdateStrategy {
        init {
            require(timeout.isPositive()) { "timeout must be positive" }
            require(maxWait >= timeout) { "maxWait must be >= timeout" }
        }

        override fun equals(other: Any?): Boolean =
            other is Debounce && timeout == other.timeout && maxWait == other.maxWait

        override fun hashCode(): Int = 31 * timeout.hashCode() + maxWait.hashCode()

        override fun toString(): String = "Debounce(timeout=$timeout, maxWait=$maxWait)"
    }

    /**
     * Check every [interval] regardless of redraws. Use for SurfaceView/video/map content.
     */
    public class Periodic(
        public val interval: Duration = 250.milliseconds,
    ) : UpdateStrategy {
        init {
            require(interval.isPositive()) { "interval must be positive" }
        }

        override fun equals(other: Any?): Boolean = other is Periodic && interval == other.interval

        override fun hashCode(): Int = interval.hashCode()

        override fun toString(): String = "Periodic(interval=$interval)"
    }

    /**
     * Only checks requested via [SmartStatusBarController.requestUpdate], and once when the effect
     * resumes after a [SmartStatusBarOverride] has left.
     */
    public data object Manual : UpdateStrategy
}
