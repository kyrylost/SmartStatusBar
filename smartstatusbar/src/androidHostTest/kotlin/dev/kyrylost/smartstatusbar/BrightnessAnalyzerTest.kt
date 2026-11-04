package dev.kyrylost.smartstatusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrightnessAnalyzerTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val gray = 0xFF808080.toInt() // luminance = 128

    private fun strip(width: Int, height: Int, pixel: (x: Int, y: Int) -> Int): IntArray =
        IntArray(width * height) { i -> pixel(i % width, i / width) }

    private fun ratio(
        px: IntArray,
        width: Int = CAPTURE_WIDTH,
        sideFraction: Float = 0.2f,
        threshold: Float = 0.5f,
    ): Float? = lightPixelRatio(px, width, sideFraction, threshold)

    // ---------- basic ----------

    @Test
    fun allWhite_isFullyLight() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> white }
        assertEquals(1f, ratio(px)!!, 0.001f)
    }

    @Test
    fun allBlack_isFullyDark() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> black }
        assertEquals(0f, ratio(px)!!, 0.001f)
    }

    @Test
    fun topRowsWhiteBottomRowsBlack_isHalf() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, y -> if (y < 2) white else black }
        assertEquals(0.5f, ratio(px)!!, 0.001f)
    }

    // ---------- side zones ----------

    @Test
    fun whiteCenterBlackEdges_isDark() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { x, _ ->
            if (x in 12 until CAPTURE_WIDTH - 12) white else black
        }
        assertEquals(0f, ratio(px)!!, 0.001f)
    }

    @Test
    fun blackCenterWhiteEdges_isLight() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { x, _ ->
            if (x in 12 until CAPTURE_WIDTH - 12) black else white
        }
        assertEquals(1f, ratio(px)!!, 0.001f)
    }

    @Test
    fun leftWhiteRightBlack_isHalf() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { x, _ -> if (x < CAPTURE_WIDTH / 2) white else black }
        assertEquals(0.5f, ratio(px)!!, 0.001f)
    }

    @Test
    fun sideFractionHalf_analyzesWholeWidth() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { x, _ -> if (x < 16) white else black }
        assertEquals(0.25f, ratio(px, sideFraction = 0.5f)!!, 0.001f)
    }

    @Test
    fun tinySideFraction_stillAnalyzesAtLeastOneColumnPerSide() {
        val px = strip(CAPTURE_WIDTH, 1) { x, _ -> if (x == 0 || x == CAPTURE_WIDTH - 1) white else black }
        assertEquals(1f, ratio(px, sideFraction = 0.001f)!!, 0.001f)
    }

    @Test
    fun width2_analyzesBothColumns() {
        val px = intArrayOf(white, black)
        assertEquals(0.5f, ratio(px, width = 2)!!, 0.001f)
    }

    @Test
    fun width1_analyzesTheSingleColumn() {
        assertEquals(1f, ratio(intArrayOf(white), width = 1)!!, 0.001f)
        assertEquals(0f, ratio(intArrayOf(black), width = 1)!!, 0.001f)
    }

    // ---------- threshold & luminance ----------

    @Test
    fun midGray_dependsOnThreshold() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> gray }
        assertEquals(1f, ratio(px, threshold = 0.5f)!!, 0.001f)
        assertEquals(0f, ratio(px, threshold = 0.6f)!!, 0.001f)
    }

    @Test
    fun threshold1_nothingIsLight() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> white }
        assertEquals(0f, ratio(px, threshold = 1f)!!, 0.001f)
    }

    @Test
    fun threshold0_blackIsDarkAnythingElseIsLight() {
        val black = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> black }
        val almostBlack = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, _ -> 0xFF0A0A0A.toInt() }
        assertEquals(0f, ratio(black, threshold = 0f)!!, 0.001f)
        assertEquals(1f, ratio(almostBlack, threshold = 0f)!!, 0.001f)
    }

    @Test
    fun luminanceWeights_greenDominates() {
        val green = strip(CAPTURE_WIDTH, 1) { _, _ -> 0xFF00FF00.toInt() }
        val red = strip(CAPTURE_WIDTH, 1) { _, _ -> 0xFFFF0000.toInt() }
        val blue = strip(CAPTURE_WIDTH, 1) { _, _ -> 0xFF0000FF.toInt() }
        assertEquals(1f, ratio(green)!!, 0.001f)
        assertEquals(0f, ratio(red)!!, 0.001f)
        assertEquals(0f, ratio(blue)!!, 0.001f)
    }

    // ---------- transparency ----------

    @Test
    fun transparentPixels_areSkipped() {
        val px = strip(CAPTURE_WIDTH, CAPTURE_HEIGHT) { _, y -> if (y < 2) 0 else white }
        assertEquals(1f, ratio(px)!!, 0.001f)
    }

    @Test
    fun fullyTransparent_returnsNull() {
        assertNull(ratio(IntArray(CAPTURE_WIDTH * CAPTURE_HEIGHT)))
    }

    // ---------- invalid input ----------

    @Test
    fun emptyArray_returnsNull() {
        assertNull(ratio(IntArray(0)))
    }

    @Test
    fun arrayShorterThanWidth_returnsNull() {
        assertNull(ratio(IntArray(10), width = 64))
    }

    @Test
    fun zeroOrNegativeWidth_returnsNull() {
        assertNull(ratio(IntArray(16), width = 0))
        assertNull(ratio(IntArray(16), width = -4))
    }
}
