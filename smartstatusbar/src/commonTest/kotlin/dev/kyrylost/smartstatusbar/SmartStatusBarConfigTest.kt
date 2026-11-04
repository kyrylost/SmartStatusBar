package dev.kyrylost.smartstatusbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SmartStatusBarConfigTest {

    // ---------- defaults ----------

    @Test
    fun defaults_areSane() {
        val c = SmartStatusBarConfig()
        assertEquals(UpdateStrategy.Debounce(), c.strategy)
        assertEquals(0.2f, c.sideFraction)
        assertEquals(0.5f, c.lightThreshold)
        assertEquals(0.05f, c.hysteresis)
    }

    // ---------- validation ----------

    @Test
    fun sideFraction_mustBeInHalfOpenRange() {
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(sideFraction = 0f) }
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(sideFraction = -0.1f) }
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(sideFraction = 0.51f) }
        SmartStatusBarConfig(sideFraction = 0.5f)
        SmartStatusBarConfig(sideFraction = 0.01f)
    }

    @Test
    fun lightThreshold_mustBeBetween0And1() {
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(lightThreshold = -0.01f) }
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(lightThreshold = 1.01f) }
        SmartStatusBarConfig(lightThreshold = 0f)
        SmartStatusBarConfig(lightThreshold = 1f)
    }

    @Test
    fun hysteresis_mustBeBetween0AndHalf() {
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(hysteresis = -0.01f) }
        assertFailsWith<IllegalArgumentException> { SmartStatusBarConfig(hysteresis = 0.51f) }
        SmartStatusBarConfig(hysteresis = 0f)
        SmartStatusBarConfig(hysteresis = 0.5f)
    }

    // ---------- equals / hashCode / toString ----------

    @Test
    fun equalConfigs_areEqualAndHaveSameHashCode() {
        val a = SmartStatusBarConfig()
        val b = SmartStatusBarConfig()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun eachField_participatesInEquals() {
        val base = SmartStatusBarConfig()
        assertNotEquals(base, SmartStatusBarConfig(strategy = UpdateStrategy.Manual))
        assertNotEquals(base, SmartStatusBarConfig(sideFraction = 0.3f))
        assertNotEquals(base, SmartStatusBarConfig(lightThreshold = 0.6f))
        assertNotEquals(base, SmartStatusBarConfig(hysteresis = 0.1f))
    }

    @Test
    fun equals_handlesNullAndOtherTypes() {
        val c = SmartStatusBarConfig()
        assertNotEquals<Any?>(c, null)
        assertNotEquals<Any>(c, "config")
    }

    @Test
    fun toString_containsAllFields() {
        val s = SmartStatusBarConfig().toString()
        listOf("strategy", "sideFraction", "lightThreshold", "hysteresis")
            .forEach { assertTrue(s.contains(it), "missing $it in $s") }
    }

    // ---------- UpdateStrategy ----------

    @Test
    fun strategies_haveExpectedDefaults() {
        assertEquals(200.milliseconds, UpdateStrategy.Debounce().timeout)
        assertEquals(1.seconds, UpdateStrategy.Debounce().maxWait)
        assertEquals(250.milliseconds, UpdateStrategy.Periodic().interval)
    }

    @Test
    fun strategies_rejectNonPositiveDurations() {
        assertFailsWith<IllegalArgumentException> { UpdateStrategy.Debounce(0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { UpdateStrategy.Debounce((-1).milliseconds) }
        assertFailsWith<IllegalArgumentException> {
            UpdateStrategy.Debounce(timeout = 200.milliseconds, maxWait = 100.milliseconds)
        }
        UpdateStrategy.Debounce(timeout = 200.milliseconds, maxWait = 200.milliseconds)
        UpdateStrategy.Debounce(timeout = 200.milliseconds, maxWait = Duration.INFINITE)
        assertFailsWith<IllegalArgumentException> { UpdateStrategy.Periodic(0.milliseconds) }
        assertFailsWith<IllegalArgumentException> { UpdateStrategy.Periodic((-5).milliseconds) }
    }

    @Test
    fun strategies_haveValueEquality() {
        assertEquals(UpdateStrategy.Debounce(100.milliseconds), UpdateStrategy.Debounce(100.milliseconds))
        assertNotEquals(UpdateStrategy.Debounce(100.milliseconds), UpdateStrategy.Debounce(200.milliseconds))
        assertNotEquals(UpdateStrategy.Periodic(100.milliseconds), UpdateStrategy.Periodic(200.milliseconds))
        assertEquals(UpdateStrategy.Periodic().hashCode(), UpdateStrategy.Periodic().hashCode())
    }
}
