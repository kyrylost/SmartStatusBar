package dev.kyrylost.smartstatusbar.sample.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.kyrylost.smartstatusbar.SmartStatusBarConfig
import dev.kyrylost.smartstatusbar.SmartStatusBarOverride
import dev.kyrylost.smartstatusbar.UpdateStrategy
import dev.kyrylost.smartstatusbar.sample.ui.SceneHeader

/** The strategy of this screen's override. */
val LiveStrategy: UpdateStrategy = UpdateStrategy.Periodic()

/**
 * The one screen that does not rely on the app-wide effect. The SurfaceView draws on its own
 * thread, so the window never redraws and Debounce would not notice; while this screen is shown,
 * its override polls instead, and the app-wide effect resumes when the screen leaves.
 *
 * [libraryEnabled] only mirrors the switch of the sample's control panel.
 */
@Composable
fun LiveScreen(libraryEnabled: Boolean) {
    if (libraryEnabled) {
        SmartStatusBarOverride(config = SmartStatusBarConfig(strategy = LiveStrategy))
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = ::LiveSurfaceView, modifier = Modifier.fillMaxSize())

        SceneHeader(title = "Live", subtitle = "Drawn on its own thread") {
            LiveBadge(Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(Color(0xFFFF3B30), CircleShape),
        )
        Text(text = "LIVE", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
