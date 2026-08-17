package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.PlateResult
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogSeeds
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
import kotlin.test.assertTrue

/**
 * What a page of pieces says about each one, which is the one place the emission label reaches paper.
 *
 * The star is the whole content of the line for the 100 pesetas of Franco: five rows that all say
 * 1966, all say Numista 1885, and differ only in the star Numista files as a variety of the issue.
 * Printing five identical footnotes is the failure #225 measured, and it happened because the label
 * was an optional parameter three drawers could forget — so what this pins is the drawn line and not
 * `emissionLabelFor`, which was right and green the whole time.
 */
class NotebookSectionsTest {
    private val catalogs: List<CollectionCatalog> = CatalogSeeds.parseAll(CatalogFiles.all())

    /** The five stars, plus a coin no curated catalog claims, in a box the collector typed. */
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

    /**
     * And a piece no issue run claims keeps the year, which is what tells its rows apart.
     *
     * The two cases live in one function on purpose: the label is not a second kind of line, it is
     * the head of the same one, taken over where the year has nothing to say.
     */
    @Test
    fun `a piece outside an issue run still leads with its year`() {
        val unclaimed = CollectedItem(id = 6, quantity = 2, typeId = 999_999, issueYear = 1994)

        assertEquals(
            listOf("Estrella 66 · Numista 1885", "1994 · Numista 999999 · ×2"),
            footnotesOf(listOf(stars.first(), unclaimed)),
        )
    }

    /**
     * The page counts what the screen and the shared sheet count.
     *
     * A card whose catalog the collector owns no issued member of yet arrives carrying the ratio
     * (ADR 0021 §7) — the one collection with an issue list that lands on pieces instead of a plate
     * — and the notebook has to print «0 de 12 · te faltan 12» like the other two. It is the third
     * of the three surfaces #226 measured, and the only other one a JVM test can read.
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
     * The stamp is a state of the plate (ADR 0026 §3 / §4), and the notebook page has to carry it
     * the same way the PNG does (#371). The Progress row stays whole — a page of the cuaderno has
     * no header figure to raise the ratio into (ADR 0026 §5) — so completeness travels as its own
     * bit on the section, not by eating the specification.
     */
    @Test
    fun `a complete plate section says so and keeps the progress row`() {
        val section = dateRunSection(ownedYears = listOf(1879, 1886))

        assertTrue(section.complete)
        assertEquals("2/2", section.ratio)
        assertEquals("Progreso" to "2 / 2 emisiones", section.facts.first())
    }

    /** Missing one is the same plate with no ink: the stamp is read off the inventory, not remembered. */
    @Test
    fun `an incomplete plate section carries no stamp`() {
        val section = dateRunSection(ownedYears = listOf(1879))

        assertFalse(section.complete)
        assertEquals("1/2", section.ratio)
        assertEquals("Progreso" to "1 / 2 emisiones", section.facts.first())
    }

    /** A page of pieces has no plate to be complete, so the rubber stamp never lands on it. */
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


    /**
     * **With the money off, nothing derived from an amount reaches the page** (#228, ADR 0021 §13).
     *
     * The switch is answered once, by handing the printer nothing rather than by asking the options
     * again further down, so a drawer that has no amount cannot print one. That is what makes
     * «apagarlo no deja escapar ninguna cifra derivada de dinero» a property of the code and not a
     * promise about it.
     */
    @Test
    fun `with the money off no fact of the page carries an amount`() {
        val section = dateRunSection(listOf(1879), options = NotebookOptions(money = false))

        assertEquals(emptyList(), section.facts.filter { (label, _) -> label == "Valor" })
        assertTrue(section.facts.none { (_, value) -> "€" in value })
    }

    /** With it on, the plate's own value joins its specification, because paper has no header. */
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
    }

    /**
     * The mark travels to the paper, and it travels as the casilla's state (ADR 0026 §4, ADR 0029 §7).
     *
     * «Alive» in §4 is what follows the finger, the sensor or the navigation; a wish mark is a state at
     * rest, like the rubber stamp of a complete plate, so it needs no exception written. It lands in the
     * line a printed caption has always reserved and never used, so it costs no millimetre and moves no
     * page count — and it is **not** behind the money switch, because what that one withholds is an
     * amount.
     */
    @Test
    fun `a marked casilla prints its mark and an owned one prints nothing`() {
        val section = dateRunSection(ownedYears = listOf(1879), wishedYears = listOf(1886))

        assertEquals(listOf(null, "lo busco"), section.cells.map { it.state })
        // With the money off the mark is still there: it is not a figure.
        assertEquals(
            listOf(null, "lo busco"),
            dateRunSection(
                ownedYears = listOf(1879),
                options = NotebookOptions(money = false),
                wishedYears = listOf(1886),
            ).cells.map { it.state },
        )
        // And with nothing marked the page is the page it always was.
        assertEquals(listOf(null, null), dateRunSection(ownedYears = listOf(1879)).cells.map { it.state })
    }

    /**
     * A two-year date run resolved the way `resolvePlate` resolves production plates: the card
     * names the catalog, the state carries the evidence, and `notebookSections` is what reads
     * completeness off the subject.
     */
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
                // The album the assembly carries (#537), which is the one the plate draws.
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
