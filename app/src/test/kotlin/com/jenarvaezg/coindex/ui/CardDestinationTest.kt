package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a card of the index goes when it is tapped — one card, one destination (ADR 0021 §9).
 *
 * The destination is chosen by a **capability** and not by a declared species: whether there is a
 * plate to open. That is the same bit that already decides the third line of the card, which is why
 * the collector never sees a seam between a curated collection and a box.
 */
class CardDestinationTest {
    private val paquillos = DerivedCollection(
        family = "100 pesetas",
        weightMillioz = 611,
        finish = Finish.Bullion,
        metal = Metal.Silver,
        distinctTypes = 4,
        quantity = 5,
    )

    private fun derived(plateCatalogId: String?, coverage: CoverageRatio? = null) =
        IndexCard.Derived(
            name = "Paquillos",
            coverage = coverage,
            issuer = "España",
            collection = paquillos,
            plateCatalogId = plateCatalogId,
        )

    @Test
    fun `a card with a reachable plate opens the plate, in one tap`() {
        val destination = destinationOf(
            derived(plateCatalogId = "espana-paquillos", coverage = CoverageRatio(4, 6)),
        )

        assertEquals(CardDestination.Plate("espana-paquillos"), destination)
    }

    @Test
    fun `a card without one opens its pieces`() {
        assertEquals(
            CardDestination.Pieces(paquillos.key()),
            destinationOf(derived(plateCatalogId = null)),
        )
    }

    /**
     * A catalog the collector owns no official type of yet has no plate to open — `plateCatalogId`
     * is null exactly when `resolvePlate` would refuse — so the card goes to its pieces rather than
     * to a screen that would only explain why it is empty.
     */
    @Test
    fun `a catalog with no evidence yet is not a plate destination`() {
        assertEquals(
            CardDestination.Pieces(paquillos.key()),
            destinationOf(derived(plateCatalogId = null, coverage = CoverageRatio(0, 6))),
        )
    }

    @Test
    fun `a box opens the same pieces screen, addressed by its own id`() {
        val box = IndexCard.Box(
            name = "Las francesas",
            issuer = "Francia",
            box = OwnGroupingView(
                OwnGrouping(id = 4, name = "Las francesas", typeIds = listOf(11)),
                listOf(CollectedItem(id = 1, quantity = 1, typeId = 11)),
            ),
        )

        assertEquals(CardDestination.Box(4), destinationOf(box))
    }
}
