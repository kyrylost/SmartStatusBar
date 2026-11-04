package dev.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Android analyzes what is under the status bar. Elsewhere there is nothing to do: from iOS 17 the
// default status bar style adapts to the content underneath on its own, and desktop and web have no
// status bar.

@Composable
internal expect fun PlatformStatusBarEffect(config: SmartStatusBarConfig, isOverride: Boolean)

/** Returns the function that asks the active effect of the current window for a check. */
@Composable
internal expect fun rememberRequestUpdate(): () -> Unit

@Composable
internal expect fun PlatformDebugOverlay(modifier: Modifier)

@Composable
internal expect fun platformStatusBarBackground(): StatusBarBackground?
