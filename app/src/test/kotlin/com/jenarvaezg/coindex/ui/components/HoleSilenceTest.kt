package com.jenarvaezg.coindex.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The three silences of a hole (#510). A photograph still loading and one that will not arrive on
 * this launch used to draw the same stand-in disc.
 */
class HoleSilenceTest {
    @Test
    fun `a photograph on the hole is no silence at all`() {
        assertNull(holeSilence(candidates = 1, settled = true, painted = true))
    }

    @Test
    fun `a face the catalogue has no picture for is the stand-in of always`() {
        assertEquals(
            HoleSilence.NoPhotograph,
            holeSilence(candidates = 0, settled = false, painted = false),
        )
    }

    @Test
    fun `a request still in flight is loading`() {
        assertEquals(
            HoleSilence.Loading,
            holeSilence(candidates = 2, settled = false, painted = false),
        )
    }

    @Test
    fun `a request that came back without a photograph is not on this phone`() {
        assertEquals(
            HoleSilence.NotOnThisPhone,
            holeSilence(candidates = 2, settled = true, painted = false),
        )
    }

    @Test
    fun `settling is what separates waiting from loading, and nothing else does`() {
        val loading = holeSilence(candidates = 1, settled = false, painted = false)
        val waiting = holeSilence(candidates = 1, settled = true, painted = false)
        assertEquals(HoleSilence.Loading, loading)
        assertEquals(HoleSilence.NotOnThisPhone, waiting)
    }
}
