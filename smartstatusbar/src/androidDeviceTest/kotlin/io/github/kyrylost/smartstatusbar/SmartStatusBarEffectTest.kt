package io.github.kyrylost.smartstatusbar

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/**
 * Must run on an emulator or a real device (PixelCopy needs a real window surface).
 *
 * Naming: `isAppearanceLightStatusBars == true` means DARK icons (light background).
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class SmartStatusBarEffectTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val fastDebounce = SmartStatusBarConfig(strategy = UpdateStrategy.Debounce(50.milliseconds, 50.milliseconds))

    // ---------- helpers ----------

    private fun darkIconsEnabled(): Boolean {
        var value = false
        rule.activityRule.scenario.onActivity { a ->
            value = WindowCompat.getInsetsController(a.window, a.window.decorView).isAppearanceLightStatusBars
        }
        return value
    }

    /** Puts the window into a known state so a test cannot pass by accident. */
    private fun prepare(initialDarkIcons: Boolean) {
        rule.activityRule.scenario.onActivity { a ->
            a.enableEdgeToEdge()
            WindowCompat.getInsetsController(a.window, a.window.decorView).isAppearanceLightStatusBars =
                initialDarkIcons
        }
    }

    /** A controller for the Activity's window, usable before the content is set. */
    private fun controller(): SmartStatusBarController {
        lateinit var controller: SmartStatusBarController
        rule.activityRule.scenario.onActivity { a -> controller = SmartStatusBarController(a.window.smartStatusBarState()::requestUpdate) }
        return controller
    }

    private fun awaitDarkIcons(expected: Boolean) {
        rule.waitUntil(timeoutMillis = 5_000) { darkIconsEnabled() == expected }
    }

    // ---------- tests ----------

    @Test
    fun whiteBackground_enablesDarkIcons() {
        prepare(initialDarkIcons = false)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            SmartStatusBarEffect(config = fastDebounce)
        }

        awaitDarkIcons(true)
    }

    @Test
    fun blackBackground_enablesLightIcons() {
        prepare(initialDarkIcons = true)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.Black))
            SmartStatusBarEffect(config = fastDebounce)
        }

        awaitDarkIcons(false)
    }

    @Test
    fun backgroundChange_atRuntime_switchesIcons() {
        prepare(initialDarkIcons = false)
        var color by mutableStateOf(Color.White)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(color))
            SmartStatusBarEffect(config = fastDebounce)
        }
        awaitDarkIcons(true)

        color = Color.Black
        awaitDarkIcons(false)

        color = Color.White
        awaitDarkIcons(true)
    }

    @Test
    fun onlySideZonesMatter_blackEdgesWhiteCenter_givesLightIcons() {
        prepare(initialDarkIcons = true)

        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.2f).fillMaxSize().background(Color.Black))
                Box(Modifier.weight(0.6f).fillMaxSize().background(Color.White))
                Box(Modifier.weight(0.2f).fillMaxSize().background(Color.Black))
            }
            SmartStatusBarEffect(
                config = SmartStatusBarConfig(strategy = fastDebounce.strategy, sideFraction = 0.2f),
            )
        }

        awaitDarkIcons(false)
    }

    @Test
    fun manualStrategy_doesNothingUntilRequested() {
        prepare(initialDarkIcons = false)
        val controller = controller()

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            SmartStatusBarEffect(
                config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual),
            )
        }

        rule.waitForIdle()
        Thread.sleep(1_000)
        assertFalse("Manual must not update on its own", darkIconsEnabled())

        controller.requestUpdate()
        awaitDarkIcons(true)
    }

    @Test
    fun requestUpdate_beforeEffectStarts_isNotLost() {
        prepare(initialDarkIcons = false)
        val controller = controller()
        controller.requestUpdate() // before setContent

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            SmartStatusBarEffect(
                config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual),
            )
        }

        awaitDarkIcons(true)
    }

    @Test
    fun requestUpdate_rightAfterStateChange_seesNewContent() {
        prepare(initialDarkIcons = false)
        val controller = controller()
        var color by mutableStateOf(Color.White)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(color))
            SmartStatusBarEffect(
                config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual),
            )
        }
        controller.requestUpdate()
        awaitDarkIcons(true)

        // Like a click handler or an image loader callback: change state, then request right away,
        // before the new content has been drawn.
        rule.runOnUiThread {
            color = Color.Black
            controller.requestUpdate()
        }
        awaitDarkIcons(false)
    }

    @Test
    fun leavingComposition_restoresInitialIconAppearance() {
        prepare(initialDarkIcons = false)
        var showEffect by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            if (showEffect) SmartStatusBarEffect(config = fastDebounce)
        }
        awaitDarkIcons(true)

        showEffect = false
        awaitDarkIcons(false) // restored to the value that was set before the effect appeared
    }

    @Test
    fun override_takesOver_andAppWideEffectChecksWhenItResumes() {
        prepare(initialDarkIcons = false)
        var showOverride by mutableStateOf(false)
        var color by mutableStateOf(Color.White)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(color))
            // The app-wide effect never checks on its own, so dark icons can only come from the override.
            SmartStatusBarEffect(config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual))
            if (showOverride) SmartStatusBarOverride(config = fastDebounce)
        }
        rule.waitForIdle()
        assertFalse(darkIconsEnabled())

        showOverride = true
        awaitDarkIcons(true)

        // The next screen is dark: the resumed Manual effect checks it without a request.
        rule.runOnUiThread {
            color = Color.Black
            showOverride = false
        }
        awaitDarkIcons(false)
    }

    @Test
    fun currentStatusBarBackground_followsTheDecision() {
        prepare(initialDarkIcons = false)
        var color by mutableStateOf(Color.White)
        var seen: StatusBarBackground? = null

        rule.setContent {
            Box(Modifier.fillMaxSize().background(color))
            SmartStatusBarEffect(config = fastDebounce)
            seen = currentStatusBarBackground()
        }
        rule.waitUntil(timeoutMillis = 5_000) { seen == StatusBarBackground.Light }

        color = Color.Black
        rule.waitUntil(timeoutMillis = 5_000) { seen == StatusBarBackground.Dark }
    }

    @Test
    fun override_winsOverAppWideEffectDeclaredAfterIt() {
        prepare(initialDarkIcons = false)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            // The override never checks on its own: dark icons would mean the app-wide effect took over.
            SmartStatusBarOverride(config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual))
            SmartStatusBarEffect(config = fastDebounce)
        }

        rule.waitForIdle()
        Thread.sleep(1_000)
        assertFalse("the override must stay active", darkIconsEnabled())
    }

    @Test
    fun appWideEffectSwitchedOffAndOn_doesNotTakeOverFromOverride() {
        prepare(initialDarkIcons = false)
        var appWide by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            if (appWide) SmartStatusBarEffect(config = fastDebounce)
            SmartStatusBarOverride(config = SmartStatusBarConfig(strategy = UpdateStrategy.Manual))
        }
        rule.waitForIdle()

        appWide = false
        rule.waitForIdle()
        appWide = true

        rule.waitForIdle()
        Thread.sleep(1_000)
        assertFalse("the re-attached app-wide effect must stay paused under the override", darkIconsEnabled())
    }

    @Test
    fun hysteresis_keepsWhatTheWindowShows_untilTheShareLeavesTheBand() {
        prepare(initialDarkIcons = false)
        // Light edges on one side, dark on the other: half the analyzed pixels are light.
        var content by mutableStateOf(listOf(Color.White, Color.White))
        // Switches only below 0.05 or above 0.95.
        val config = SmartStatusBarConfig(strategy = fastDebounce.strategy, hysteresis = 0.45f)

        rule.setContent {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxSize().background(content[0]))
                Box(Modifier.weight(1f).fillMaxSize().background(content[1]))
            }
            SmartStatusBarEffect(config = config)
        }
        awaitDarkIcons(true)

        content = listOf(Color.White, Color.Black)
        rule.waitForIdle()
        Thread.sleep(1_000)
        assertTrue("a 50% share is inside the band", darkIconsEnabled())

        content = listOf(Color.Black, Color.Black)
        awaitDarkIcons(false)

        content = listOf(Color.White, Color.Black)
        rule.waitForIdle()
        Thread.sleep(1_000)
        assertFalse("a 50% share is inside the band", darkIconsEnabled())
    }
}
