package dev.kyrylost.smartstatusbar

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WindowStateTest {

    private val state = WindowState()

    private fun effect() = WindowState.Effect(isOverride = false)

    private fun override() = WindowState.Effect(isOverride = true)

    private suspend fun requested(): Boolean = withTimeoutOrNull(100) { state.requests.first() } != null

    @Test
    fun singleEffect_restoresOriginalWhenItLeaves() {
        val a = effect()
        state.attach(a, appearance = false)
        assertSame(a, state.active.value)

        assertEquals(false, state.detach(a))
        assertNull(state.active.value)
    }

    @Test
    fun navigation_betweenScreensWithTheirOwnEffect_restoresWhatWasThereBeforeTheFirst() = runTest {
        // Screen A, then B attaches during the transition, then A leaves.
        val a = effect()
        val b = effect()
        state.attach(a, appearance = false)
        state.attach(b, appearance = true)
        assertSame(b, state.active.value)

        assertNull("A was paused: leaving must not touch the icons", state.detach(a))
        assertSame(b, state.active.value)
        assertEquals(false, requested())

        assertEquals(false, state.detach(b))
    }

    @Test
    fun override_winsOverEffectAttachedAfterIt() {
        val screen = override()
        val appWide = effect()
        state.attach(screen, appearance = false)
        state.attach(appWide, appearance = false)

        assertSame(screen, state.active.value)
    }

    @Test
    fun appWideEffectSwitchedOffAndOn_staysUnderTheOverride() {
        val appWide = effect()
        val screen = override()
        state.attach(appWide, appearance = false)
        state.attach(screen, appearance = true)

        assertNull(state.detach(appWide))
        val appWideAgain = effect()
        state.attach(appWideAgain, appearance = true)
        assertSame(screen, state.active.value)

        assertNull(state.detach(screen))
        assertSame(appWideAgain, state.active.value)
        assertEquals("what the window had before any effect", false, state.detach(appWideAgain))
    }

    @Test
    fun nestedOverrides_latestIsActive_andEarlierResumesWhenItLeaves() {
        val appWide = effect()
        val scenario1 = override()
        val scenario2 = override()
        state.attach(appWide, appearance = false)
        state.attach(scenario1, appearance = false)
        state.attach(scenario2, appearance = false)
        assertSame(scenario2, state.active.value)

        assertNull(state.detach(scenario2))
        assertSame(scenario1, state.active.value)
        assertNull(state.detach(scenario1))
        assertSame(appWide, state.active.value)
    }

    @Test
    fun activeLeaving_makesTheResumedEffectCheck() = runTest {
        val appWide = effect()
        val screen = override()
        state.attach(appWide, appearance = false)
        state.attach(screen, appearance = false)

        state.detach(screen)

        assertNotNull(withTimeoutOrNull(100) { state.requests.first() })
    }

    @Test
    fun originalIsTakenAgainAfterAllEffectsLeft() {
        val a = effect()
        state.attach(a, appearance = false)
        state.detach(a)

        val b = effect()
        state.attach(b, appearance = true)
        assertEquals(true, state.detach(b))
    }

    @Test
    fun detachingUnknownEffect_isIgnored() {
        state.attach(effect(), appearance = true)

        assertNull(state.detach(effect()))
        assertNull(WindowState().detach(effect()))
    }

    @Test
    fun background_followsDecisions_andIsClearedWhenTheLastEffectLeaves() {
        val appWide = effect()
        val screen = override()
        assertNull(state.background.value)

        state.attach(appWide, appearance = false)
        state.recordDecision(lightAppearance = true)
        assertEquals(StatusBarBackground.Light, state.background.value)

        state.attach(screen, appearance = true)
        state.recordDecision(lightAppearance = false)
        assertEquals(StatusBarBackground.Dark, state.background.value)

        state.detach(screen)
        assertEquals("kept while an effect remains", StatusBarBackground.Dark, state.background.value)
        state.detach(appWide)
        assertNull(state.background.value)
    }
}
