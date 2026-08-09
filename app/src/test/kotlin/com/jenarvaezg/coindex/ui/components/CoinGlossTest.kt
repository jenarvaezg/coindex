package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The band of light, measured as a fraction of the coin and not in dp.
 *
 * #303 prototyped ±55 dp over a hole of 121 dp and wrote down what that was: ±45 % of the diameter.
 * Production's hole is 104 dp, so 55 dp literally would be ±53 % and the band would spend more time
 * off the coin than on it. The proportion is what survives a hole of another size — «Las cifras»
 * brings one of its own.
 */
class CoinGlossTest {
    /** The photograph inside a 104 dp casilla, once the die-cut's cardboard is taken off. */
    private val casilla = 104f - 2 * HOLE_CARD_PADDING_DP

    @Test
    fun `the approved travel is the prototype's proportion, not its dp`() {
        assertEquals(0.45f, CoinGloss.Default.travel)
        assertEquals(105f, CoinGloss.Default.angleDegrees)

        val prototype = CoinGloss.Default.bandCentre(diameterPx = 121f, lateral = 1f)
        // 54.45 dp: the prototype's own ±55 dp, which is where it rounded its ±45 % from.
        assertEquals(55f, prototype, 0.6f)
    }

    @Test
    fun `the same proportion on a smaller hole is a shorter travel`() {
        val travel = CoinGloss.Default.bandCentre(diameterPx = casilla, lateral = 1f)

        assertEquals(42.3f, travel, 0.1f)
        assertTrue(travel < casilla / 2f, "the band must not leave the coin")
    }

    @Test
    fun `on the table the band sits on the centre of the coin`() {
        assertEquals(0f, CoinGloss.Default.bandCentre(diameterPx = casilla, lateral = 0f))
    }

    @Test
    fun `past the forty five degrees that saturate it the band stops`() {
        val extreme = CoinGloss.Default.bandCentre(diameterPx = casilla, lateral = 4f)

        assertEquals(CoinGloss.Default.bandCentre(casilla, lateral = 1f), extreme)
        assertEquals(CoinGloss.Default.bandCentre(casilla, lateral = -1f), CoinGloss.Default.bandCentre(casilla, -9f))
    }

    @Test
    fun `the band is as wide as the prototype's, also in proportion`() {
        assertEquals(0.32f, CoinGloss.Default.halfBand)
        assertEquals(casilla * 0.32f, CoinGloss.Default.bandHalfWidth(casilla))
    }
}
