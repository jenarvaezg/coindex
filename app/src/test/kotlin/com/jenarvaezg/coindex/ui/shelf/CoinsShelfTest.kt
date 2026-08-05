package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shelf of Coins: five chip rows, a live count on each, and the search on top of them.
 *
 * The one that replaces a screen is «Sin colección» — the masthead button «Sin clasificar · N» was
 * always this filter (ADR 0021 §1), and what it does *not* carry any more is the reason: §12 moved
 * the why to the field report and left the app answering only which.
 */
class CoinsShelfTest {
    private val rows = coinRows(ShelfFixtures.state)

    @Test
    fun `Sin coleccion leaves exactly the coins no collection claims`() {
        val narrowed = CoinsShelf(membership = Membership.InNone).narrow(rows, "")

        assertEquals(listOf(ShelfFixtures.UNCACHED), narrowed.map { it.typeId })
    }

    @Test
    fun `the class chip separates the medal without taking it out of its collection`() {
        val medals = CoinsShelf(objectClass = ObjectClass.Exonumia).narrow(rows, "")
        val money = CoinsShelf(objectClass = ObjectClass.Coin).narrow(rows, "")

        assertEquals(listOf(ShelfFixtures.ONZA_MEXICANA), medals.map { it.typeId })
        assertEquals(3, money.size)
        // Still claimed by its box: a chip narrows the list, it does not move the coin.
        assertEquals(1, medals.single().claims.size)
    }

    @Test
    fun `two chips narrow together`() {
        val shelf = CoinsShelf(issuer = "Venezuela", weight = GramBand.TenToTwentyFive)

        assertEquals(listOf(ShelfFixtures.FUERTE), shelf.narrow(rows, "").map { it.typeId })
        assertEquals(
            emptyList(),
            shelf.copy(weight = GramBand.Ounce).narrow(rows, "").map { it.typeId },
        )
    }

    @Test
    fun `the search runs on top of the chips, without accents`() {
        val shelf = CoinsShelf(objectClass = ObjectClass.Coin)

        assertEquals(listOf(ShelfFixtures.FUERTE), shelf.narrow(rows, "bolivar").map { it.typeId })
        assertEquals(3, shelf.narrow(rows, "").size)
    }

    @Test
    fun `a facet counts with its own choice dropped, so a chip says what tapping it would give`() {
        val counts = coinsFacetCounts(rows, CoinsShelf(membership = Membership.InNone), "")

        // Its own row still offers the other three, and its «all» chip says the whole four.
        assertEquals(4, counts.membership.total)
        assertEquals(3, counts.membership.of(Membership.InSome))
        assertEquals(1, counts.membership.of(Membership.InNone))
        // Every other facet counts only inside the one coin the shelf leaves.
        assertEquals(1, counts.objectClass.total)
    }

    @Test
    fun `an uncached coin is offered by no country chip and dropped by none either`() {
        val counts = coinsFacetCounts(rows, CoinsShelf(), "")

        assertEquals(4, counts.issuer.total)
        assertEquals(
            listOf("México" to 1, "Reino Unido" to 1, "Venezuela" to 1),
            counts.issuers(),
        )
    }

    /**
     * The invariant that makes a shelf trustworthy: the chips of a facet add up to its total, so a
     * coin reachable by no chip cannot exist. The uncached coin is the case that proves it — it has
     * no year and no weight, and lands on «Sin año» and «Sin peso» rather than on nothing.
     */
    @Test
    fun `every chip row adds up to its own total, so no coin is unreachable`() {
        val counts = coinsFacetCounts(rows, CoinsShelf(), "")

        assertEquals(4, counts.year.total)
        assertEquals(counts.year.total, counts.year.byValue.values.sum())
        assertEquals(counts.weight.total, counts.weight.byValue.values.sum())
        assertEquals(counts.objectClass.total, counts.objectClass.byValue.values.sum())
        assertEquals(counts.membership.total, counts.membership.byValue.values.sum())
        assertEquals(1, counts.year.of(YearBand.Undated))
        assertEquals(1, counts.weight.of(GramBand.Unweighed))
    }
}
