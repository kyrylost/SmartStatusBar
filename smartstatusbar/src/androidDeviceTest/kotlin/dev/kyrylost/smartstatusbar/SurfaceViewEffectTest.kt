package dev.kyrylost.smartstatusbar

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.milliseconds

/** SurfaceView content is not part of the window surface and has to be copied separately. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class SurfaceViewEffectTest {

    private val periodic = SmartStatusBarConfig(strategy = UpdateStrategy.Periodic(100.milliseconds))

    @Test
    fun lightSurfaceView_enablesDarkIcons() {
        launchWithContent(initialDarkIcons = false) {
            // Dark until the surface shows up, so the test cannot pass on the window background.
            Box(Modifier.fillMaxSize().background(Color.Black))
            SolidSurfaceView(Color.White)
            SmartStatusBarEffect(config = periodic)
        }.use { scenario ->
            assertTrue("white SurfaceView content must give dark icons", scenario.awaitDarkIcons(true))
        }
    }

    @Test
    fun composeContentOverSurfaceView_takesPrecedence() {
        launchWithContent(initialDarkIcons = true) {
            SolidSurfaceView(Color.White)
            // A dark toolbar drawn by the window on top of the white surface: the window content wins,
            // so simply replacing the strip with surface pixels would be wrong here.
            Box(Modifier.fillMaxWidth().height(120.dp).background(Color.Black))
            SmartStatusBarEffect(config = periodic)
        }.use { scenario ->
            assertTrue("dark content over the surface must give light icons", scenario.awaitDarkIcons(false))
        }
    }
}

@Composable
private fun SolidSurfaceView(color: Color) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) = Unit
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        val canvas = holder.lockHardwareCanvas()
                        canvas.drawColor(color.toArgb())
                        holder.unlockCanvasAndPost(canvas)
                    }
                    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                })
            }
        },
    )
}
