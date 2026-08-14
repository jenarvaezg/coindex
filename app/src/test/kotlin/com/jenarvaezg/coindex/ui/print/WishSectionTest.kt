package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.wishKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NOW = 1_786_400_000_000L

/**
 * «La lista de lo que busco» on paper (ADR 0029 §7).
 *
 * It exists because ADR 0026 §4 alone was not enough: the mark reaches the paper of any plate the
 * collector can open, and the plate of «Explorar» has no «Exportar» at all — so without this lámina the
 * 157 slots of the shelf window could never be printed. What is pinned here is that it is **a lámina
 * like the others** and that it says nothing a page of coins nobody owns must not say.
 */
class WishSectionTest {
    private val kooka = dateRun("kooka", 2_010..2_012, typeId = 30)
    private val koala = dateRun("koala", 2_014..2_015, typeId = 40)
    private val state = CollectionState(
        AssembledCollection(
            typeMeta = mapOf(
                30 to TypeMeta(id = 30, title = "1 Dollar", sizeMillimetres = 40.6),
                40 to TypeMeta(id = 40, title = "1 Dollar"),
            ),
        ),
    )

    @Test
    fun `the list is one lamina of holes, and every cell says which plate it came from`() {
        val section = wishSections(
            state,
            listOf(slot(kooka, "kooka-2011"), slot(koala, "koala-2014")),
            NotebookOptions(),
        ).single()

        assertEquals("COINDEX · LO QUE BUSCO", section.eyebrow)
        assertEquals("Lo que busco", section.title)
        assertEquals(listOf("Casillas" to "2 casillas en 2 láminas"), section.facts)
        assertEquals(listOf("2011", "2014"), section.cells.map { it.label })
        assertEquals(listOf("kooka", "koala"), section.cells.map { it.footnote })
        // Every one of them is a coin the collector does not have, so every one is a die-cut hole.
        assertTrue(section.cells.none { it.filled })
        // The mark itself is **not** repeated here: this whole sheet is what he is looking for, and
        // «lo busco» under each cell would print the same two words to distinguish nothing.
        assertTrue(section.cells.all { it.state == null })
        // A real diameter where the cache has one, borrowed by nobody where it has none.
        assertEquals(listOf(40.6f, null), section.cells.map { it.diameterMm })
    }

    /**
     * It is not a page of the collection, and the eyebrow is where that is said once.
     *
     * The paper outlives the app: a sheet that said «COLECCIÓN» over seven coins in a dealer's tray
     * would be a false claim in somebody else's hands, and the source line has the same problem — «tu
     * colección en Numista» is what every other page says and none of these coins is in it.
     */
    @Test
    fun `it never claims to be a collection`() {
        val section = wishSections(state, listOf(slot(kooka, "kooka-2011")), NotebookOptions()).single()

        assertTrue("COLECCIÓN" !in section.eyebrow)
        assertEquals("los catálogos curados de Coindex", section.source)
        // No completion stamp and no ratio: there is no album here to be complete against.
        assertEquals(null, section.ratio)
        assertTrue(!section.complete)
    }

    /** No folio is ever spent on a heading with nothing under it. */
    @Test
    fun `an empty list prints no lamina at all`() {
        assertTrue(wishSections(state, emptyList(), NotebookOptions()).isEmpty())
    }

    /**
     * It obeys the same switches every other lámina does (#228, #231).
     *
     * One machine and not a second printer: with the photographs off a cell has no face, and with both
     * faces on it has two — which is what makes the list a page of the notebook rather than a report.
     */
    @Test
    fun `the switches reach its cells like any other lamina`() {
        val slots = listOf(slot(kooka, "kooka-2011"))

        assertEquals(1, wishSections(state, slots, NotebookOptions()).single().cells.single().faces.size)
        assertEquals(
            2,
            wishSections(state, slots, NotebookOptions(bothFaces = true))
                .single().cells.single().faces.size,
        )
        assertTrue(
            wishSections(state, slots, NotebookOptions(photographs = false))
                .single().cells.single().faces.isEmpty(),
        )
    }
}

private fun slot(catalog: CollectionCatalog, memberId: String): WishedSlot {
    val member = catalog.members.first { it.id == memberId }
    return WishedSlot(Wish(requireNotNull(member.wishKey()), NOW), catalog, member)
}

private fun dateRun(id: String, years: IntRange, typeId: Int): CollectionCatalog = CollectionCatalog(
    schemaVersion = 2,
    id = id,
    name = id,
    shortName = id,
    family = id,
    issuerCode = "australie",
    weightMillioz = 1_000,
    metal = Metal.Silver,
    seriesStatus = SeriesStatus.Open,
    source = "https://en.numista.com/catalogue/pieces1.html",
    updatedAt = "2026-08-14",
    members = years.map { year ->
        CollectionCatalogMember(
            id = "$id-$year",
            label = year.toString(),
            year = year,
            numistaTypeId = typeId,
        )
    },
)
