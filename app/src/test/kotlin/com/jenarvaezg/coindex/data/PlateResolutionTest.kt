package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.MemberStatus
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    @Test
    fun `a curated catalog over pieces you own opens its plate with no gesture at all`() {
        val state = state(
            catalog = SOUTHERN_CROSS,
            items = listOf(item(1, 2025)),
        )

        val result = resolvePlate(state, listOf(SOUTHERN_CROSS), SOUTHERN_CROSS.id)

        val available = assertIs<PlateResult.Available>(result)
        assertEquals(SOUTHERN_CROSS.id, available.catalog.id)
        // Evidencia por tipo: la lámina abre entera, con su casilla vacía dentro.
        assertEquals(1, available.album.ownedMembers())
        assertEquals(2, available.album.issuedMembers())
    }

    @Test
    fun `a catalog nobody shipped is said plainly rather than guessed at`() {
        val state = state(catalog = SOUTHERN_CROSS, items = listOf(item(1, 2025)))

        val result = resolvePlate(state, listOf(SOUTHERN_CROSS), "un-catalogo-que-no-existe")

        assertEquals(PlateResult.Unavailable(PlateUnavailable.UnknownCatalog), result)
    }

    /**
     * With no pieces of the variant there is no card and no plate. Cutting the toll does **not**
     * open the 51 catalogs to navigation: that would be new capability against ADR 0007.
     */
    @Test
    fun `a variant you own nothing of is not a collection, so it has no plate`() {
        val result = resolvePlate(CollectionState(), listOf(SOUTHERN_CROSS), SOUTHERN_CROSS.id)

        assertEquals(PlateResult.Unavailable(PlateUnavailable.NotACollection), result)
    }

    /**
     * A card can exist without any official issue of its catalog: the piece is of the variant, but
     * of no member the catalog names. There the plate would be all holes, so it stays shut.
     */
    @Test
    fun `a card with no official issue of the catalog still has no plate`() {
        val state = state(catalog = SOUTHERN_CROSS, items = listOf(item(1, typeId = 777_777)))

        val result = resolvePlate(state, listOf(SOUTHERN_CROSS), SOUTHERN_CROSS.id)

        assertEquals(PlateResult.Unavailable(PlateUnavailable.NoEvidence), result)
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

        val result = resolvePlate(state, listOf(SOUTHERN_CROSS), SOUTHERN_CROSS.id)

        assertEquals(PlateResult.Unavailable(PlateUnavailable.NoEvidence), result)
    }

    private fun state(catalog: CollectionCatalog, items: List<CollectedItem>): CollectionState {
        val key = catalog.key()
        return CollectionState(
            items = items,
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
            evidencedCatalogIds = if (catalog.isEvidencedBy(items)) setOf(catalog.id) else emptySet(),
            itemsByKey = mapOf(key to items),
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
