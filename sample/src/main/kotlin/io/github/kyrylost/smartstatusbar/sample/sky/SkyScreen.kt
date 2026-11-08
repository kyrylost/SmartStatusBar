package io.github.kyrylost.smartstatusbar.sample.sky

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.kyrylost.smartstatusbar.sample.ui.SampleTheme
import io.github.kyrylost.smartstatusbar.sample.ui.SceneHeader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val CYCLE_MILLIS = 16_000

@Composable
fun SkyScreen() {
    // 0 = midday, 0.5 = midnight, 1 = midday again.
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            phase.animateTo(1f, tween(CYCLE_MILLIS, easing = LinearEasing))
            phase.snapTo(0f)
        }
    }
    val timeOfDay by remember { derivedStateOf { timeOfDay(phase.value) } }

    Box(Modifier.fillMaxSize()) {
        // Reading phase only inside the draw lambda keeps recomposition out of every frame.
        Canvas(Modifier.fillMaxSize()) { drawSky(phase.value) }

        // The title does not compute its color from the sky: it follows the library's decision.
        SceneHeader(title = timeOfDay, subtitle = "Day & night cycle", modifier = Modifier.align(Alignment.TopCenter))
    }
}

private class SkyKeyframe(val at: Float, val top: Color, val bottom: Color)

private val Keyframes = listOf(
    SkyKeyframe(0.00f, Color(0xFF9CCFFF), Color(0xFFEAF6FF)), // midday
    SkyKeyframe(0.12f, Color(0xFFCFE3F7), Color(0xFFFFF1D6)), // golden hour
    SkyKeyframe(0.22f, Color(0xFFFFB38A), Color(0xFFFFE3B3)), // sunset
    SkyKeyframe(0.36f, Color(0xFF2E2357), Color(0xFFC0627D)), // dusk
    SkyKeyframe(0.50f, Color(0xFF070B1E), Color(0xFF1A2550)), // midnight
    SkyKeyframe(0.72f, Color(0xFF0B1230), Color(0xFF26346A)), // late night
    SkyKeyframe(0.86f, Color(0xFF3B3F7A), Color(0xFFF2A7A0)), // dawn
    SkyKeyframe(1.00f, Color(0xFF9CCFFF), Color(0xFFEAF6FF)), // midday
)

private fun skyColors(phase: Float): Pair<Color, Color> {
    val end = Keyframes.indexOfFirst { it.at >= phase }.coerceAtLeast(1)
    val a = Keyframes[end - 1]
    val b = Keyframes[end]
    val t = ((phase - a.at) / (b.at - a.at)).coerceIn(0f, 1f)
    return lerp(a.top, b.top, t) to lerp(a.bottom, b.bottom, t)
}

private fun timeOfDay(phase: Float): String = when {
    phase < 0.15f -> "Midday"
    phase < 0.3f -> "Sunset"
    phase < 0.42f -> "Dusk"
    phase < 0.8f -> "Night"
    phase < 0.93f -> "Dawn"
    else -> "Midday"
}

private val HillColor = Color(0xFF1E2C3D)

private val Stars: List<Offset> = Random(42).let { random ->
    List(60) { Offset(random.nextFloat(), random.nextFloat() * 0.6f) }
}

private fun DrawScope.drawSky(phase: Float) {
    val (top, bottom) = skyColors(phase)
    drawRect(Brush.verticalGradient(listOf(top, bottom)))

    // Stars fade in after dusk and out before dawn.
    val starAlpha = (1f - abs(phase - 0.6f) / 0.22f).coerceIn(0f, 1f)
    if (starAlpha > 0f) {
        Stars.forEachIndexed { i, star ->
            drawCircle(
                color = Color.White.copy(alpha = starAlpha * (0.4f + (i % 3) * 0.2f)),
                radius = 1.5.dp.toPx() + (i % 2),
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }

    // The sun and the moon move along one circle, opposite each other.
    val horizon = size.height * 0.62f
    val orbit = size.width * 0.55f
    val angle = phase * 2 * PI
    val sun = Offset(center.x + orbit * sin(angle).toFloat(), horizon - orbit * cos(angle).toFloat())
    val moon = Offset(center.x - orbit * sin(angle).toFloat(), horizon + orbit * cos(angle).toFloat())
    drawCircle(Color(0xFFFFE08A).copy(alpha = 0.25f), radius = 72.dp.toPx(), center = sun)
    drawCircle(Color(0xFFFFD45C), radius = 44.dp.toPx(), center = sun)
    drawCircle(Color(0xFFF4F1E8), radius = 34.dp.toPx(), center = moon)

    drawHill(lerp(bottom, HillColor, 0.6f), baseline = horizon + 40.dp.toPx(), amplitude = 36.dp.toPx(), shift = 0f)
    drawHill(lerp(bottom, HillColor, 0.85f), baseline = horizon + 110.dp.toPx(), amplitude = 28.dp.toPx(), shift = 0.5f)
}

private fun DrawScope.drawHill(color: Color, baseline: Float, amplitude: Float, shift: Float) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseline)
        val steps = 24
        for (i in 0..steps) {
            val x = size.width * i / steps
            val y = baseline - amplitude * sin((i.toFloat() / steps + shift) * 2 * PI).toFloat()
            lineTo(x, y)
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color)
}

@Preview
@Composable
private fun SkyPreview() {
    SampleTheme {
        SkyScreen()
    }
}
