package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DieCutWallTest {
    private val wall = DieCutWall()
    private val stops = wall.stops()

    @Test
    fun `the wall closes where the two half arcs used to meet`() {
        // 3 and 9 o'clock: the terminations of the old `sweepAngle = 180f` pair.
        assertEquals(0f, alphaAt(0f), TOLERANCE)
        assertEquals(0f, alphaAt(0.5f), TOLERANCE)
        assertEquals(0f, alphaAt(1f), TOLERANCE)
    }

    @Test
    fun `the wall has no step anywhere along the sweep`() {
        // The arc that #357 measured jumped 76 of the 255 luminance levels between two pixels. This
        // is the profile the brush is handed, sampled every half degree: what replaces the seam is
        // the absence of any step big enough to be a border, not a smaller step.
        val steepest = (0..720)
            .map { alphaAt(it / 720f) }
            .zipWithNext { before, after -> abs(after - before) }
            .max()

        assertTrue(steepest < 0.006f, "the sweep jumps $steepest of alpha in half a degree")
    }

    @Test
    fun `the lit edge is at the bottom and the shadow at the top`() {
        // Fractions run clockwise from 3 o'clock, so 0.25 is 6 o'clock and 0.75 is 12 o'clock.
        assertEquals(wall.sheenAlpha, alphaAt(0.25f), TOLERANCE)
        assertEquals(wall.shadowAlpha, alphaAt(0.75f), TOLERANCE)
        assertEquals(Color.White, colorAt(0.25f))
        assertEquals(Paper.ink, colorAt(0.75f))
    }

    @Test
    fun `the wall is translucent everywhere and never the separating hairline`() {
        // A 5 dp stroke that models a cut edge is not the 1 dp rule that separates cardboard from
        // paper: #349 gave the hairline its 3:1 and #357 keeps the two jobs apart.
        assertTrue(stops.all { (_, color) -> color.alpha < 1f })
        assertTrue(stops.none { (_, color) -> color.copy(alpha = 1f) == Paper.hairline })
    }

    @Test
    fun `the wall is exactly as wide as the cardboard the die leaves free`() {
        // Drawn inwards from the hole's edge, so this is what puts its inner edge where the
        // photograph starts and not one dp further in. #303: the photograph brings its own light
        // already baked in, and a second one painted on top contradicts it.
        assertEquals(HOLE_CARD_PADDING_DP, wall.widthDp)
    }

    @Test
    fun `the bench moves both halves of the wall independently`() {
        val calibrated = DieCutWall(sheenAlpha = 0.3f, shadowAlpha = 0.1f)

        val moved = calibrated.stops()

        assertEquals(0.3f, alphaAt(0.25f, moved), TOLERANCE)
        assertEquals(0.1f, alphaAt(0.75f, moved), TOLERANCE)
    }

    /** The same linear interpolation between stops that a sweep gradient paints. */
    private fun alphaAt(fraction: Float, sweep: Array<Pair<Float, Color>> = stops): Float {
        val next = sweep.indexOfFirst { (stop, _) -> stop >= fraction }.coerceAtLeast(1)
        val (fromStop, from) = sweep[next - 1]
        val (toStop, to) = sweep[next]
        val progress = ((fraction - fromStop) / (toStop - fromStop)).coerceIn(0f, 1f)
        return from.alpha + (to.alpha - from.alpha) * progress
    }

    /** The tint of the half a fraction falls in, with its alpha set aside. */
    private fun colorAt(fraction: Float): Color =
        stops.first { (stop, _) -> stop >= fraction }.second.copy(alpha = 1f)

    private companion object {
        /** `Color` keeps sRGB channels in 8 bits, so an alpha never comes back exactly as asked. */
        const val TOLERANCE = 1f / 255f
    }
}
