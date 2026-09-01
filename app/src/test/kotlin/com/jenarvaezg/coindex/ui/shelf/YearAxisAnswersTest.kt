package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AlbumSlot
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The invariant of #550: every seat of the year axis that opens Monedas finds something there.
 *
 * The axis places a piece by [com.jenarvaezg.coindex.domain.placementYear] and paints a ghost on
 * every year a plate names, but the shelf answered only the engraved years of the pieces owned —
 * so a seat could paint a coin whose own tap opened an empty page. Measured on the father's
 * collection before the fix: nine pieces whose Gregorian year differs from the engraved one, and
 * 1899, 1947, 1955 and 1956 painting a coin nobody could reach.
 */
class YearAxisAnswersTest {
    /**
     * The 100 pesetas «*67»: engraved 1966, struck 1967, placed on 1967 by the axis.
     *
     * The card keeps saying 1966 — that is what is on the coin (ADR 0016) — and the seat that
     * paints it now finds it too.
     */
    @Test
    fun `a piece placed by its Gregorian year answers to both years`() {
        val state = state(
            items = listOf(
                CollectedItem(
                    id = 1,
                    quantity = 1,
                    typeId = PESETAS,
                    issueYear = 1_966,
                    gregorianYear = 1_967,
                ),
            ),
            typeMeta = mapOf(PESETAS to TypeMeta(id = PESETAS, title = "100 Pesetas", minYear = 1_966)),
        )

        val row = coinRows(state, slotYears(state)).single()

        assertEquals(listOf(1_966), row.years, "the cartouche says what the coin says")
        assertTrue(YearFilter.Of(1_966) in row.yearFilters)
        assertTrue(YearFilter.Of(1_967) in row.yearFilters, "the seat that paints it must find it")
        assertTrue(matchesYear(row, 1_967))
    }

    /**
     * A ghost of an evidenced plate finds the versions of its type the collector does own.
     *
     * The father's 2 bolívares: he holds 1879 and the plate names 1900 and 1901 as holes. Tapping
     * the hole used to open nothing at all; it now opens the coin the hole is about.
     */
    @Test
    fun `a hole of an evidenced plate answers with the type it is a hole of`() {
        val state = state(
            items = listOf(piece(id = 1, typeId = BOLIVARES, year = 1_879)),
            typeMeta = mapOf(BOLIVARES to TypeMeta(id = BOLIVARES, title = "2 Bolívares", minYear = 1_879)),
            slots = dateRun(RUN, BOLIVARES, listOf(1_879, 1_900, 1_901), owned = setOf(1_879)),
        )

        val row = coinRows(state, slotYears(state)).single()

        assertTrue(matchesYear(row, 1_900))
        assertTrue(matchesYear(row, 1_901))
        assertEquals(listOf(1_879), row.years, "a hole is not a coin the collector holds")
    }

    /** Chips and predicate come from the same list, so the ghost's year is a chip that counts one. */
    @Test
    fun `the year of a hole is a chip of its own`() {
        val state = state(
            items = listOf(piece(id = 1, typeId = BOLIVARES, year = 1_879)),
            typeMeta = mapOf(BOLIVARES to TypeMeta(id = BOLIVARES, minYear = 1_879)),
            slots = dateRun(RUN, BOLIVARES, listOf(1_879, 1_900), owned = setOf(1_879)),
        )

        val rows = coinRows(state, slotYears(state))
        val counts = coinsFacetCounts(rows, CoinsShelf(), query = "").year

        assertEquals(1, counts.populated().single { it.first == YearFilter.Of(1_900) }.second)
        assertTrue(matchesQueryYear(rows.single(), "1900"), "search keeps parity with the chips")
    }

    /**
     * The explicit restriction of #550: the ficha's run is **not** a filter criterion.
     *
     * The Maria Theresa Thaler is a posthumous restrike, ficha 1780–2024. Answering to its 245
     * years would put it under almost every seat of the axis, and the shelf's year chips would go
     * from ~230 to more than 800. What answers is the curated knowledge — the casillas.
     */
    @Test
    fun `a type whose ficha spans centuries does not answer to the years in between`() {
        val state = state(
            items = listOf(piece(id = 1, typeId = THALER, year = 1_780)),
            typeMeta = mapOf(
                THALER to TypeMeta(id = THALER, title = "1 Thaler", minYear = 1_780, maxYear = 2_024),
            ),
            slots = dateRun(RESTRIKE, THALER, listOf(1_780), owned = setOf(1_780)),
        )

        val row = coinRows(state, slotYears(state)).single()

        assertTrue(matchesYear(row, 1_780))
        assertFalse(matchesYear(row, 1_900))
        assertFalse(matchesYear(row, 2_024))
        assertEquals(1, row.yearFilters.size)
    }

    /** «Sin año» is not replaced by what the axis adds: an undated medal keeps its chip. */
    @Test
    fun `an undated piece still answers to Sin año`() {
        val state = state(
            items = listOf(piece(id = 1, typeId = MEDAL, year = null)),
            typeMeta = emptyMap(),
        )

        val row = coinRows(state, slotYears(state)).single()

        assertEquals(listOf(YearFilter.Undated), row.yearFilters)
    }

    /**
     * The one paseo: what the axis paints as a ghost is what the shelf answers to.
     *
     * Both surfaces read the same [slotYears], so a plate cannot name a year on one screen and be
     * unknown to the other one tap later.
     */
    @Test
    fun `every ghost of the axis is answered by the coin whose hole it is`() {
        val state = state(
            items = listOf(piece(id = 1, typeId = BOLIVARES, year = 1_879)),
            typeMeta = mapOf(BOLIVARES to TypeMeta(id = BOLIVARES, minYear = 1_879)),
            slots = dateRun(RUN, BOLIVARES, listOf(1_879, 1_900, 1_901), owned = setOf(1_879)),
        )
        val rows = coinRows(state, slotYears(state))

        val ghosts = yearAxis(state).cells
            .filter { it.state == YearCellState.Ghost }
            .map { it.year }

        assertEquals(listOf(1_900, 1_901), ghosts)
        for (year in ghosts) {
            assertTrue(
                rows.any { matchesYear(it, year) },
                "the ghost of $year opens a page with the coin it is a hole of",
            )
        }
    }

    private fun matchesYear(row: CoinRow, year: Int): Boolean =
        CoinsShelf(year = YearFilter.Of(year)).narrow(listOf(row), query = "").isNotEmpty()

    private fun matchesQueryYear(row: CoinRow, query: String): Boolean =
        CoinsShelf().narrow(listOf(row), query).isNotEmpty()

    private fun state(
        items: List<CollectedItem>,
        typeMeta: Map<Int, TypeMeta>,
        slots: List<AlbumSlot> = emptyList(),
    ) = CollectionState(
        // The casillas the assembly carries (#538): what this walk indexes is what the plate draws.
        AssembledCollection(items = items, typeMeta = typeMeta, slots = slots),
    )

    private fun piece(id: Long, typeId: Int, year: Int?) =
        CollectedItem(id = id, quantity = 1, typeId = typeId, issueYear = year)

    /** The casillas of a date run as the assembly hands them over (#538). */
    private fun dateRun(
        catalogId: String,
        typeId: Int,
        years: List<Int>,
        owned: Set<Int> = emptySet(),
    ) = years.map { year ->
        AlbumSlot(
            catalogId = catalogId,
            memberId = "$year",
            typeId = typeId,
            owned = year in owned,
            quantity = if (year in owned) 1 else 0,
            country = "Venezuela",
            year = year,
        )
    }

    companion object {
        private const val PESETAS = 10
        private const val BOLIVARES = 11
        private const val THALER = 12
        private const val MEDAL = 13
        private const val RUN = "bolivares"
        private const val RESTRIKE = "thaler"
    }
}
