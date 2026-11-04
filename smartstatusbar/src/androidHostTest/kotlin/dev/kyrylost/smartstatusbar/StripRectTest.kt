package dev.kyrylost.smartstatusbar

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StripRectTest {

    private val density = Density(2f)

    @Test
    fun defaultInsets_areAppliedInPixels() {
        val rect = stripRect(statusBarHeightPx = 100, windowWidthPx = 1080, density = density)
        assertNotNull(rect)
        assertEquals(8, rect!!.left)
        assertEquals(8, rect.top)
        assertEquals(1080 - 8, rect.right)
        assertEquals(100 - 8, rect.bottom)
    }

    @Test
    fun insetsUseDensity() {
        val rect = stripRect(100, 1000, Density(1.5f))!!
        assertEquals(6, rect.left)
        assertEquals(6, rect.top)
    }

    @Test
    fun zeroStatusBarHeight_returnsNull() {
        assertNull(stripRect(0, 1080, density))
    }

    @Test
    fun negativeStatusBarHeight_returnsNull() {
        assertNull(stripRect(-1, 1080, density))
    }

    @Test
    fun zeroWidth_returnsNull() {
        assertNull(stripRect(100, 0, density))
    }

    @Test
    fun verticalInsetsConsumeWholeHeight_returnsNull() {
        assertNull(stripRect(16, 1080, density))
    }

    @Test
    fun verticalInsetsLeaveOnePixel_returnsRect() {
        val rect = stripRect(17, 1080, density)
        assertNotNull(rect)
        assertEquals(1, rect!!.height)
    }

    @Test
    fun horizontalInsetsConsumeWholeWidth_returnsNull() {
        assertNull(stripRect(100, 16, density))
    }
}
