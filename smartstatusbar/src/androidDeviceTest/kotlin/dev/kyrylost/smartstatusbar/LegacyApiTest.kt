package dev.kyrylost.smartstatusbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** PixelCopy of a window needs API 26: below that the effect must leave the icons alone and not crash. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(maxSdkVersion = 25)
class LegacyApiTest {

    @Test
    fun belowApi26_effectDoesNothing() {
        launchWithContent(initialDarkIcons = false) {
            Box(Modifier.fillMaxSize().background(Color.White))
            SmartStatusBarEffect()
        }.use { scenario ->
            assertTrue("icons must stay put", true !in scenario.observeDarkIcons(durationMillis = 1_000))
        }
    }
}
