package io.github.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable

/** What a check found under the status bar: mostly light or mostly dark pixels. */
public enum class StatusBarBackground {
    /** Dark system icons are shown. */
    Light,

    /** Light system icons are shown. */
    Dark,
}

/**
 * What the active [SmartStatusBarEffect] of the current window last found under the status bar, so
 * that content of your own drawn there can match the system icons: for example the icons of a
 * transparent toolbar over a photo, dark on [StatusBarBackground.Light] and light on
 * [StatusBarBackground.Dark]. It follows the same decision as the system icons, hysteresis included,
 * and recomposes when it changes.
 *
 * Null before the first check in the window, after the last effect has left, and outside Android
 * (on iOS the system adapts the status bar itself and does not tell the app).
 */
@Composable
public fun currentStatusBarBackground(): StatusBarBackground? = platformStatusBarBackground()
