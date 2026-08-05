package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The folded shelf's own line, which is the only thing on screen that says a filter is on.
 *
 * The shelf enters folded (ADR 0021 §1) and the filters survive a launch, so this line carries the
 * whole weight of «why is half my collection missing?» — a summary that said «Filtros y orden» with
 * a country selected would be the failure the persisted search text was rejected for.
 */
class ShelfLabelsTest {
    /** Both sides carry a sort (ADR 0021 §1), so both resting lines say the same thing. */
    @Test
    fun `an untouched shelf says what it is, not that nothing is on`() {
        assertEquals("Filtros y orden", indexShelfSummary(IndexShelf()))
        assertEquals("Filtros y orden", coinsShelfSummary(CoinsShelf()))
    }

    @Test
    fun `a filter that is on is counted out loud`() {
        assertEquals("1 filtro", indexShelfSummary(IndexShelf(issuer = "Venezuela")))
        assertEquals(
            "2 filtros",
            indexShelfSummary(IndexShelf(issuer = "Venezuela", series = SeriesStatus.Closed)),
        )
        assertEquals("1 filtro", coinsShelfSummary(CoinsShelf(membership = Membership.InNone)))
    }

    @Test
    fun `the sort of Coins is named on the same terms`() {
        assertEquals(
            "orden más pesadas",
            coinsShelfSummary(CoinsShelf(sort = CoinSort.Heaviest)),
        )
        assertEquals(
            "1 filtro · orden alfabético",
            coinsShelfSummary(
                CoinsShelf(sort = CoinSort.Alphabetical, membership = Membership.InNone),
            ),
        )
        assertEquals("Filtros y orden", coinsShelfSummary(CoinsShelf(sort = CoinSort.ByCountry)))
    }

    @Test
    fun `the sort is named only when it is not the one the index would have used anyway`() {
        assertEquals(
            "orden alta más reciente",
            indexShelfSummary(IndexShelf(sort = IndexSort.RecentlyAdded)),
        )
        assertEquals(
            "1 filtro · orden alfabético",
            indexShelfSummary(IndexShelf(issuer = "Venezuela", sort = IndexSort.Alphabetical)),
        )
        // ADR 0021 §6's own comparator: choosing it on purpose is not a deviation to announce.
        assertEquals("Filtros y orden", indexShelfSummary(IndexShelf(sort = IndexSort.MostComplete)))
    }

    @Test
    fun `the tally says N de M only while something is narrowed`() {
        assertEquals("58 colecciones", indexTally(58, 58))
        assertEquals("5 de 58 colecciones", indexTally(5, 58))
        assertEquals("1 colección", indexTally(1, 1))
        assertEquals("191 tipos", coinsTally(191, 191))
        assertEquals("6 de 191 tipos", coinsTally(6, 191))
    }
}
