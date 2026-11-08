package io.github.kyrylost.smartstatusbar

import androidx.compose.runtime.Immutable

/**
 * Configuration options for SmartStatusBar. The defaults suit most screens.
 *
 * @param strategy When to check the content under the status bar (default [UpdateStrategy.Debounce]).
 * @param sideFraction Share of the width (0.0..0.5] analyzed at each end of the status bar, where the clock,
 *   notification icons and battery are. Raise it on devices or layouts where icons spread further to the
 *   middle; 0.5 analyzes the whole bar. [SmartStatusBarDebugOverlay] shows the analyzed zones.
 * @param lightThreshold Luma (0.0..1.0 of 255) above which a pixel counts as light: 0.5 is about 128/255.
 *   Raise it to treat mid-tones as dark (and so get light icons on them), lower it for the opposite.
 * @param hysteresis Dead zone (0.0..0.5) around a 50% share of light pixels: the icons switch only once the
 *   share leaves `0.5 ± hysteresis`, so content hovering near the boundary does not make them flicker.
 *   0 switches at exactly 50%.
 */
@Immutable
public class SmartStatusBarConfig(
    public val strategy: UpdateStrategy = UpdateStrategy.Debounce(),
    public val sideFraction: Float = 0.2f,
    public val lightThreshold: Float = 0.5f,
    public val hysteresis: Float = 0.05f,
) {
    init {
        require(sideFraction > 0f && sideFraction <= 0.5f) { "sideFraction must be in (0.0, 0.5]" }
        require(lightThreshold in 0.0f..1.0f) { "lightThreshold must be between 0.0 and 1.0" }
        require(hysteresis in 0.0f..0.5f) { "hysteresis must be in 0.0..0.5" }
    }

    override fun equals(other: Any?): Boolean =
        other is SmartStatusBarConfig &&
            strategy == other.strategy &&
            sideFraction == other.sideFraction &&
            lightThreshold == other.lightThreshold &&
            hysteresis == other.hysteresis

    override fun hashCode(): Int {
        var r = strategy.hashCode()
        r = 31 * r + sideFraction.hashCode()
        r = 31 * r + lightThreshold.hashCode()
        r = 31 * r + hysteresis.hashCode()
        return r
    }

    override fun toString(): String =
        "SmartStatusBarConfig(strategy=$strategy, sideFraction=$sideFraction, " +
            "lightThreshold=$lightThreshold, hysteresis=$hysteresis)"
}
