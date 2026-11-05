package dev.kyrylost.smartstatusbar

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/** Timing-sensitive checks that run on the real frame clock (see RealClock.kt). */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class RealClockEffectTest {

    @Test
    fun debounce_doesNotFlipIconsDuringEnterTransition() {
        launchWithContent(initialDarkIcons = true) {
            // Like a crossfade from a dark screen: the first frames are black, the settled screen is white.
            val color = remember { Animatable(Color.Black) }
            LaunchedEffect(Unit) { color.animateTo(Color.White, tween(durationMillis = 600)) }
            Box(Modifier.fillMaxSize().background(color.value))
            SmartStatusBarEffect()
        }.use { scenario ->
            val seen = scenario.observeDarkIcons(durationMillis = 2_000)
            assertFalse("icons must not follow the black frames of the transition", false in seen)
            assertTrue(seen.isNotEmpty())
        }
    }

    @Test
    fun stoppedHost_pausesChecksUntilItIsStartedAgain() {
        var color by mutableStateOf(Color.White)
        launchWithContent(initialDarkIcons = false) {
            Box(Modifier.fillMaxSize().background(color))
            SmartStatusBarEffect(config = SmartStatusBarConfig(strategy = UpdateStrategy.Debounce(50.milliseconds, 50.milliseconds)))
        }.use { scenario ->
            assertTrue(scenario.awaitDarkIcons(true))

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { color = Color.Black }
            assertFalse("no checks while stopped", scenario.awaitDarkIcons(false, timeoutMillis = 1_000))

            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue("checks resume with the host", scenario.awaitDarkIcons(false))
        }
    }
}
