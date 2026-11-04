package dev.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shows what [SmartStatusBarEffect] sees: the captured strip, enlarged, with the zones that are
 * not analyzed dimmed, the share of light pixels, the decision, and how long the capture took
 * (time spent in `PixelCopy.request` on the main thread, and time until the pixels were ready).
 *
 * Meant for tuning [SmartStatusBarConfig] and for checking what a screen looks like to the
 * library. The overlay follows the effect that is active in the window and updates when the
 * captured picture or the decision changes, so the timings are those of the last such change.
 * Nothing is captured for it while it is not in the composition.
 *
 * Do not place it under the status bar: what it draws there would be analyzed too.
 * Outside Android it draws nothing.
 */
@Composable
public fun SmartStatusBarDebugOverlay(modifier: Modifier = Modifier) {
    PlatformDebugOverlay(modifier)
}
