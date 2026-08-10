package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The year axis: three states, Gregorian placement, no Hijri stretch (ADR 0026 §9).
 */
class YearAxisTest {
    @Test
    fun `Hijri engraved years land on their Gregorian twins and do not stretch the arc`() {
        val model = yearAxis(
            state = state(
                items = listOf(
                    CollectedItem(
                        id = 1,
                        quantity = 1,
                        typeId = DIRHAM,
                        issueYear = 1316,
                        gregorianYear = 1899,
                    ),
                    CollectedItem(
                        id = 2,
                        quantity = 1,
                        typeId = QIRSH,
                        issueYear = 1375,
                        gregorianYear = 1956,
                    ),
                    item(3, MODERN, year = 1960),
                ),
                typeMeta = mapOf(
                    DIRHAM to meta(DIRHAM, 1899),
                    QIRSH to meta(QIRSH, 1956),
                    MODERN to meta(MODERN, 1960),
                ),
                evidenced = emptySet(),
            ),
            catalogs = emptyList(),
        )

        assertEquals(1899, model.cells.first().year)
        assertEquals(1960, model.cells.last().year)
        assertTrue(model.totalYears < 200, "Hijri years must not stretch the axis to 711 years")
        assertIs<YearCellState.Coin>(model.cells.first { it.year == 1899 }.state)
        assertIs<YearCellState.Coin>(model.cells.first { it.year == 1956 }.state)
    }

    @Test
    fun `a year the plate names and the collector lacks is a ghost, an unnamed gap is bare cardboard`() {
        val model = yearAxis(
            state = state(
                items = listOf(item(1, TYPE_A, year = 1876)),
                typeMeta = mapOf(TYPE_A to meta(TYPE_A, 1876), TYPE_B to meta(TYPE_B, 1878)),
                evidenced = setOf("fuertes"),
            ),
            catalogs = listOf(
                dateRun(
                    id = "fuertes",
                    issuer = "venezuela",
                    typeId = TYPE_A,
                    years = 1876..1878,
                ),
            ),
        )

        assertEquals(1876, model.cells.first().year)
        assertEquals(1878, model.cells.last().year)
        assertIs<YearCellState.Coin>(model.cells[0].state) // 1876 owned
        assertIs<YearCellState.Ghost>(model.cells[1].state) // 1877 named, missing
        assertIs<YearCellState.Ghost>(model.cells[2].state) // 1878 named, missing
    }

    @Test
    fun `bare cardboard fills years nobody names between the first and last`() {
        val model = yearAxis(
            state = state(
                items = listOf(
                    item(1, TYPE_A, year = 1813),
                    item(2, TYPE_B, year = 1876),
                ),
                typeMeta = mapOf(TYPE_A to meta(TYPE_A, 1813), TYPE_B to meta(TYPE_B, 1876)),
                evidenced = emptySet(),
            ),
            catalogs = emptyList(),
        )

        assertEquals(1813, model.cells.first().year)
        assertEquals(1876, model.cells.last().year)
        assertEquals(2, model.ownedYears)
        assertEquals(1876 - 1813 + 1, model.totalYears)
        assertIs<YearCellState.Bare>(model.cells.first { it.year == 1850 }.state)
    }

    @Test
    fun `an undated piece inherits the type minimum so it still paints a coin`() {
        val model = yearAxis(
            state = state(
                items = listOf(
                    CollectedItem(id = 1, quantity = 1, typeId = TYPE_A, issueYear = null),
                ),
                typeMeta = mapOf(TYPE_A to meta(TYPE_A, 1929)),
                evidenced = emptySet(),
            ),
            catalogs = emptyList(),
        )

        assertEquals(1, model.ownedYears)
        assertIs<YearCellState.Coin>(model.cells.single().state)
        assertEquals(1929, model.cells.single().year)
    }

    @Test
    fun `a year the collector owns is never painted empty even when no plate names it`() {
        // The prototype bug: eleven owned years painted as empty because placement read the
        // wrong year. Here an owned year with no slot must still be Coin, not Bare.
        val model = yearAxis(
            state = state(
                items = listOf(item(1, TYPE_A, year = 1790)),
                typeMeta = mapOf(TYPE_A to meta(TYPE_A, 1790)),
                evidenced = setOf("later"),
            ),
            catalogs = listOf(
                dateRun(id = "later", issuer = "france", typeId = TYPE_B, years = 1876..1878),
            ),
        )

        // 1790 is owned and outside the plate; it opens the arc and must be Coin.
        assertEquals(1790, model.cells.first().year)
        assertIs<YearCellState.Coin>(model.cells.first().state)
    }

    private fun state(
        items: List<CollectedItem>,
        typeMeta: Map<Int, TypeMeta>,
        evidenced: Set<String>,
    ) = CollectionState(
        AssembledCollection(
            items = items,
            typeMeta = typeMeta,
            evidencedCatalogIds = evidenced,
        ),
    )

    private fun item(id: Long, typeId: Int, year: Int) = CollectedItem(
        id = id,
        quantity = 1,
        typeId = typeId,
        issueYear = year,
    )

    private fun meta(id: Int, minYear: Int) = TypeMeta(id = id, minYear = minYear)

    private fun dateRun(
        id: String,
        issuer: String,
        typeId: Int,
        years: IntRange,
    ) = CollectionCatalog(
        schemaVersion = 2,
        id = id,
        name = id,
        shortName = id,
        family = id,
        weightMillioz = 804,
        finish = null,
        metal = Metal.Silver,
        issuerCode = issuer,
        seriesStatus = SeriesStatus.Closed,
        source = "test",
        updatedAt = "2026-08-10",
        members = years.map { year ->
            CollectionCatalogMember(
                id = "$year",
                label = "$year",
                year = year,
                numistaTypeId = typeId,
            )
        },
    )

    companion object {
        private const val DIRHAM = 1
        private const val QIRSH = 2
        private const val MODERN = 3
        private const val TYPE_A = 10
        private const val TYPE_B = 11
    }
}
