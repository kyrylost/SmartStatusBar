package io.github.kyrylost.smartstatusbar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class CaptureBackoffTest {

    private val time = TestTimeSource()
    private val backoff = CaptureBackoff(time, maxFailures = 3, pause = 2.seconds)

    private fun fail(times: Int) = repeat(times) { backoff.recordFailure() }

    @Test
    fun fewFailures_doNotBackOff() {
        fail(2)
        assertFalse(backoff.isBackingOff)
    }

    @Test
    fun enoughFailures_backOffUntilThePauseHasPassed() {
        fail(3)
        assertTrue(backoff.isBackingOff)

        time += 1_999.milliseconds
        assertTrue(backoff.isBackingOff)

        time += 1.milliseconds
        assertFalse(backoff.isBackingOff)
    }

    @Test
    fun failureAfterThePause_startsANewPause() {
        fail(3)
        time += 3.seconds
        backoff.recordFailure()

        assertTrue(backoff.isBackingOff)
        time += 2.seconds
        assertFalse(backoff.isBackingOff)
    }

    @Test
    fun success_resetsTheFailureCount() {
        fail(3)
        backoff.recordSuccess()
        assertFalse(backoff.isBackingOff)

        fail(2)
        assertFalse(backoff.isBackingOff)
    }
}
