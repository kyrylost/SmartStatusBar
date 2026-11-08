package io.github.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable

/**
 * Side-effect composable that looks at what is drawn under the status bar and switches the system
 * status bar icons between light and dark for contrast.
 *
 * Place one at the Activity / NavHost level for app-wide behavior, or on the one screen that needs it.
 * A screen that needs a different [SmartStatusBarConfig] under an app-wide effect uses
 * [SmartStatusBarOverride]. With several effects in one window the one attached last is active and the
 * others pause until it leaves; the one that resumes then checks the screen right away. When the last
 * one leaves, the icon appearance from before the first one is restored.
 *
 * Checks run only while the lifecycle owner of the composition is at least STARTED.
 *
 * The host must call `enableEdgeToEdge()` itself; this library only changes
 * `isAppearanceLightStatusBars`. Works on API 26+ (PixelCopy); on older versions it does nothing.
 * Content that cannot be captured (DRM video on devices that block reading it) is not analyzed, and
 * the previous icon color is kept.
 *
 * On other platforms it does nothing: from iOS 17 the default status bar style adapts to the content
 * underneath on its own, and desktop and web have no status bar.
 *
 * @param config Strategy, analyzed zones, threshold and hysteresis.
 */
@Composable
public fun SmartStatusBarEffect(
    config: SmartStatusBarConfig = SmartStatusBarConfig(),
) {
    PlatformStatusBarEffect(config, isOverride = false)
}

/**
 * Takes over from [SmartStatusBarEffect] with a different [config] while it is in the composition,
 * for example a video screen that needs [UpdateStrategy.Periodic] under an app-wide Debounce. When it
 * leaves, the effect it overrode resumes and checks the screen right away, whatever its strategy.
 *
 * An override wins over every [SmartStatusBarEffect] of the window, wherever either is declared and
 * whichever was attached first, so switching the app-wide effect off and on does not take over from
 * the screen. Among several overrides the one attached last is active.
 *
 * Does nothing outside Android, like [SmartStatusBarEffect].
 *
 * @param config Strategy, analyzed zones, threshold and hysteresis for this screen.
 */
@Composable
public fun SmartStatusBarOverride(config: SmartStatusBarConfig) {
    PlatformStatusBarEffect(config, isOverride = true)
}
