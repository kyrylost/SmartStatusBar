package io.github.kyrylost.smartstatusbar

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

// Keeps the analyzed strip off the status bar's rounded corners and its top and bottom edges.
private val HORIZONTAL_INSET = 4.dp
private val VERTICAL_INSET = 4.dp

/** The part of the status bar (window coordinates) to analyze, or null if nothing is left of it. */
internal fun stripRect(statusBarHeightPx: Int, windowWidthPx: Int, density: Density): IntRect? {
    if (statusBarHeightPx <= 0 || windowWidthPx <= 0) return null
    val h = with(density) { HORIZONTAL_INSET.roundToPx() }
    val v = with(density) { VERTICAL_INSET.roundToPx() }
    val rect = IntRect(h, v, windowWidthPx - h, statusBarHeightPx - v)
    return rect.takeUnless { it.isEmpty }
}
