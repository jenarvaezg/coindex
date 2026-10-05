package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.ShowcaseLabels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The folded shelf's summary line. The shelf enters folded and its filters survive a launch
 * (ADR 0021 §1), so this line is the only place that says a filter is on.
 */
class ShelfLabelsTest {
    @Test
    fun `the disclosure mark says whether the shelf is open`() {
        assertEquals("▸ ", shelfDisclosure(expanded = false))
        assertEquals("▾ ", shelfDisclosure(expanded = true))
    }

    /** Both sides carry a sort (ADR 0021 §1), so both resting lines say the same thing. */
    @Test
    fun `an untouched shelf says what it is, not that nothing is on`() {
        assertEquals("Filtros y orden", indexShelfSummary(IndexShelf()))
        assertEquals("Filtros y orden", coinsShelfSummary(CoinsShelf()))
    }

    /** Named as well as counted (#414): a bare «1 filtro» meant scanning every chip for it. */
    @Test
    fun `a filter that is on is counted out loud and said out loud`() {
        assertEquals("1 filtro · Venezuela", indexShelfSummary(IndexShelf(issuer = "Venezuela")))
        assertEquals(
            "1 filtro · Año 1960",
            coinsShelfSummary(CoinsShelf(year = YearFilter.Of(1960))),
        )
        assertEquals(
            "1 filtro · Sin colección",
            coinsShelfSummary(CoinsShelf(membership = Membership.InNone)),
        )
    }

    @Test
    fun `every filter that is on is named, in the order of the chip rows`() {
        assertEquals(
            "2 filtros · Venezuela · Serie Cerrada",
            indexShelfSummary(IndexShelf(issuer = "Venezuela", series = SeriesStatus.Closed)),
        )
        assertEquals(
            "2 filtros · Peso 10 – 25 g · Clase Monedas",
            coinsShelfSummary(
                CoinsShelf(weight = GramBand.TenToTwentyFive, objectClass = ObjectClass.Coin),
            ),
        )
    }

    /** Alone, «Cerrada» is just an adjective and «Monedas» is the name of the screen itself. */
    @Test
    fun `a chip that would be read as something else keeps its facet`() {
        assertEquals(
            "1 filtro · Serie Abierta",
            indexShelfSummary(IndexShelf(series = SeriesStatus.Open)),
        )
        assertEquals(
            "1 filtro · Clase Medallas y fichas",
            coinsShelfSummary(CoinsShelf(objectClass = ObjectClass.Exonumia)),
        )
    }

    /**
     * Unlike the axis (atlas-315): a chosen year sits rows down behind a calendar of chips, so the
     * line names it even while the shelf is open (#414).
     */
    @Test
    fun `the filters are named whether the shelf is open or folded`() {
        assertEquals(
            "1 filtro · Año 1960",
            coinsShelfSummary(CoinsShelf(year = YearFilter.Of(1960)), expanded = true),
        )
    }

    /**
     * Chip labels lean on the facet heading above them: on the line «1960» needs «Año», while
     * «Sin colección» and «Antes de 1950» already carry their noun (#414).
     */
    @Test
    fun `a chip that names itself is not made to say its facet twice`() {
        assertEquals(
            "1 filtro · Antes de 1950",
            indexShelfSummary(IndexShelf(startsIn = StartBand.BeforeFifty)),
        )
        assertEquals(
            "1 filtro · Empieza en 1950 – 1999",
            indexShelfSummary(IndexShelf(startsIn = StartBand.FiftyToNinetyNine)),
        )
        assertEquals(
            "1 filtro · Varias onzas",
            indexShelfSummary(IndexShelf(weight = OunceBand.Spanning)),
        )
        assertEquals("1 filtro · Sin peso", coinsShelfSummary(CoinsShelf(weight = GramBand.Unweighed)))
        assertEquals("1 filtro · Sin año", coinsShelfSummary(CoinsShelf(year = YearFilter.Undated)))
        assertEquals(
            "1 filtro · Sin lámina",
            indexShelfSummary(IndexShelf(status = PlateStatus.NoPlate)),
        )
        assertEquals(
            "1 filtro · A medias",
            indexShelfSummary(IndexShelf(status = PlateStatus.PartlyDone)),
        )
    }

    @Test
    fun `the sort of Coins is named on the same terms`() {
        assertEquals(
            "orden más pesadas",
            coinsShelfSummary(CoinsShelf(sort = CoinSort.Heaviest)),
        )
        assertEquals(
            "1 filtro · Sin colección · orden alfabético",
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
            "1 filtro · Venezuela · orden alfabético",
            indexShelfSummary(IndexShelf(issuer = "Venezuela", sort = IndexSort.Alphabetical)),
        )
        // The index's default comparator (ADR 0021 §6), so there is nothing to announce.
        assertEquals("Filtros y orden", indexShelfSummary(IndexShelf(sort = IndexSort.MostComplete)))
    }

    @Test
    fun `the axis is named only while folded and only when it is not por lamina`() {
        assertEquals("Eje País", indexShelfSummary(IndexShelf(axis = NotebookAxis.ByCountry)))
        assertEquals("Eje Año", indexShelfSummary(IndexShelf(axis = NotebookAxis.ByYear)))
        assertEquals(
            "1 filtro · Italia · Eje País",
            indexShelfSummary(IndexShelf(axis = NotebookAxis.ByCountry, issuer = "Italia")),
        )
        // Open: the chip is in view, so the line stays quiet about the axis (atlas-315).
        assertEquals(
            "Filtros y orden",
            indexShelfSummary(IndexShelf(axis = NotebookAxis.ByCountry), expanded = true),
        )
        assertEquals("Eje Año", coinsShelfSummary(CoinsShelf(axis = NotebookAxis.ByYear)))
        assertEquals(
            "Filtros y orden",
            coinsShelfSummary(CoinsShelf(axis = NotebookAxis.ByYear), expanded = true),
        )
    }

    @Test
    fun `the year-axis tally says N de M años`() {
        assertEquals("93 de 112 años", yearAxisTally(93, 112))
        assertEquals("112 años", yearAxisTally(112, 112))
    }

    /** Changing the axis changes the unit under the same label, so the tally names it (#416). */
    @Test
    fun `the country-axis tally says N de M casillas`() {
        assertEquals("170 de 678 casillas", countryAxisTally(170, 678))
        assertEquals("678 casillas", countryAxisTally(678, 678))
        assertEquals("1 casilla", countryAxisTally(1, 1))
    }

    /** Checks the last character rather than the wording: any noun will do (#416). */
    @Test
    fun `every axis tally ends in what it counted`() {
        val tallies = listOf(
            indexTally(5, 58),
            indexTally(58, 58),
            countryAxisTally(170, 678),
            countryAxisTally(678, 678),
            yearAxisTally(93, 112),
            yearAxisTally(112, 112),
        )
        for (tally in tallies) {
            assertTrue(tally.last().isLetter(), "«$tally» acaba en cifra y no dice qué cuenta")
        }
    }

    @Test
    fun `a year seat says ×N only when more than one piece lands there`() {
        assertEquals(null, yearAxisQuantityMark(1))
        assertEquals("×2", yearAxisQuantityMark(2))
        assertEquals("×12", yearAxisQuantityMark(12))
    }

    @Test
    fun `the tally says N de M only while something is narrowed`() {
        assertEquals("58 colecciones", indexTally(58, 58))
        assertEquals("5 de 58 colecciones", indexTally(5, 58))
        assertEquals("1 colección", indexTally(1, 1))
        assertEquals("191 tipos", coinsTally(191, 191))
        assertEquals("6 de 191 tipos", coinsTally(6, 191))
    }

    /** Reading the database takes a frame or two; «todavía no hay» in that gap would be false. */
    @Test
    fun `an empty index says which of the three cases it is`() {
        assertEquals(
            "Leyendo tu colección…",
            indexEmptyLabel(loading = true, anyCollections = false, narrowing = ShelfNarrowing.Both),
        )
        assertEquals(
            "Ninguna colección pasa por lo que has puesto.",
            indexEmptyLabel(loading = false, anyCollections = true, narrowing = ShelfNarrowing.Both),
        )
        assertEquals(
            "Todavía no hay colecciones. Sincroniza para traer tu colección de Numista.",
            indexEmptyLabel(
                loading = false,
                anyCollections = false,
                narrowing = ShelfNarrowing.None,
            ),
        )
    }

    /** Coins has no loading case: it has nothing to read off the database first. */
    @Test
    fun `an empty Coins tells a filter from an empty collection`() {
        assertEquals(
            "Ninguna moneda pasa por lo que has puesto.",
            coinsEmptyLabel(anyCoins = true, narrowing = ShelfNarrowing.Both),
        )
        assertEquals(
            "Todavía no hay monedas. Sincroniza para traer tu colección de Numista.",
            coinsEmptyLabel(anyCoins = false, narrowing = ShelfNarrowing.None),
        )
    }

    @Test
    fun `the loading gap is never reported as a filter`() {
        assertEquals(
            indexEmptyLabel(loading = true, anyCollections = false, narrowing = ShelfNarrowing.Both),
            indexEmptyLabel(loading = true, anyCollections = true, narrowing = ShelfNarrowing.Both),
        )
    }

    /**
     * Chips persist across launches and the query does not (ADR 0021 §1), so they are told apart
     * (#515). A blank query narrows nothing.
     */
    @Test
    fun `the narrowing is the chips, the word, both or neither`() {
        assertEquals(ShelfNarrowing.None, shelfNarrowing(filters = 0, query = ""))
        assertEquals(ShelfNarrowing.None, shelfNarrowing(filters = 0, query = "   "))
        assertEquals(ShelfNarrowing.Filters, shelfNarrowing(filters = 2, query = ""))
        assertEquals(ShelfNarrowing.Search, shelfNarrowing(filters = 0, query = "panda"))
        assertEquals(ShelfNarrowing.Both, shelfNarrowing(filters = 1, query = "panda"))
    }

    /** Each narrowing has its own verb (#515): a card «pasa por» a chip, «responde a» a query. */
    @Test
    fun `an empty shelf names the narrowing that emptied it`() {
        assertEquals(
            "Ninguna colección pasa por los filtros.",
            indexEmptyLabel(false, anyCollections = true, narrowing = ShelfNarrowing.Filters),
        )
        assertEquals(
            "Ninguna colección responde a lo que has escrito.",
            indexEmptyLabel(false, anyCollections = true, narrowing = ShelfNarrowing.Search),
        )
        assertEquals(
            "Ninguna moneda responde a lo que has escrito.",
            coinsEmptyLabel(anyCoins = true, narrowing = ShelfNarrowing.Search),
        )
    }

    /** The country and year axes can be empty with nothing narrowing them. */
    @Test
    fun `an axis with nothing on it blames no filter and offers no way out`() {
        assertEquals(
            "Ninguna colección aparece en este eje.",
            indexEmptyLabel(false, anyCollections = true, narrowing = ShelfNarrowing.None),
        )
        assertNull(clearNarrowingAction(ShelfNarrowing.None))
    }

    @Test
    fun `the way out is named after what it undoes`() {
        assertEquals("Quitar los filtros", clearNarrowingAction(ShelfNarrowing.Filters))
        assertEquals(SEARCH_CLEAR_LABEL, clearNarrowingAction(ShelfNarrowing.Search))
        assertEquals("Quitar los filtros y la búsqueda", clearNarrowingAction(ShelfNarrowing.Both))
    }

    /** «tus» marks what the collector has; the «Explorar» window holds what they don't (#515). */
    @Test
    fun `every search box says what it searches`() {
        assertEquals("Buscar entre tus colecciones", INDEX_SEARCH_PLACEHOLDER)
        assertEquals("Buscar entre tus monedas", COINS_SEARCH_PLACEHOLDER)
        assertEquals("Buscar entre las láminas", ShowcaseLabels.SEARCH_PLACEHOLDER)
        for (placeholder in listOf(
            INDEX_SEARCH_PLACEHOLDER,
            COINS_SEARCH_PLACEHOLDER,
            ShowcaseLabels.SEARCH_PLACEHOLDER,
        )) {
            assertTrue(placeholder.startsWith("Buscar entre "), placeholder)
        }
    }
}
