package dev.kyrylost.smartstatusbar.sample.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.kyrylost.smartstatusbar.StatusBarBackground
import dev.kyrylost.smartstatusbar.currentStatusBarBackground

private val Ink = Color(0xFF14161C)

/**
 * The color of content drawn under the status bar: ink where the library shows dark icons, white
 * where it shows light ones, and white while it has not decided (or is switched off).
 */
@Composable
fun sceneContentColor(): Color {
    val target = if (currentStatusBarBackground() == StatusBarBackground.Light) Ink else Color.White
    return animateColorAsState(target, tween(durationMillis = 300), label = "scene content").value
}

/**
 * The big title every screen shows right under the status bar. Its color comes from
 * [currentStatusBarBackground], so it switches together with the system icons.
 */
@Composable
fun SceneHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val color = sceneContentColor()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp, start = 24.dp, end = 24.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.displayLarge, color = color, textAlign = TextAlign.Center)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = color.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        content()
    }
}
