package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.CommemorativeProgrammeMember
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
 * When a plate opens: only the inventory and the curation decide, not a gesture of the collector
 * (ADR 0021 §7), so curating a catalog over pieces already owned lights its plate (#21).
 */
class PlateResolutionTest {
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

    /** ADR 0030 §1: with no evidence and under twenty casillas, the plate opens from «Explorar». */
    @Test
    fun `a catalog you own nothing of opens as one of the shelf window, and not as yours`() {
        val result = resolvePlate(stateWithoutCards(SOUTHERN_CROSS), curation, SOUTHERN_CROSS.id)

        val available = assertIs<PlateResult.Available>(result)
        assertFalse(available.mine)
        assertEquals(0, available.album.ownedMembers())
        assertEquals(2, available.album.issuedMembers())
    }

    /**
     * The twenty-casilla cut of ADR 0030 §1. `NotACollection` and not `NoEvidence` because there is
     * no card either: the window is checked first, then the two refusals in that order.
     */
    @Test
    fun `a catalog too big for the shelf window is still shut`() {
        val result = resolvePlate(stateWithoutCards(LONG_RUN), Curation(listOf(LONG_RUN)), LONG_RUN.id)

        assertEquals(PlateResult.Unavailable(PlateUnavailable.NotACollection), result)
    }

    /**
     * The piece is of the catalog's variant but of no member it names: a card of the index is not
     * evidence, only a matching member is.
     */
    @Test
    fun `a card with no official issue of the catalog opens a plate that is not yours`() {
        val state = state(catalog = SOUTHERN_CROSS, items = listOf(item(1, typeId = 777_777)))

        val result = resolvePlate(state, curation, SOUTHERN_CROSS.id)

        assertFalse(assertIs<PlateResult.Available>(result).mine)
    }

    /**
     * `design_type_id` is the same design in another variant and never counts for matching (#31):
     * otherwise a proof coin would fill a bullion casilla.
     */
    @Test
    fun `the design of an announced member is no evidence either`() {
        val state = state(
            catalog = SOUTHERN_CROSS,
            items = listOf(item(1, typeId = DESIGN_TYPE_2027)),
        )

        val result = resolvePlate(state, curation, SOUTHERN_CROSS.id)

        // A shelf-window plate with every casilla empty (ADR 0030 §1).
        val available = assertIs<PlateResult.Available>(result)
        assertFalse(available.mine)
        assertEquals(0, available.album.ownedMembers())
    }

    /** La tarjeta y su lámina dividen por el mismo objeto álbum (#537), no por dos cálculos. */
    @Test
    fun `the card's ratio and its plate's ratio come out of one album`() {
        val items = listOf(item(1, 2025))
        val assembled = curation.assemble(
            CollectionSnapshot(
                items = items,
                // La ficha en caché hace de la pieza una tarjeta y no residuo; la clave la declara
                // el catálogo que reclama el tipo (ADR 0016).
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
     * La lámina lee los programas que trae el ensamblaje en vez de derivarlos (#539): el cuaderno
     * resuelve una lámina por tarjeta, y la respuesta sólo depende de la instantánea.
     */
    @Test
    fun `a plate reads the programme standings the assembly carried`() {
        val programme = CommemorativeProgramme(
            schemaVersion = 1,
            id = "cruz-del-sur",
            name = "Cruz del Sur · prueba",
            shortName = "Cruz del Sur",
            issuerCode = "niue",
            year = 2_025,
            source = "https://example.org/cruz-del-sur",
            sourceNote = "Dos denominaciones para la prueba.",
            updatedAt = "2026-09-01",
            members = listOf(
                CommemorativeProgrammeMember("1 oz", TYPE_2025),
                CommemorativeProgrammeMember("2 oz", 295_026),
            ),
        )
        val withProgramme = Curation(listOf(SOUTHERN_CROSS), programmes = listOf(programme))
        val assembled = withProgramme.assemble(
            CollectionSnapshot(
                items = listOf(item(1, 2025)),
                // La ficha en caché es lo que hace de la pieza una tarjeta y no un residuo.
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

        val available = assertIs<PlateResult.Available>(
            resolvePlate(CollectionState(assembled), withProgramme, SOUTHERN_CROSS.id),
        )

        assertSame(assembled.programmeStandings[SOUTHERN_CROSS], available.programmes)
        assertEquals(listOf("Cruz del Sur"), available.programmes.map { it.programme.shortName })
    }

    /** No cards, but the albums `Curation.assemble` would carry (#537). */
    private fun stateWithoutCards(catalog: CollectionCatalog) = CollectionState(
        AssembledCollection(albums = CatalogAlbums.over(listOf(catalog), emptyList())),
    )

    private fun state(catalog: CollectionCatalog, items: List<CollectedItem>): CollectionState {
        val key = catalog.key()
        return CollectionState(
            AssembledCollection(
                items = items,
                // As `Curation.assemble` would: the plate reads its card's album (#537).
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

        /** Twenty casillas, the shelf-window cut: a plate this long stays shut. */
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

        /** Dos casillas emitidas y una anunciada: la forma de un catálogo abierto. */
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
