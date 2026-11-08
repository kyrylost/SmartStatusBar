package io.github.kyrylost.smartstatusbar

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SurfaceCompositeTest {

    // A 640 x 40 strip at (10, 10) maps to a 64 x 4 grid of 10 x 10 cells.
    private val strip = IntRect(10, 10, 650, 50)

    @Test
    fun gridCells_areaCoveringStrip_coversWholeGrid() {
        assertEquals(IntRect(0, 0, 64, 4), gridCells(strip, IntRect(0, 0, 1080, 2400), 64, 4))
    }

    @Test
    fun gridCells_partialArea_roundsOutwardToWholeCells() {
        // Clipped to the strip: x 25..105 -> cells 1.5..9.5 -> 1..10; y 10..25 -> 0..1.5 -> 0..2
        assertEquals(IntRect(1, 0, 10, 2), gridCells(strip, IntRect(25, 0, 105, 25), 64, 4))
    }

    @Test
    fun gridCells_areaOutsideStrip_returnsNull() {
        assertNull(gridCells(strip, IntRect(0, 100, 1080, 2400), 64, 4))
    }

    @Test
    fun cellsToWindow_mapsBackToStripCoordinates() {
        assertEquals(IntRect(20, 10, 110, 30), cellsToWindow(strip, IntRect(1, 0, 10, 2), 64, 4))
    }

    @Test
    fun fillHoles_keepsOpaqueReplacesTransparentAndBlendsTranslucent() {
        val opaque = 0xFF112233.toInt()
        val transparent = 0x00000000
        val halfWhite = 0x80FFFFFF.toInt()
        val window = intArrayOf(opaque, transparent, halfWhite, transparent)
        val surface = intArrayOf(0xFF000000.toInt(), 0xFFABCDEF.toInt(), 0xFF000000.toInt())

        // Width 4, fill cells x 0..3 of row 0; the last pixel is outside and stays transparent.
        fillHoles(window, width = 4, surface = surface, cells = IntRect(0, 0, 3, 1))

        assertArrayEquals(
            intArrayOf(opaque, 0xFFABCDEF.toInt(), 0xFF808080.toInt(), transparent),
            window,
        )
    }
}
