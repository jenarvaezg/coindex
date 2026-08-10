package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `Sin coleccion leaves exactly the coins with a piece no collection claims`() {
        val narrowed = CoinsShelf(membership = Membership.InNone).narrow(rows, "")

        assertEquals(
            listOf(ShelfFixtures.BRITANNIA, ShelfFixtures.UNCACHED),
            narrowed.map { it.typeId }.sorted(),
        )
    }

    /**
     * The regression this chip exists to prevent, measured on the father's collection.
     *
     * His American Silver Eagle N#298883 is two rows, issues 760576 and 1059386, and its catalog
     * qualifies members by issue (ADR 0019): one row fills a member and the other is unclassified
     * residue. `UnclassifiedScreen` listed that second coin row by row; deciding membership from the
     * type's `claims` would have made it disappear from the one place ADR 0021 §12 leaves for it.
     */
    @Test
    fun `a coin in a collection is still under Sin coleccion while one of its pieces is loose`() {
        val britannia = rows.single { it.typeId == ShelfFixtures.BRITANNIA }

        assertEquals(2, britannia.quantity)
        assertEquals(1, britannia.unclaimedPieces)
        // In a collection *and* holding a loose piece: both statements are true of it.
        assertEquals(listOf("Britannia"), britannia.claims.map { it.name })
        assertTrue(
            britannia in CoinsShelf(membership = Membership.InNone).narrow(rows, ""),
        )
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

    /** ADR 0021 §1: «both sides carry filters, **sorting** and a live search». */
    @Test
    fun `the sort reorders what the chips leave, and never loses a row`() {
        val byCountry = CoinsShelf().narrow(rows, "")
        val alphabetical = CoinsShelf(sort = CoinSort.Alphabetical).narrow(rows, "")
        val newest = CoinsShelf(sort = CoinSort.Newest).narrow(rows, "")
        val heaviest = CoinsShelf(sort = CoinSort.Heaviest).narrow(rows, "")
        val mostPieces = CoinsShelf(sort = CoinSort.MostPieces).narrow(rows, "")

        assertEquals(byCountry.toSet(), alphabetical.toSet())
        assertEquals(
            listOf("1 Onza", "Bolívar de 25 g", "Britannia", "Pieza 12"),
            alphabetical.map { it.title },
        )
        // Newest first, and the coin nobody dated last rather than first.
        assertEquals(
            listOf(2020, 1978, 1929, null),
            newest.map { it.year },
        )
        // The unweighed coin sits at the bottom: it is not lighter, it is unweighed.
        assertEquals(ShelfFixtures.UNCACHED, heaviest.last().typeId)
        assertEquals(ShelfFixtures.ONZA_MEXICANA, heaviest.first().typeId)
        // Three of the Bolívar against one or two of everything else.
        assertEquals(ShelfFixtures.FUERTE, mostPieces.first().typeId)
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

        // Its own row still offers the others, and its «all» chip says the whole four.
        assertEquals(4, counts.membership.total)
        assertEquals(2, counts.membership.of(Membership.InSome))
        assertEquals(2, counts.membership.of(Membership.InNone))
        // Every other facet counts only inside the two coins the shelf leaves.
        assertEquals(2, counts.objectClass.total)
    }

    @Test
    fun `an uncached coin is offered by no country chip and dropped by none either`() {
        val counts = coinsFacetCounts(rows, CoinsShelf(), "")

        assertEquals(4, counts.issuer.total)
        assertEquals(
            listOf("México" to 1, "Reino Unido" to 1, "Venezuela" to 1),
            counts.issuer.issuers(),
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
        assertEquals(1, counts.year.of(YearFilter.Undated))
        assertEquals(1, counts.weight.of(GramBand.Unweighed))
    }

    @Test
    fun `a year chip leaves only that year, and the chips list newest first`() {
        val shelf = CoinsShelf(year = YearFilter.Of(1929))
        val counts = coinsFacetCounts(rows, CoinsShelf(), "")

        assertEquals(listOf(ShelfFixtures.FUERTE), shelf.narrow(rows, "").map { it.typeId })
        assertEquals(
            listOf(
                YearFilter.Of(2020) to 1,
                YearFilter.Of(1978) to 1,
                YearFilter.Of(1929) to 1,
                YearFilter.Undated to 1,
            ),
            counts.year.years(),
        )
    }
}
