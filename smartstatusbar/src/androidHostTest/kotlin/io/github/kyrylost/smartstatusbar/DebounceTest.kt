package io.github.kyrylost.smartstatusbar

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class DebounceTest {

    /** Emits 0, 1, 2, ... every [periodMs] ms, [count] times. */
    private fun burst(count: Int, periodMs: Long) = flow {
        repeat(count) {
            emit(it)
            delay(periodMs)
        }
    }

    @Test
    fun withoutMaxWait_emitsOnlyAfterSettling() = runTest {
        val start = currentTime
        val out = burst(count = 10, periodMs = 50)
            .debounceWithMaxWait(timeout = 200.milliseconds, maxWait = Duration.INFINITE)
            .map { it to currentTime - start }
            .toList()
        // Last value emitted at 450 ms; the flow completes at 500 ms and debounce flushes it.
        assertEquals(listOf(9), out.map { it.first })
    }

    @Test
    fun withMaxWait_settlesLikeDebounce() = runTest {
        val start = currentTime
        val out = flow {
            emit(0)
            delay(50)
            emit(1)
            delay(1_000)
        }
            .debounceWithMaxWait(timeout = 200.milliseconds, maxWait = 500.milliseconds)
            .map { it to currentTime - start }
            .toList()
        assertEquals(listOf(1 to 250L), out)
    }

    @Test
    fun withMaxWait_emitsDuringEndlessBurst() = runTest {
        val start = currentTime
        // ~2 s of events every 60 ms: plain debounce(200) would never emit until the end.
        val out = burst(count = 34, periodMs = 60)
            .debounceWithMaxWait(timeout = 200.milliseconds, maxWait = 500.milliseconds)
            .map { it to currentTime - start }
            .toList()
        val times = out.map { it.second }
        assertEquals(listOf(500L, 1_040L, 1_580L), times.take(3))
        // Latest value at the deadline is emitted, not the first one of the burst.
        assertEquals(8, out.first().first)
        // The final value is always delivered.
        assertEquals(33, out.last().first)
    }

    @Test
    fun withMaxWait_emptyUpstream_emitsNothing() = runTest {
        val out = flow<Int> { }
            .debounceWithMaxWait(timeout = 200.milliseconds, maxWait = 500.milliseconds)
            .toList()
        assertEquals(emptyList<Int>(), out)
    }
}
