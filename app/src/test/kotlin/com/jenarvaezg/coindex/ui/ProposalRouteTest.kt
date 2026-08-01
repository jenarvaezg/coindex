package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SPANNING_VARIANTS_WEIGHT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A proposal route carries the same three canonical parts a disposition is stored under, and it
 * is read back with the same suspicion: anything that is not already canonical is refused rather
 * than guessed at, because the alternative is a screen about a variant that does not exist.
 *
 * `Routes.proposal` itself is not exercised here: it encodes through `android.net.Uri`, which is
 * not available to a JVM unit test. What matters is the reading.
 */
class ProposalRouteTest {
    @Test
    fun `a canonical route rebuilds its key, accents and spaces included`() {
        assertEquals(
            CollectionProposalKey("100 Pesetas de Franco", 611, null, Metal.Silver),
            proposalKeyFromRoute("100 Pesetas de Franco", "611", "unknown", "silver"),
        )
        assertEquals(
            CollectionProposalKey("Tudor Beasts", 2_000, Finish.Bullion, Metal.Silver),
            proposalKeyFromRoute("Tudor Beasts", "2000", "bullion", "silver"),
        )
    }

    /** A set spans physical variants, so its weight is the sentinel rather than a number. */
    @Test
    fun `a set route rebuilds an absent weight`() {
        val key = proposalKeyFromRoute(
            "Trío de 1983",
            SPANNING_VARIANTS_WEIGHT.toString(),
            "unknown",
            "unknown",
        )
        assertEquals(CollectionProposalKey("Trío de 1983", null, null, null), key)
        assertNull(key?.weightMillioz)
    }

    @Test
    fun `anything that is not canonical is refused`() {
        // An unnormalized family, an impossible weight, an unknown finish or metal code.
        assertNull(proposalKeyFromRoute("Dos  espacios", "611", "unknown", "silver"))
        assertNull(proposalKeyFromRoute("Familia", "0", "unknown", "silver"))
        assertNull(proposalKeyFromRoute("Familia", "611", "chapado en resina", "silver"))
        assertNull(proposalKeyFromRoute("Familia", "611", "unknown", "plata"))
        // A truncated or absent argument is not a key either.
        assertNull(proposalKeyFromRoute(null, "611", "unknown", "silver"))
        assertNull(proposalKeyFromRoute("Familia", null, "unknown", "silver"))
        assertNull(proposalKeyFromRoute("Familia", "seiscientos", "unknown", "silver"))
        assertNull(proposalKeyFromRoute("Familia", "611", null, "silver"))
        assertNull(proposalKeyFromRoute("Familia", "611", "unknown", null))
    }
}
