package com.jenarvaezg.coindex.ui.components

import androidx.compose.ui.graphics.Color
import com.jenarvaezg.coindex.ui.theme.Paper
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DieCutWallTest {
    private val stops = dieCutWallStops(AlbumToneConfig())

    @Test
    fun `the wall closes where the two half arcs used to meet`() {
        // 3 and 9 o'clock: the terminations of the old `sweepAngle = 180f` pair.
        assertEquals(0f, alphaAt(0f), TOLERANCE)
        assertEquals(0f, alphaAt(0.5f), TOLERANCE)
        assertEquals(0f, alphaAt(1f), TOLERANCE)
    }

    @Test
    fun `the wall has no step anywhere along the sweep`() {
        // The arc that #357 measured went from 0.62 of white to nothing between two pixels. This
        // is the same profile sampled every half degree: what replaces the seam is the absence of
        // any step big enough to be a border, not a smaller step.
        val steepest = (0..720)
            .map { alphaAt(it / 720f) }
            .zipWithNext { before, after -> abs(after - before) }
            .max()

        assertTrue(steepest < 0.006f, "el barrido salta $steepest de alfa en medio grado")
    }

    @Test
    fun `the lit edge is at the bottom and the shadow at the top`() {
        val tone = AlbumToneConfig()

        // Fractions run clockwise from 3 o'clock, so 0.25 is 6 o'clock and 0.75 is 12 o'clock.
        assertEquals(tone.dieWallSheenAlpha, alphaAt(0.25f), TOLERANCE)
        assertEquals(tone.dieWallShadowAlpha, alphaAt(0.75f), TOLERANCE)
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
    fun `the wall never reaches a pixel of the photograph`() {
        // It is drawn inwards from the hole's edge, so it stays on cardboard only while it is no
        // wider than the cardboard the padding leaves free. #303: the photograph brings its own
        // light already baked in, and a second one painted on top contradicts it.
        assertTrue(AlbumToneConfig().dieWallWidthDp <= HOLE_CARD_PADDING_DP)
    }

    @Test
    fun `the album hole draws no arc at all any more`() {
        // Both defects #357 reported were half arcs: the pair on the cardboard that ended at 3 and
        // 9 o'clock, and the pair inside the hole that fell on the coin. The sweep replaced the
        // first and the second was withdrawn, so a new `drawArc` here would be a regression of one
        // or the other.
        val source = File(requireNotNull(System.getProperty("user.dir")))
            .resolve("src/main/kotlin/com/jenarvaezg/coindex/ui/components/AlbumPaper.kt")

        assertFalse(source.readText().contains("drawArc"))
    }

    @Test
    fun `the bench moves both halves of the wall independently`() {
        val calibrated = AlbumToneConfig(dieWallSheenAlpha = 0.3f, dieWallShadowAlpha = 0.1f)

        val moved = dieCutWallStops(calibrated)

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
