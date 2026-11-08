package io.github.kyrylost.smartstatusbar

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario

/*
 * Helpers for tests that need the real frame clock and real time.
 *
 * Compose test rules replace both with a virtual clock (delays in effects complete instantly and
 * animations advance in virtual time), so these tests use a plain [ActivityScenario] instead.
 *
 * Naming: `isAppearanceLightStatusBars == true` means DARK icons (light background).
 */

internal fun launchWithContent(
    initialDarkIcons: Boolean,
    content: @Composable () -> Unit,
): ActivityScenario<ComponentActivity> =
    ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
        activity.enableEdgeToEdge()
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = initialDarkIcons
        activity.setContent(content = content)
    }

internal fun ActivityScenario<ComponentActivity>.awaitDarkIcons(expected: Boolean, timeoutMillis: Long = 5_000): Boolean {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (SystemClock.uptimeMillis() < deadline) {
        var value = !expected
        onActivity { value = WindowCompat.getInsetsController(it.window, it.window.decorView).isAppearanceLightStatusBars }
        if (value == expected) return true
        SystemClock.sleep(50)
    }
    return false
}

/** Samples `isAppearanceLightStatusBars` every 20 ms for [durationMillis] and returns what it saw. */
internal fun ActivityScenario<ComponentActivity>.observeDarkIcons(durationMillis: Long): List<Boolean> {
    val seen = mutableListOf<Boolean>()
    val deadline = SystemClock.uptimeMillis() + durationMillis
    while (SystemClock.uptimeMillis() < deadline) {
        onActivity { seen += WindowCompat.getInsetsController(it.window, it.window.decorView).isAppearanceLightStatusBars }
        SystemClock.sleep(20)
    }
    return seen
}
