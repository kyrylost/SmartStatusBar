package dev.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Triggers status bar icon color checks by hand, for [UpdateStrategy.Manual] or to speed up any other
 * strategy.
 *
 * A controller addresses the window it was created in, not a particular [SmartStatusBarEffect]: the
 * request goes to whichever effect is active there. It can be created anywhere in the screen,
 * for example next to an image loader. A request made while no effect is active is kept and handled
 * as soon as one becomes active.
 *
 * A `Dialog` or a bottom sheet has a window of its own: a controller created inside one addresses
 * that window, not the Activity's. Outside Android requests are ignored.
 */
public class SmartStatusBarController internal constructor(private val request: () -> Unit) {

    /**
     * Request a status bar icon color re-check.
     *
     * Safe to call right after changing what is on screen (for example from a click handler or an
     * image loader callback): the check waits until the new content has been rendered.
     */
    public fun requestUpdate() {
        request()
    }
}

/**
 * Creates and remembers a [SmartStatusBarController] for the current window.
 */
@Composable
public fun rememberSmartStatusBarController(): SmartStatusBarController {
    val request = rememberRequestUpdate()
    return remember(request) { SmartStatusBarController(request) }
}
