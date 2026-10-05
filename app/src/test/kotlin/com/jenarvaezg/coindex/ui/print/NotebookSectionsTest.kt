package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.data.SHIPPED_CURATION
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.PlateValue
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.WishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A day in June 2026: a price from the quarterly pass, read in October (#561). */
private const val VALUED = 1_782_000_000_000L

/**
 * What a notebook section prints. Footnotes are pinned on the drawn cell, not on
 * `emissionLabelFor` (#225): the label was an optional parameter a drawer could forget.
 */
class NotebookSectionsTest {
    private val catalogs: List<CollectionCatalog> = SHIPPED_CURATION.catalogs

    /** The five stars of the 100 pesetas 1966 (N#1885), told apart only by their emission. */
    private val stars = listOf(
        CollectedItem(id = 1, quantity = 1, typeId = 1_885, issueYear = 1966, issueId = 8_508),
        CollectedItem(id = 2, quantity = 1, typeId = 1_885, issueYear = 1966, issueId = 33_204),
        CollectedItem(id = 3, quantity = 1, typeId = 1_885, issueYear = 1966, issueId = 33_205),
        CollectedItem(id = 4, quantity = 1, typeId = 1_885, issueYear = 1966, issueId = 33_206),
        CollectedItem(id = 5, quantity = 1, typeId = 1_885, issueYear = 1966, issueId = 33_207),
    )

    private fun footnotesOf(items: List<CollectedItem>): List<String?> {
        val curation = Curation(catalogs)
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = items,
                ownGroupings = listOf(
                    OwnGrouping(
                        id = 1,
                        name = "Los paquillos de mi padre",
                        typeIds = items.map { it.typeId }.distinct(),
                    ),
                ),
            ),
        )
        val box = assembled.index.filterIsInstance<IndexCard.Box>().single()
        return notebookSections(
            CollectionState(assembled),
            listOf(box),
            emptyList(),
            curation,
            NotebookOptions(),
        ).single().cells.map { it.footnote }
    }

    @Test
    fun `each star is named by its emission where the year names nothing`() {
        assertEquals(
            listOf(
                "Estrella 66 · Numista 1885",
                "Estrella 67 · Numista 1885",
                "Estrella 68 · Numista 1885",
                "Estrella 69 · Numista 1885",
                "Estrella 70 · Numista 1885",
            ),
            footnotesOf(stars),
        )
    }

    /** The emission label takes the year's place at the head of the same line. */
    @Test
    fun `a piece outside an issue run still leads with its year`() {
        val unclaimed = CollectedItem(id = 6, quantity = 2, typeId = 999_999, issueYear = 1994)

        assertEquals(
            listOf("Estrella 66 · Numista 1885", "1994 · Numista 999999 · ×2"),
            footnotesOf(listOf(stars.first(), unclaimed)),
        )
    }

    /**
     * Same count as the screen (#226): a card with no issued member owned yet arrives with the
     * ratio (ADR 0021 §7) and lands on a page of pieces instead of a plate.
     */
    @Test
    fun `a page of pieces counts the ratio the card arrived with`() {
        val francesas = DerivedCollection(
            family = "Monnaie de Paris",
            weightMillioz = 1_000,
            finish = Finish.Bullion,
            metal = Metal.Silver,
            distinctTypes = 3,
            quantity = 4,
        )
        val pieces = listOf(
            CollectedItem(id = 1, quantity = 2, typeId = 100, issueYear = 1996),
            CollectedItem(id = 2, quantity = 1, typeId = 101, issueYear = 1997),
            CollectedItem(id = 3, quantity = 1, typeId = 102, issueYear = 1998),
        )
        val card = IndexCard.Derived(
            name = "Las francesas",
            coverage = CoverageRatio(0, 12),
            issuer = "Francia",
            collection = francesas,
            plateCatalogId = null,
        )
        val state = CollectionState(AssembledCollection(itemsByKey = mapOf(card.key to pieces)))

        val section = notebookSections(
            state,
            listOf(card),
            emptyList(),
            Curation(catalogs),
            NotebookOptions(),
        ).single()

        assertEquals(
            listOf("País" to "Francia", "Piezas" to "0 de 12 · te faltan 12"),
            section.facts,
        )
    }

    /**
     * No country when the pieces disagree and no variant for a box (#431, #543): a blank row on
     * paper reads as something lost.
     */
    @Test
    fun `a page of pieces leaves unsaid what nothing can name`() {
        val item = CollectedItem(id = 1, quantity = 1, typeId = 100, issueYear = 2024)
        val card = IndexCard.Box(
            name = "Lo que fue cayendo",
            issuer = null,
            box = OwnGroupingView(
                OwnGrouping(1, "Lo que fue cayendo", typeIds = listOf(100)),
                listOf(item),
            ),
        )

        val section = notebookSections(
            CollectionState(AssembledCollection()),
            listOf(card),
            emptyList(),
            Curation(emptyList()),
            NotebookOptions(),
        ).single()

        assertEquals(listOf("Piezas" to "1 moneda · 1 tipo"), section.facts)
        assertNull(section.subtitle)
    }

    @Test
    fun `a notebook piece cell preserves the same two name ranges as the screen`() {
        val item = CollectedItem(id = 1, quantity = 1, typeId = 100, issueYear = 2024)
        val card = IndexCard.Box(
            name = "Dragones",
            issuer = "Reino Unido",
            box = OwnGroupingView(OwnGrouping(1, "Dragones", listOf(100)), listOf(item)),
        )
        val state = CollectionState(
            AssembledCollection(
                typeMeta = mapOf(
                    100 to TypeMeta(
                        id = 100,
                        title = "5 Pounds - Elizabeth II (Red Dragon of Wales; 2 oz Fine Silver)",
                    ),
                ),
            ),
        )

        val cell = notebookSections(
            state,
            listOf(card),
            emptyList(),
            Curation(emptyList()),
            NotebookOptions(),
        ).single().cells.single()

        assertEquals("5 Pounds", cell.name?.denomination)
        assertEquals("Red Dragon of Wales", cell.name?.theme)
    }

    /**
     * The stamp travels as its own bit, as on the PNG (#371, ADR 0026 §3, §4). The Progress row
     * stays, since paper has no header to raise the ratio into (ADR 0026 §5).
     */
    @Test
    fun `a complete plate section says so and keeps the progress row`() {
        val section = dateRunSection(ownedYears = listOf(1879, 1886))

        assertTrue(section.complete)
        assertEquals("2/2", section.ratio)
        assertEquals("Progreso" to "2 / 2 emisiones", section.facts.first())
    }

    @Test
    fun `an incomplete plate section carries no stamp`() {
        val section = dateRunSection(ownedYears = listOf(1879))

        assertFalse(section.complete)
        assertEquals("1/2", section.ratio)
        assertEquals("Progreso" to "1 / 2 emisiones", section.facts.first())
    }

    @Test
    fun `a pieces page never carries the completion stamp`() {
        val card = IndexCard.Box(
            name = "Dragones",
            issuer = "Reino Unido",
            box = OwnGroupingView(
                OwnGrouping(1, "Dragones", typeIds = listOf(100)),
                listOf(CollectedItem(id = 1, quantity = 1, typeId = 100, issueYear = 2024)),
            ),
        )

        val section = notebookSections(
            CollectionState(),
            listOf(card),
            emptyList(),
            Curation(emptyList()),
            NotebookOptions(),
        ).single()

        assertFalse(section.complete)
        assertEquals(null, section.ratio)
    }


    /** The printer is handed no amount at all, so no drawer can print one (#228, ADR 0021 §13). */
    @Test
    fun `with the money off no fact of the page carries an amount`() {
        val section = dateRunSection(listOf(1879), options = NotebookOptions(money = false))

        assertEquals(emptyList(), section.facts.filter { (label, _) -> label == "Valor" })
        assertTrue(section.facts.none { (_, value) -> "€" in value })
    }

    /** Paper has no header, so the plate's value joins its specification. */
    @Test
    fun `with the money on the plate prints what is in it`() {
        val section = dateRunSection(
            listOf(1879),
            options = NotebookOptions(money = true),
            plateValue = { PlateValue(eur = 54.0, pieces = 1) },
        )

        assertEquals(
            listOf("Valor" to "54 € · al mayor de tres precios"),
            section.facts.filter { it.first == "Valor" },
        )
        // With no catalogue price there is no day to print: the row is absent, not blank.
        assertTrue(section.facts.none { it.first == "Tasación" })
    }

    /**
     * Written out in full (#594): paper has no «hoy», and a catalog price lives ninety days
     * (ADR 0028 §5, amended by #561).
     */
    @Test
    fun `the printed value says the day it was priced`() {
        val section = dateRunSection(
            listOf(1879),
            options = NotebookOptions(money = true),
            plateValue = { PlateValue(eur = 54.0, pieces = 1, catalogReadAt = VALUED) },
        )

        assertEquals(
            listOf("Tasación" to printedValuationLabel(VALUED)),
            section.facts.filter { it.first == "Tasación" },
        )
    }

    @Test
    fun `with the money off the date of the valuation goes with the amount`() {
        val section = dateRunSection(listOf(1879), options = NotebookOptions(money = false))

        assertTrue(section.facts.none { it.first == "Tasación" })
    }

    /**
     * The mark is a state at rest, like the stamp (ADR 0026 §4, ADR 0029 §7). It uses the caption's
     * reserved line, so it moves no page count, and the money switch leaves it: it is no amount.
     */
    @Test
    fun `a marked casilla prints its mark and an owned one prints nothing`() {
        val section = dateRunSection(ownedYears = listOf(1879), wishedYears = listOf(1886))

        assertEquals(listOf(null, "lo busco"), section.cells.map { it.state })
        // With the money off, the mark stays.
        assertEquals(
            listOf(null, "lo busco"),
            dateRunSection(
                ownedYears = listOf(1879),
                options = NotebookOptions(money = false),
                wishedYears = listOf(1886),
            ).cells.map { it.state },
        )
        // Nothing marked, nothing printed.
        assertEquals(listOf(null, null), dateRunSection(ownedYears = listOf(1879)).cells.map { it.state })
    }

    /** A two-year date run, resolved as `resolvePlate` resolves production plates. */
    private fun dateRunSection(
        ownedYears: List<Int>,
        options: NotebookOptions = NotebookOptions(),
        plateValue: (PlateResult.Available) -> PlateValue? = { null },
        wishedYears: List<Int> = emptyList(),
    ): PrintSection {
        val typeId = 10_340
        val catalog = CollectionCatalog(
            schemaVersion = 2,
            id = "venezuela-fuertes-test",
            name = "Fuertes · Venezuela",
            shortName = "Fuertes",
            issuerCode = "venezuela",
            family = "Fuertes de Venezuela",
            weightMillioz = 804,
            finish = null,
            metal = Metal.Silver,
            seriesStatus = SeriesStatus.Closed,
            source = "https://en.numista.com/catalogue/pieces10340.html",
            updatedAt = "2026-08-01",
            members = listOf(
                CollectionCatalogMember(id = "1879", label = "1879", year = 1879, numistaTypeId = typeId),
                CollectionCatalogMember(id = "1886", label = "1886", year = 1886, numistaTypeId = typeId),
            ),
        )
        val key = catalog.key()
        val items = ownedYears.mapIndexed { index, year ->
            CollectedItem(id = index + 1L, quantity = 1, typeId = typeId, issueYear = year)
        }
        val card = IndexCard.Derived(
            name = catalog.name,
            coverage = CoverageRatio(owned = ownedYears.size, issued = 2),
            issuer = "Venezuela",
            collection = DerivedCollection(
                family = key.family,
                weightMillioz = key.weightMillioz,
                finish = key.finish,
                metal = key.metal,
                distinctTypes = 1,
                quantity = items.size,
            ),
            plateCatalogId = catalog.id,
        )
        val state = CollectionState(
            AssembledCollection(
                items = items,
                index = listOf(card),
                derivedCollections = listOf(card.collection),
                // The plate draws the assembly's album (#537).
                albums = CatalogAlbums.over(listOf(catalog), items),
                evidencedCatalogIds = setOf(catalog.id),
                itemsByKey = mapOf(key to items),
            ),
        )
        return notebookSections(
            state,
            listOf(card),
            emptyList(),
            Curation(listOf(catalog)),
            options,
            plateValue,
            wished = wishedYears.mapTo(mutableSetOf()) { year ->
                WishKey(typeId = typeId, year = year, issueId = null)
            },
        ).single()
    }
}
