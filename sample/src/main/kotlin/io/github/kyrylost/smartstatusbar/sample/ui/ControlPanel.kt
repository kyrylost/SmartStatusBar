package io.github.kyrylost.smartstatusbar.sample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kyrylost.smartstatusbar.SmartStatusBarDebugOverlay
import io.github.kyrylost.smartstatusbar.UpdateStrategy
import io.github.kyrylost.smartstatusbar.sample.R
import io.github.kyrylost.smartstatusbar.sample.SampleTab

/**
 * Height of the panel from the bottom of the screen, navigation bar included: what screens leave free
 * at the bottom so that their content can end above it. Measured, so it follows the panel's state.
 */
val LocalPanelClearance = compositionLocalOf { 0.dp }

/**
 * Floating panel over the scenes: the tabs, the strategy that checks the current screen, the switch
 * to compare against the library being off, and, on demand, what the screen shows and what the
 * library captures.
 */
@Composable
fun ControlPanel(
    tab: SampleTab,
    onTabChange: (SampleTab) -> Unit,
    libraryEnabled: Boolean,
    onLibraryEnabledChange: (Boolean) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    debug: Boolean,
    onDebugChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        // A translucent color is not one of the scheme's, so the content color would default to black.
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Tabs(selected = tab, onSelect = onTabChange)

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = tab.title, style = MaterialTheme.typography.titleMedium)
                    StrategyLine(tab, libraryEnabled)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(checked = libraryEnabled, onCheckedChange = onLibraryEnabledChange)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp),
                ) {
                    Text(
                        text = tab.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = tab.code,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    FilterChip(selected = debug, onClick = { onDebugChange(!debug) }, label = { Text("Debug overlay") })
                    if (debug) {
                        SmartStatusBarDebugOverlay(Modifier.clip(RoundedCornerShape(12.dp)))
                    }
                }
            }

            val rotation by animateFloatAsState(if (expanded) 0f else 180f, label = "chevron")
            TextButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.padding(top = 4.dp)) {
                Text(if (expanded) "Hide details" else "Show details")
                Spacer(Modifier.width(4.dp))
                Icon(painterResource(R.drawable.ic_expand), contentDescription = null, modifier = Modifier.size(18.dp).rotate(rotation))
            }
        }
    }
}

/** The tabs as a segmented control: icon and label, the selected one filled. */
@Composable
private fun Tabs(selected: SampleTab, onSelect: (SampleTab) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        SampleTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = 8.dp),
            ) {
                Icon(painterResource(tab.icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(2.dp))
                Text(text = tab.label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
            }
        }
    }
}

/** Which strategy checks this screen, and whether it is the app-wide effect or the screen's override. */
@Composable
private fun StrategyLine(tab: SampleTab, libraryEnabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (libraryEnabled) tab.strategy.describe() else "off",
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            text = if (libraryEnabled) tab.strategyNote else "the system icons stay as the theme set them",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun UpdateStrategy.describe(): String = when (this) {
    is UpdateStrategy.Debounce -> "Debounce $timeout · max $maxWait"
    is UpdateStrategy.Periodic -> "Periodic $interval"
    UpdateStrategy.Manual -> "Manual"
}

@Preview
@Composable
private fun ControlPanelPreview() {
    SampleTheme {
        ControlPanel(
            tab = SampleTab.Live,
            onTabChange = {},
            libraryEnabled = true,
            onLibraryEnabledChange = {},
            expanded = true,
            onExpandedChange = {},
            debug = false,
            onDebugChange = {},
        )
    }
}
