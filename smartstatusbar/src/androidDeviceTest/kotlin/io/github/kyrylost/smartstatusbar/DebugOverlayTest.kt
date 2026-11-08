package io.github.kyrylost.smartstatusbar

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class DebugOverlayTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val fastDebounce = SmartStatusBarConfig(strategy = UpdateStrategy.Debounce(50.milliseconds, 50.milliseconds))

    @Before
    fun enableEdgeToEdge() {
        rule.activityRule.scenario.onActivity { it.enableEdgeToEdge() }
    }

    private fun snapshot(): DebugSnapshot? {
        var value: DebugSnapshot? = null
        rule.activityRule.scenario.onActivity { value = it.window.smartStatusBarState().snapshot }
        return value
    }

    @Test
    fun overlay_receivesTheCaptureAndTheDecision() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                SmartStatusBarDebugOverlay(Modifier.align(Alignment.BottomStart))
            }
            SmartStatusBarEffect(config = fastDebounce)
        }

        rule.waitUntil(timeoutMillis = 5_000) { snapshot() != null }

        val snapshot = snapshot()!!
        assertEquals(1f, snapshot.ratio, 0.001f)
        assertTrue(snapshot.lightAppearance)
        assertEquals(CAPTURE_WIDTH * CAPTURE_HEIGHT, snapshot.pixels.size)
        assertTrue(snapshot.captureMillis >= snapshot.blockedMillis)
    }

    @Test
    fun withoutOverlay_nothingIsPublished() {
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White))
            SmartStatusBarEffect(config = fastDebounce)
        }

        rule.waitUntil(timeoutMillis = 5_000) { darkIcons() }
        assertNull(snapshot())
    }

    @Test
    fun leavingTheComposition_clearsTheSnapshot() {
        var showOverlay by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                if (showOverlay) SmartStatusBarDebugOverlay(Modifier.align(Alignment.BottomStart))
            }
            SmartStatusBarEffect(config = fastDebounce)
        }
        rule.waitUntil(timeoutMillis = 5_000) { snapshot() != null }
        assertNotNull(snapshot())

        showOverlay = false
        rule.waitUntil(timeoutMillis = 5_000) { snapshot() == null }
    }

    private fun darkIcons(): Boolean {
        var value = false
        rule.activityRule.scenario.onActivity {
            value = androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                .isAppearanceLightStatusBars
        }
        return value
    }
}
