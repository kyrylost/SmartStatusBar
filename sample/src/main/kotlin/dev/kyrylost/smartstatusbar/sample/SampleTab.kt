package dev.kyrylost.smartstatusbar.sample

import androidx.annotation.DrawableRes
import dev.kyrylost.smartstatusbar.UpdateStrategy
import dev.kyrylost.smartstatusbar.sample.live.LiveStrategy
import dev.kyrylost.smartstatusbar.sample.photo.PhotoStrategy

/**
 * One tab per kind of content under the status bar, with what the library does about it. In the
 * order of the story: watch it work, scroll real content, try your own, then the hard case.
 */
enum class SampleTab(
    val label: String,
    @DrawableRes val icon: Int,
    val title: String,
    val description: String,
    val code: String,
    /** The strategy that checks this screen, and where it comes from and when it checks. */
    val strategy: UpdateStrategy,
    val strategyNote: String,
) {
    Sky(
        label = "Sky",
        icon = R.drawable.ic_moon,
        title = "Endless animation",
        description = "The sky redraws every frame, so Debounce never settles; its maxWait still checks once a second. " +
            "The title takes its color from the same decision as the icons.",
        code = "SmartStatusBarEffect() // once, in the Activity",
        strategy = AppWideStrategy,
        strategyNote = "app-wide · forced every second while it animates",
    ),
    Feed(
        label = "Feed",
        icon = R.drawable.ic_feed,
        title = "Photos under the bar",
        description = "Bright and dark photos scroll under the status bar; the icons switch once scrolling settles.",
        code = "// nothing on the screen itself",
        strategy = AppWideStrategy,
        strategyNote = "app-wide · once scrolling settles",
    ),
    Photo(
        label = "Your photo",
        icon = R.drawable.ic_image,
        title = "Your own photo",
        description = "Pick any photo, then drag or pinch it: whatever ends up under the status bar decides the icons. " +
            "This screen knows when to check, so its override is Manual and asks for a check when you let go: " +
            "the icons never change in the middle of a gesture.",
        code = "SmartStatusBarOverride(SmartStatusBarConfig(UpdateStrategy.Manual))\n" +
            "controller.requestUpdate() // when the finger lifts",
        strategy = PhotoStrategy,
        strategyNote = "screen override · when you let go",
    ),
    Live(
        label = "Live",
        icon = R.drawable.ic_play,
        title = "SurfaceView",
        description = "A SurfaceView renders on its own thread and the window never redraws, so this screen " +
            "overrides the app-wide effect with a timer.",
        code = "SmartStatusBarOverride(SmartStatusBarConfig(UpdateStrategy.Periodic()))",
        strategy = LiveStrategy,
        strategyNote = "screen override · polls the SurfaceView",
    ),
}
