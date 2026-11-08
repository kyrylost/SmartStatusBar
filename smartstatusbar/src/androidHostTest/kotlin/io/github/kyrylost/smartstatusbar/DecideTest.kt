package io.github.kyrylost.smartstatusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecideTest {

    private val h = 0.05f

    // ---------- hysteresis ----------

    @Test
    fun currentTrue_insideBand_staysTrue() {
        assertTrue(decide(0.48f, current = true, hysteresis = h))
        assertTrue(decide(0.46f, current = true, hysteresis = h))
    }

    @Test
    fun currentTrue_belowBand_switchesToFalse() {
        assertFalse(decide(0.44f, current = true, hysteresis = h))
        assertFalse(decide(0f, current = true, hysteresis = h))
    }

    @Test
    fun currentFalse_insideBand_staysFalse() {
        assertFalse(decide(0.52f, current = false, hysteresis = h))
        assertFalse(decide(0.54f, current = false, hysteresis = h))
    }

    @Test
    fun currentFalse_aboveBand_switchesToTrue() {
        assertTrue(decide(0.56f, current = false, hysteresis = h))
        assertTrue(decide(1f, current = false, hysteresis = h))
    }

    @Test
    fun currentTrue_aboveHalf_staysTrue() {
        assertTrue(decide(0.9f, current = true, hysteresis = h))
    }

    @Test
    fun currentFalse_belowHalf_staysFalse() {
        assertFalse(decide(0.1f, current = false, hysteresis = h))
    }

    @Test
    fun zeroHysteresis_switchesImmediately() {
        assertFalse(decide(0.49f, current = true, hysteresis = 0f))
        assertTrue(decide(0.51f, current = false, hysteresis = 0f))
    }

    @Test
    fun zeroHysteresis_exactlyHalf_keepsCurrent() {
        assertTrue(decide(0.5f, current = true, hysteresis = 0f))
        assertFalse(decide(0.5f, current = false, hysteresis = 0f))
    }

    // ---------- sequences ----------

    @Test
    fun noisyRatioAroundBoundary_doesNotFlicker() {
        val noisy = listOf(0.52f, 0.48f, 0.53f, 0.47f, 0.51f, 0.49f)
        var current = true
        val history = noisy.map { r ->
            decide(r, current, h).also { current = it }
        }
        // every sample stays inside the band
        assertEquals(List(noisy.size) { true }, history)
    }

    @Test
    fun clearTransition_switchesOnce() {
        val ratios = listOf(0.9f, 0.8f, 0.3f, 0.2f, 0.1f)
        var current = false
        val history = ratios.map { r -> decide(r, current, h).also { current = it } }
        assertEquals(listOf(true, true, false, false, false), history)
    }
}
