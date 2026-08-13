package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The folded shelf's own line, which is the only thing on screen that says a filter is on.
 *
 * The shelf enters folded (ADR 0021 §1) and the filters survive a launch, so this line carries the
 * whole weight of «why is half my collection missing?» — a summary that said «Filtros y orden» with
 * a country selected would be the failure the persisted search text was rejected for.
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

    /**
     * Counted **and named** (#414): «1 filtro» sent the collector to open the shelf and scan a wall
     * of chips for the green one, which on the year facet is a calendar of them.
     */
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

    /** In the order the shelf paints the rows, so the line and the open shelf read downwards alike. */
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

    /**
     * The two chips that cannot be read away from their eyebrow, for opposite reasons: «Cerrada» is
     * an adjective on its own, and «Monedas» is the name of the screen it would be printed on.
     */
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
     * Named while open too, which is where the axis stays quiet (atlas-315).
     *
     * The axis is one chip in the first row of an opened shelf, so opening it is enough to see it.
     * A chosen year is eight rows down behind a calendar of chips — the walk #414 is about — so the
     * line keeps saying which one it is for as long as it is on.
     */
    @Test
    fun `the filters are named whether the shelf is open or folded`() {
        assertEquals(
            "1 filtro · Año 1960",
            coinsShelfSummary(CoinsShelf(year = YearFilter.Of(1960)), expanded = true),
        )
    }

    /**
     * A chip borrows its facet's noun only when it does not name itself (#414).
     *
     * Chip labels are written to be read under their facet's eyebrow: «1960» and «10 – 25 g» say
     * what they are only because «Año» and «Peso» are printed above them, and out on the folded line
     * there is no eyebrow. «Sin colección» and «Antes de 1950» carry their own noun already, and
     * «Colección Sin colección» would be the same mistake in the other direction.
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
            "1 filtro · Conjunto o caja",
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
        // ADR 0021 §6's own comparator: choosing it on purpose is not a deviation to announce.
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

    /**
     * The country axis names its unit, like the other two (#416).
     *
     * Changing the axis changes the magnitude under the same rótulo — «70 colecciones» becomes a
     * count of casillas across the sheet — and a bare «170/678» beside «Exportar láminas» left the
     * collector to guess which two things had been divided. The shape is the year axis's, so the
     * three axes say the same sentence about three different units.
     */
    @Test
    fun `the country-axis tally says N de M casillas`() {
        assertEquals("170 de 678 casillas", countryAxisTally(170, 678))
        assertEquals("678 casillas", countryAxisTally(678, 678))
        assertEquals("1 casilla", countryAxisTally(1, 1))
    }

    /**
     * No axis prints a cifra pelada: whatever a tally counts, the tally names (#416).
     *
     * The guard is the last character rather than the wording: a fraction that stops at a digit is
     * exactly the failure, and any noun at all answers it.
     */
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

    /**
     * Three questions and not one: reading the collection off the database takes a frame or two, and
     * «todavía no hay colecciones» in that gap is a lie about a collection already on the device.
     */
    @Test
    fun `an empty index says which of the three cases it is`() {
        assertEquals(
            "Leyendo tu colección…",
            indexEmptyLabel(loading = true, anyCollections = false),
        )
        assertEquals(
            "Ninguna colección pasa por lo que has puesto.",
            indexEmptyLabel(loading = false, anyCollections = true),
        )
        assertEquals(
            "Todavía no hay colecciones. Sincroniza para traer tu colección de Numista.",
            indexEmptyLabel(loading = false, anyCollections = false),
        )
    }

    /** The other side has nothing to read off the database first, so it has two cases and not three. */
    @Test
    fun `an empty Coins tells a filter from an empty collection`() {
        assertEquals("Ninguna moneda pasa por lo que has puesto.", coinsEmptyLabel(anyCoins = true))
        assertEquals(
            "Todavía no hay monedas. Sincroniza para traer tu colección de Numista.",
            coinsEmptyLabel(anyCoins = false),
        )
    }

    /** Loading wins over the filter: a shelf cannot have hidden what has not been read yet. */
    @Test
    fun `the loading gap is never reported as a filter`() {
        assertEquals(
            indexEmptyLabel(loading = true, anyCollections = false),
            indexEmptyLabel(loading = true, anyCollections = true),
        )
    }
}
