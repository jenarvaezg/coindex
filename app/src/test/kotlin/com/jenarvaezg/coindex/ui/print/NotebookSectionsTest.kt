package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.OwnGrouping
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.TypeMeta
import kotlin.test.Test
import kotlin.test.assertEquals

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

        assertEquals("5 Pounds", cell.denomination)
        assertEquals("Red Dragon of Wales", cell.theme)
    }
}
