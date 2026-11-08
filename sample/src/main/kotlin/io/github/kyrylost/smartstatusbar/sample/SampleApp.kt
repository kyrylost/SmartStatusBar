package io.github.kyrylost.smartstatusbar.sample

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.kyrylost.smartstatusbar.SmartStatusBarConfig
import io.github.kyrylost.smartstatusbar.SmartStatusBarEffect
import io.github.kyrylost.smartstatusbar.UpdateStrategy
import io.github.kyrylost.smartstatusbar.sample.feed.FeedScreen
import io.github.kyrylost.smartstatusbar.sample.live.LiveScreen
import io.github.kyrylost.smartstatusbar.sample.photo.PhotoScreen
import io.github.kyrylost.smartstatusbar.sample.sky.SkyScreen
import io.github.kyrylost.smartstatusbar.sample.ui.ControlPanel
import io.github.kyrylost.smartstatusbar.sample.ui.LocalPanelClearance

private const val TabFadeMillis = 300

/** The strategy of the app-wide effect: the default one. */
val AppWideStrategy: UpdateStrategy = UpdateStrategy.Debounce()

@Composable
fun SampleApp() {
    var selected by rememberSaveable { mutableStateOf(SampleTab.Sky) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var debug by rememberSaveable { mutableStateOf(false) }
    var panelHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // The one effect of the app: screens do not know about it. The switch only exists to compare
    // with what the icons look like without the library.
    if (enabled) SmartStatusBarEffect(SmartStatusBarConfig(strategy = AppWideStrategy))

    // Every screen is a full-bleed scene under the status bar; the panel floats over it.
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPanelClearance provides panelHeight) {
            Screens(selected, libraryEnabled = enabled)
        }
        ControlPanel(
            tab = selected,
            onTabChange = { selected = it },
            libraryEnabled = enabled,
            onLibraryEnabledChange = { enabled = it },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            debug = debug,
            onDebugChange = { debug = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { panelHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun Screens(selected: SampleTab, libraryEnabled: Boolean) {
    AnimatedContent(
        targetState = selected,
        transitionSpec = { fadeIn(tween(TabFadeMillis)) togetherWith fadeOut(tween(TabFadeMillis)) },
        label = "tab",
    ) { tab ->
        when (tab) {
            SampleTab.Sky -> SkyScreen()
            SampleTab.Feed -> FeedScreen()
            SampleTab.Photo -> PhotoScreen(
                libraryEnabled = libraryEnabled,
                shown = transition.targetState == EnterExitState.Visible && !transition.isRunning,
            )
            SampleTab.Live -> LiveScreen(libraryEnabled = libraryEnabled)
        }
    }
}
