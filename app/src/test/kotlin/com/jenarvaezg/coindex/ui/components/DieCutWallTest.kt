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
        // 3 and 9 o'clock, where the old pair of `sweepAngle = 180f` arcs met.
        assertEquals(0f, alphaAt(0f), TOLERANCE)
        assertEquals(0f, alphaAt(0.5f), TOLERANCE)
        assertEquals(0f, alphaAt(1f), TOLERANCE)
    }

    @Test
    fun `the wall has no step anywhere along the sweep`() {
        // Sampled every half degree: no step may be big enough to read as a border (#357).
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
        // The cut edge and the 1 dp hairline that separates cardboard from paper (#349) are
        // separate jobs (#357).
        assertTrue(stops.all { (_, color) -> color.alpha < 1f })
        assertTrue(stops.none { (_, color) -> color.copy(alpha = 1f) == Paper.hairline })
    }

    @Test
    fun `the wall is exactly as wide as the cardboard the die leaves free`() {
        // Drawn inwards from the hole's edge, so its inner edge meets the photograph and adds no
        // light over the one already baked into it (#303).
        assertEquals(HOLE_CARD_PADDING_DP, wall.widthDp)
    }

    @Test
    fun `a dense axis hole keeps a readable cardboard ring without the 104 dp bite`() {
        // 5 dp on 104 dp is the measured ring. Pure proportion on 34 dp is ~1.6 dp and vanishes;
        // 2 dp is the floor that still reads on dark metal without the country-axis «mucho borde».
        assertEquals(HOLE_CARD_PADDING_DP, holeCardPaddingDp(DESIGN_HOLE_DP))
        assertEquals(2f, holeCardPaddingDp(34f), 0.01f)
    }

    @Test
    fun `a turned casilla is lit from the other side`() {
        // #509: the lit and shadowed sides swap. The sweep is cross-faded against this profile,
        // so the cardboard stays still (ADR 0026 §3).
        val turned = wall.turnedStops()

        assertEquals(Paper.ink, colorAt(0.25f, turned))
        assertEquals(Color.White, colorAt(0.75f, turned))
        assertEquals(wall.turnedShadowAlpha, alphaAt(0.25f, turned), TOLERANCE)
        assertEquals(wall.turnedSheenAlpha, alphaAt(0.75f, turned), TOLERANCE)
    }

    @Test
    fun `the turned wall is brighter than the one at rest, and still closes`() {
        // Turned, the wall competes with a photograph that has just changed, so swapping sides
        // alone is not enough.
        val turned = wall.turnedStops()

        assertTrue(wall.turnedSheenAlpha > wall.sheenAlpha)
        assertTrue(wall.turnedShadowAlpha > wall.shadowAlpha)
        assertEquals(0f, alphaAt(0f, turned), TOLERANCE)
        assertEquals(0f, alphaAt(0.5f, turned), TOLERANCE)
        assertEquals(0f, alphaAt(1f, turned), TOLERANCE)
    }

    @Test
    fun `the turned wall has no step anywhere along the sweep either`() {
        val turned = wall.turnedStops()

        val steepest = (0..720)
            .map { alphaAt(it / 720f, turned) }
            .zipWithNext { before, after -> abs(after - before) }
            .max()

        assertTrue(steepest < 0.008f, "the turned sweep jumps $steepest of alpha in half a degree")
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
    private fun colorAt(fraction: Float, sweep: Array<Pair<Float, Color>> = stops): Color =
        sweep.first { (stop, _) -> stop >= fraction }.second.copy(alpha = 1f)

    private companion object {
        /** `Color` keeps sRGB channels in 8 bits, so an alpha never comes back exactly as asked. */
        const val TOLERANCE = 1f / 255f
    }
}
