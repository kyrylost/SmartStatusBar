package dev.kyrylost.smartstatusbar.sample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFFEC4899),
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF831843),
    background = Color(0xFFF7F8FB),
    surface = Color.White,
    onSurface = Color(0xFF14161C),
    surfaceVariant = Color(0xFFEFF1F6),
    onSurfaceVariant = Color(0xFF5B6070),
    surfaceContainer = Color(0xFFF7F8FB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFF9A8D4),
    secondaryContainer = Color(0xFF4A1D3A),
    onSecondaryContainer = Color(0xFFFBCFE8),
    background = Color(0xFF0E1015),
    surface = Color(0xFF15171E),
    onSurface = Color(0xFFECEDF2),
    surfaceVariant = Color(0xFF232631),
    onSurfaceVariant = Color(0xFFA3A8B8),
    surfaceContainer = Color(0xFF15171E),
)

private val SampleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp),
    )
}

@Composable
fun SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SampleTypography,
        content = content,
    )
}
