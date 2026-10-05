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
 * Where an index card goes when tapped (ADR 0021 §9), chosen by whether it has a plate to open
 * rather than by its species: the same bit that decides the card's third line.
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

    /** `plateCatalogId` is null exactly when `resolvePlate` would refuse. */
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
