package dev.kyrylost.smartstatusbar

import android.os.Build
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
internal actual fun PlatformStatusBarEffect(config: SmartStatusBarConfig, isOverride: Boolean) {
    // PixelCopy of a window needs API 26; below that the library leaves the icons alone.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || LocalInspectionMode.current) return

    val view = LocalView.current
    val window = view.findWindow() ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val statusBars = WindowInsets.statusBars
    val insetsController = remember(window, view) { WindowCompat.getInsetsController(window, view) }

    val state = remember(window) { window.smartStatusBarState() }
    val effect = remember { WindowState.Effect(isOverride) }

    DisposableEffect(state, insetsController) {
        state.attach(effect, insetsController.isAppearanceLightStatusBars)
        onDispose {
            state.detach(effect)?.let { insetsController.isAppearanceLightStatusBars = it }
        }
    }

    // Another effect is active in this window (an override, or an effect attached later): pause until it leaves.
    val active by state.active.collectAsState()
    if (active !== effect) return

    LaunchedEffect(window, view, config, density, lifecycleOwner) {
        val host = CheckHost(
            window = window,
            view = view,
            density = density,
            statusBarTop = { statusBars.getTop(density) },
            // The first value is the current height: the initial check comes from the strategy itself.
            insetsChanges = snapshotFlow { statusBars.getTop(density) }.drop(1).filter { it > 0 }.map { },
            awaitFrame = { withFrameNanos { } },
        )
        runChecks(host, state, config, lifecycleOwner.lifecycle)
    }
}
