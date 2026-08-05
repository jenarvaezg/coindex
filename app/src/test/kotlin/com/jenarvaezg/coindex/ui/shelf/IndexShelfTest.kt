package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shelf of Collections, and the one thing it must not do: replace the order of ADR 0021 §6.
 *
 * The default entry is that comparator, applied in the domain, and every other sort is built on top
 * of the list it already produced — so a second definition of «the order of the index» never appears
 * in the UI layer.
 */
class IndexShelfTest {
    private val facts = indexFacts(ShelfFixtures.state)

    private fun names(shelf: IndexShelf, query: String = "") =
        shelf.narrow(facts, query).map { it.name }

    @Test
    fun `the default order is the domain's, untouched`() {
        assertEquals(
            listOf("Britannia", "Bolívar de Venezuela", "Las mexicanas"),
            names(IndexShelf()),
        )
    }

    @Test
    fun `alphabetical is Spanish alphabetical, so the accent does not go after Z`() {
        assertEquals(
            listOf("Bolívar de Venezuela", "Britannia", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.Alphabetical)),
        )
    }

    @Test
    fun `a box is unweighed rather than light, so it sits at the bottom of Mas pesadas`() {
        assertEquals(
            listOf("Britannia", "Bolívar de Venezuela", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.Heaviest)),
        )
    }

    @Test
    fun `Menos completas puts the ratios the other way round and leaves the box last`() {
        assertEquals(
            listOf("Bolívar de Venezuela", "Britannia", "Las mexicanas"),
            names(IndexShelf(sort = IndexSort.LeastComplete)),
        )
    }

    @Test
    fun `Alta mas reciente reads the highest Numista row id, which is all there is`() {
        // The box holds row 5, the Bolívar rows 1 and 2, Britannia row 9.
        assertEquals(
            listOf("Britannia", "Las mexicanas", "Bolívar de Venezuela"),
            names(IndexShelf(sort = IndexSort.RecentlyAdded)),
        )
    }

    @Test
    fun `the estado chips are the ratio and its absence, and nothing about curation`() {
        assertEquals(listOf("Britannia"), names(IndexShelf(status = PlateStatus.Complete)))
        assertEquals(
            listOf("Bolívar de Venezuela"),
            names(IndexShelf(status = PlateStatus.PartlyDone)),
        )
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(status = PlateStatus.NoPlate)))
    }

    @Test
    fun `a box has no series, so no series chip offers it`() {
        assertEquals(listOf("Bolívar de Venezuela"), names(IndexShelf(series = SeriesStatus.Closed)))
        assertEquals(listOf("Britannia"), names(IndexShelf(series = SeriesStatus.Open)))

        val counts = indexFacetCounts(facts, IndexShelf(), "")
        assertEquals(3, counts.series.total)
        assertEquals(2, counts.series.byValue.values.sum())
    }

    @Test
    fun `where a collection starts is its earliest coin, not its catalog`() {
        assertEquals(
            listOf("Bolívar de Venezuela"),
            names(IndexShelf(startsIn = StartBand.BeforeFifty)),
        )
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(startsIn = StartBand.FiftyToNinetyNine)))
        assertEquals(listOf("Britannia"), names(IndexShelf(startsIn = StartBand.SinceTwoThousand)))
    }

    @Test
    fun `the search finds a card by its variant as well as by its name`() {
        assertEquals(listOf("Bolívar de Venezuela"), names(IndexShelf(), query = "bolivar"))
        assertEquals(listOf("Britannia"), names(IndexShelf(), query = "bullion"))
    }

    @Test
    fun `a facet counts with its own choice dropped`() {
        val counts = indexFacetCounts(facts, IndexShelf(status = PlateStatus.Complete), "")

        assertEquals(3, counts.status.total)
        assertEquals(1, counts.status.of(PlateStatus.NoPlate))
        // Every other facet sees only the one card the shelf leaves.
        assertEquals(1, counts.issuer.total)
        assertEquals(listOf("Reino Unido" to 1), counts.issuers())
    }

    @Test
    fun `the country of a box is the country of its pieces, and it filters like any other`() {
        assertEquals(listOf("Las mexicanas"), names(IndexShelf(issuer = "México")))
    }

    /**
     * The same invariant Coins has: the chips of a facet add up to its total, so no card is
     * unreachable. «Serie» is the deliberate exception — a box declares no series, and inventing one
     * for it would be the word of provenance ADR 0021 §2 removed, said in a chip.
     */
    @Test
    fun `every chip row adds up to its own total, except the one a box cannot answer`() {
        val counts = indexFacetCounts(facts, IndexShelf(), "")

        assertEquals(counts.weight.total, counts.weight.byValue.values.sum())
        assertEquals(counts.startsIn.total, counts.startsIn.byValue.values.sum())
        assertEquals(counts.status.total, counts.status.byValue.values.sum())
        assertEquals(1, counts.weight.of(OunceBand.Spanning))
        assertEquals(counts.series.total - 1, counts.series.byValue.values.sum())
    }
}
