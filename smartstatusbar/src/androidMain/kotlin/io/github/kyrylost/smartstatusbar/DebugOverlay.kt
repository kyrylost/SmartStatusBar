package io.github.kyrylost.smartstatusbar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OverlayHeight = 88.dp
private val OverlayPadding = 12.dp
private val StripHeight = 32.dp
private val OverlayBackground = Color(0xE6101218)
private val NoPixel = Color(0xFF3A3F4B)

@Composable
internal actual fun PlatformDebugOverlay(modifier: Modifier) {
    val state = rememberWindowState()
    DisposableEffect(state) {
        val detach = state.attachOverlay()
        onDispose(detach)
    }
    val snapshot = state.snapshot
    val textMeasurer = rememberTextMeasurer()

    Spacer(
        modifier
            .fillMaxWidth()
            .height(OverlayHeight)
            .drawBehind { drawOverlay(snapshot, textMeasurer) },
    )
}

private fun DrawScope.drawOverlay(snapshot: DebugSnapshot?, textMeasurer: TextMeasurer) {
    drawRect(OverlayBackground)
    val padding = OverlayPadding.toPx()
    val style = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    val textTop = padding + StripHeight.toPx() + 8.dp.toPx()

    if (snapshot == null) {
        drawText(textMeasurer, "Waiting for a check…", Offset(padding, padding), style)
        return
    }

    val width = CAPTURE_WIDTH
    val height = snapshot.pixels.size / width
    val stripWidth = size.width - 2 * padding
    val cellW = stripWidth / width
    val cellH = StripHeight.toPx() / height
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = snapshot.pixels[y * width + x]
            drawRect(
                color = if (pixel ushr 24 == 0) NoPixel else Color(pixel or (0xFF shl 24)),
                topLeft = Offset(padding + x * cellW, padding + y * cellH),
                size = Size(cellW + 0.5f, cellH + 0.5f),
            )
        }
    }

    // Only the outer columns are analyzed: dim the middle.
    val side = sideColumns(width, snapshot.sideFraction)
    val analyzedEnd = padding + side * cellW
    val analyzedStart = padding + (width - side) * cellW
    drawRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(analyzedEnd, padding),
        size = Size(analyzedStart - analyzedEnd, StripHeight.toPx()),
    )
    for (x in listOf(analyzedEnd, analyzedStart)) {
        drawLine(Color.White, Offset(x, padding), Offset(x, padding + StripHeight.toPx()), strokeWidth = 1.dp.toPx())
    }

    val icons = if (snapshot.lightAppearance) "dark icons" else "light icons"
    drawText(
        textMeasurer,
        "light pixels ${"%.2f".format(snapshot.ratio)} → $icons\n" +
            "request ${"%.1f".format(snapshot.blockedMillis)} ms · capture ${"%.1f".format(snapshot.captureMillis)} ms",
        Offset(padding, textTop),
        style,
    )
}
