package com.jenarvaezg.coindex.data

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
import com.jenarvaezg.coindex.domain.MemberStatus
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.coverage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * What a plate demands of the inventory, now that it demands nothing of the collector (ADR 0021 §7).
 *
 * A plate used to have four conditions, and following was the only one of them that said nothing
 * about the world: the other three describe the inventory, that one said «tap here first». The field
 * report of #21 measured what it cost — two Lunar III catalogs curated, evidenced and photographed,
 * and both plates invisible until «seguir colección» was found. A newly curated catalog was born
 * `Disponible`, so **curating never lit a plate on its own**.
 *
 * The three surviving reasons had no test at all before this file: a grep of `resolvePlate` in both
 * `src/test` trees came back empty, which is exactly why the condition is pinned here as it changes.
 */
class PlateResolutionTest {
    /** El único catálogo curado que ve esta prueba: la lámina se resuelve contra la curación. */
    private val curation = Curation(listOf(SOUTHERN_CROSS))

    @Test
    fun `a curated catalog over pieces you own opens its plate with no gesture at all`() {
        val state = state(
            catalog = SOUTHERN_CROSS,
            items = listOf(item(1, 2025)),
        )

        val result = resolvePlate(state, curation, SOUTHERN_CROSS.id)

        val available = assertIs<PlateResult.Available>(result)
        assertEquals(SOUTHERN_CROSS.id, available.catalog.id)
        // Evidencia por tipo: la lámina abre entera, con su casilla vacía dentro.
        assertEquals(1, available.album.ownedMembers())
        assertEquals(2, available.album.issuedMembers())
    }

    @Test
    fun `a catalog nobody shipped is said plainly rather than guessed at`() {
        val state = state(catalog = SOUTHERN_CROSS, items = listOf(item(1, 2025)))

        val result = resolvePlate(state, curation, "un-catalogo-que-no-existe")

        assertEquals(PlateResult.Unavailable(PlateUnavailable.UnknownCatalog), result)
    }

    /**
     * With no pieces of the variant there is no card — and since ADR 0030 there **is** a plate.
     *
     * This is the clause §7 left open being answered: *«cutting the toll does not open the 51 catalogs
     * to navigation … is not decided here»*. It is decided in ADR 0030 §1, and bounded — a curated
     * catalog with no evidence and fewer than twenty measurable casillas is one of the twenty of
     * «Explorar», and it opens **not as the collector's**: no ratio of theirs, no «Exportar», and a
     * gesture that spends where the export was.
     */
    @Test
    fun `a catalog you own nothing of opens as one of the shelf window, and not as yours`() {
        val result = resolvePlate(stateWithoutCards(SOUTHERN_CROSS), curation, SOUTHERN_CROSS.id)

        val available = assertIs<PlateResult.Available>(result)
        assertFalse(available.mine)
        // Every casilla a hole, which is what «you own none of it» means on the sheet.
        assertEquals(0, available.album.ownedMembers())
        assertEquals(2, available.album.issuedMembers())
    }

    /**
     * The cut is what keeps this from being «the 51 catalogs open to navigation» (ADR 0030 §1).
     *
     * A plate of twenty casillas the collector owns nothing of is not a shelf window: it is a
     * catalogue nobody asked for, and it stays shut with the reason it always had. `NotACollection`
     * rather than `NoEvidence` because there is no card either — the two refusals are ordered, and
     * only the window is asked before them.
     */
    @Test
    fun `a catalog too big for the shelf window is still shut`() {
        val result = resolvePlate(stateWithoutCards(LONG_RUN), Curation(listOf(LONG_RUN)), LONG_RUN.id)

        assertEquals(PlateResult.Unavailable(PlateUnavailable.NotACollection), result)
    }

    /**
     * A card can exist without any official issue of its catalog: the piece is of the variant, but of
     * no member the catalog names.
     *
     * The plate is then all holes, and since ADR 0030 that is a plate of the shelf window rather than a
     * refusal — **and it is still not the collector's**, which is the half of this test that was always
     * the point: a card of the index is not evidence, a matching member is.
     */
    @Test
    fun `a card with no official issue of the catalog opens a plate that is not yours`() {
        val state = state(catalog = SOUTHERN_CROSS, items = listOf(item(1, typeId = 777_777)))

        val result = resolvePlate(state, curation, SOUTHERN_CROSS.id)

        assertFalse(assertIs<PlateResult.Available>(result).mine)
    }

    /**
     * Owning the **design** of an announced member is not owning the member: `design_type_id` is
     * the same design in another variant, and it is never consulted for matching (#31). A plate
     * lit by it would draw a bullion slot from a proof coin that does not exist in bullion.
     */
    @Test
    fun `the design of an announced member is no evidence either`() {
        val state = state(
            catalog = SOUTHERN_CROSS,
            items = listOf(item(1, typeId = DESIGN_TYPE_2027)),
        )

        val result = resolvePlate(state, curation, SOUTHERN_CROSS.id)

        // Not the collector's, which is the whole claim: a plate lit by a design would draw a bullion
        // casilla from a proof coin that does not exist in bullion. What it is instead is one of the
        // twenty, with every casilla empty (ADR 0030 §1).
        val available = assertIs<PlateResult.Available>(result)
        assertFalse(available.mine)
        assertEquals(0, available.album.ownedMembers())
    }

    /**
     * **La tarjeta y su lámina no son dos lecturas que coinciden: son el mismo álbum** (#537).
     *
     * El invariante del #218 estaba argumentado en prosa y garantizado por nadie: el índice construía
     * un álbum para dividir su tarjeta y la resolución de la lámina construía un segundo contra su
     * propio comentario. Aquí se afirman las dos mitades — el numerador y el denominador salen iguales,
     * y salen iguales porque es el mismo objeto, así que no hay dos reglas que puedan separarse.
     */
    @Test
    fun `the card's ratio and its plate's ratio come out of one album`() {
        val items = listOf(item(1, 2025))
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = items,
                // La ficha en caché, que es lo que hace de la pieza una tarjeta y no un residuo sin
                // clasificar: la clave la declara el catálogo que reclama el tipo (ADR 0016).
                typeMeta = mapOf(
                    TYPE_2025 to TypeMeta(
                        id = TYPE_2025,
                        issuerCode = "niue",
                        issuerName = "Niue",
                        weightOz = 1.0,
                        metal = Metal.Silver,
                        category = "coin",
                    ),
                ),
            ),
        )
        val card = assembled.index.filterIsInstance<IndexCard.Derived>().single()

        val available = assertIs<PlateResult.Available>(
            resolvePlate(CollectionState(assembled), curation, SOUTHERN_CROSS.id),
        )

        assertEquals(CoverageRatio(owned = 1, issued = 2), card.coverage)
        assertEquals(card.coverage, available.album.coverage())
        assertSame(assembled.albums[SOUTHERN_CROSS], available.album)
    }

    /**
     * An assembly with no card at all, carrying the albums the curation would carry anyway (#537).
     *
     * The two are separate facts and this test file needs them apart: a catalog the collector owns
     * nothing of has no derived collection, and its album is still the one the shelf window draws.
     */
    private fun stateWithoutCards(catalog: CollectionCatalog) = CollectionState(
        AssembledCollection(albums = CatalogAlbums.over(listOf(catalog), emptyList())),
    )

    private fun state(catalog: CollectionCatalog, items: List<CollectedItem>): CollectionState {
        val key = catalog.key()
        return CollectionState(
            AssembledCollection(
                items = items,
                // The albums the assembly carries (#537): the plate reads the one its card divided by,
                // so a state fabricated by hand carries them exactly as `Curation.assemble` would.
                albums = CatalogAlbums.over(listOf(catalog), items),
                derivedCollections = listOf(
                    DerivedCollection(
                        family = key.family,
                        weightMillioz = key.weightMillioz,
                        finish = key.finish,
                        metal = key.metal,
                        distinctTypes = items.map { it.typeId }.distinct().size,
                        quantity = items.sumOf { it.quantity },
                    ),
                ),
                evidencedCatalogIds = if (catalog.isEvidencedBy(items)) {
                    setOf(catalog.id)
                } else {
                    emptySet()
                },
                itemsByKey = mapOf(key to items),
            ),
        )
    }

    private fun item(id: Long, year: Int? = null, typeId: Int = TYPE_2025) = CollectedItem(
        id = id,
        quantity = 1,
        typeId = typeId,
        issueYear = year,
    )

    private companion object {
        const val TYPE_2025 = 295_025

        /** Twenty casillas, which is exactly the cut of the shelf window: a plate this long stays shut. */
        val LONG_RUN = CollectionCatalog(
            schemaVersion = 2,
            id = "panda-plata-30g",
            name = "Panda de plata 30 g",
            shortName = "Panda",
            issuerCode = "chine",
            family = "Panda",
            weightMillioz = 1_000,
            finish = Finish.Bullion,
            metal = Metal.Silver,
            seriesStatus = SeriesStatus.Open,
            source = "https://en.numista.com/catalogue/pieces100000.html",
            updatedAt = "2026-08-14",
            members = (2_006..2_025).map { year ->
                CollectionCatalogMember(
                    id = year.toString(),
                    label = year.toString(),
                    year = year,
                    numistaTypeId = 100_000 + year,
                )
            },
        )

        /** El mismo diseño en otra variante: la casilla de 2027 lo cita, nadie casa con él. */
        const val DESIGN_TYPE_2027 = 999_001

        /**
         * Southern Cross de Niue: dos casillas emitidas y una anunciada, que es la forma que
         * tienen los catálogos abiertos del padre.
         */
        val SOUTHERN_CROSS = CollectionCatalog(
            schemaVersion = 2,
            id = "niue-southern-cross-1oz-bullion",
            name = "Southern Cross · Niue · 1 oz bullion desde 2025",
            shortName = "Southern Cross",
            issuerCode = "niue",
            family = "Southern Cross",
            weightMillioz = 1_000,
            finish = Finish.Bullion,
            metal = Metal.Silver,
            seriesStatus = SeriesStatus.Open,
            source = "https://en.numista.com/catalogue/pieces295025.html",
            updatedAt = "2026-08-04",
            members = listOf(
                CollectionCatalogMember(
                    id = "2025",
                    label = "2025",
                    year = 2025,
                    numistaTypeId = TYPE_2025,
                ),
                CollectionCatalogMember(
                    id = "2026",
                    label = "2026",
                    year = 2026,
                    numistaTypeId = TYPE_2025,
                ),
                CollectionCatalogMember(
                    id = "2027",
                    label = "2027",
                    status = MemberStatus.Announced,
                    source = "https://www.nzmint.com/",
                    sourceNote = "Anunciada por la casa emisora y todavía sin acuñar.",
                    designTypeId = DESIGN_TYPE_2027,
                ),
            ),
        )
    }
}
