package io.github.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** The state of the window of the current composition; a detached one in previews, which have no window. */
@Composable
internal fun rememberWindowState(): WindowState {
    val window = LocalView.current.findWindow()
    return remember(window) { window?.smartStatusBarState() ?: WindowState() }
}

@Composable
internal actual fun rememberRequestUpdate(): () -> Unit {
    val state = rememberWindowState()
    return remember(state) { state::requestUpdate }
}

@Composable
internal actual fun platformStatusBarBackground(): StatusBarBackground? =
    rememberWindowState().background.collectAsState().value
