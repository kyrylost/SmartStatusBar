package dev.kyrylost.smartstatusbar

import android.graphics.Color
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/** [SmartStatusBar] in an Activity without Compose, on the real frame clock (see RealClock.kt). */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class ViewApiTest {

    private val fastDebounce = SmartStatusBarConfig(strategy = UpdateStrategy.Debounce(50.milliseconds, 50.milliseconds))
    private val manual = SmartStatusBarConfig(strategy = UpdateStrategy.Manual)

    private lateinit var content: FrameLayout

    private fun launch(initialDarkIcons: Boolean, background: Int): ActivityScenario<ComponentActivity> =
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.enableEdgeToEdge()
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .isAppearanceLightStatusBars = initialDarkIcons
            content = FrameLayout(activity).apply { setBackgroundColor(background) }
            activity.setContentView(content)
        }

    @Test
    fun install_followsTheContent() {
        launch(initialDarkIcons = false, background = Color.WHITE).use { scenario ->
            lateinit var handle: SmartStatusBarHandle
            scenario.onActivity { handle = SmartStatusBar.install(it, fastDebounce) }
            assertTrue(scenario.awaitDarkIcons(true))
            assertEquals(StatusBarBackground.Light, handle.background.value)

            scenario.onActivity { content.setBackgroundColor(Color.BLACK) }
            assertTrue(scenario.awaitDarkIcons(false))
            assertEquals(StatusBarBackground.Dark, handle.background.value)
        }
    }

    @Test
    fun override_takesOver_andRemovingHandlesRestores() {
        launch(initialDarkIcons = false, background = Color.WHITE).use { scenario ->
            lateinit var appWide: SmartStatusBarHandle
            lateinit var screen: SmartStatusBarHandle
            scenario.onActivity {
                // Declared after the override: it still stays paused under it.
                screen = SmartStatusBar.override(it.window, it, manual)
                appWide = SmartStatusBar.install(it, fastDebounce)
            }
            assertTrue("the Manual override must stay active", true !in scenario.observeDarkIcons(durationMillis = 1_000))

            scenario.onActivity { screen.remove() }
            assertTrue("the app-wide effect resumes", scenario.awaitDarkIcons(true))

            scenario.onActivity { appWide.remove() }
            assertTrue("the last one restores what the window had", scenario.awaitDarkIcons(false))
        }
    }

    @Test
    fun destroyedLifecycle_detaches() {
        val scenario = launch(initialDarkIcons = false, background = Color.WHITE)
        lateinit var handle: SmartStatusBarHandle
        scenario.onActivity { handle = SmartStatusBar.install(it, fastDebounce) }
        assertTrue(scenario.awaitDarkIcons(true))

        scenario.close()
        assertEquals(null, handle.background.value)
    }
}
