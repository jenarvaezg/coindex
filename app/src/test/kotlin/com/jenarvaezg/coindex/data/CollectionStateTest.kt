package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.MetalSpotEntity
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.prices.PlateHole
import com.jenarvaezg.coindex.data.prices.SILVER_SYMBOL
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.MemberStatus
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.UnclassifiedReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * What the phone actually shows, assembled from the rows the phone actually has (#217).
 *
 * `observeState()` composes the whole domain — five mappers, the derivation, the boxes, the index
 * comparator, the evidence — and until this file it had no test at all, because the repository took
 * Room's `CoindexDatabase` and there is no standing in for an abstract class with a generated
 * subclass. It takes the three DAOs now, so the seam has two adapters and this is the second one.
 *
 * Deliberately over a catalog written here rather than over `data/`: what is under test is the
 * assembly, and a test that counted the shipped catalogs would go red every time the curator adds
 * a plate.
 */
class CollectionStateTest {
    @Test
    fun `one snapshot becomes the card, its ratio, its pictures and its date stamp`() = runTest {
        val repository = repository(
            items = listOf(item(id = 1, typeId = TYPE_2025)),
            types = listOf(type(TYPE_2025, fetchedAt = 1_700_000_000_000)),
        )

        val state = repository.observeState().first()

        val card = assertIs<IndexCard.Derived>(state.index.single())
        assertEquals("Southern Cross", card.name)
        // Uno de los dos miembros emitidos: la casilla anunciada no cuenta contra el coleccionista.
        assertEquals(1, card.coverage?.owned)
        assertEquals(2, card.coverage?.issued)
        assertEquals(SOUTHERN_CROSS.id, card.plateCatalogId)
        assertEquals(setOf(SOUTHERN_CROSS.id), state.evidencedCatalogIds)
        assertEquals(listOf(1L), state.itemsByKey[SOUTHERN_CROSS.key()]?.map { it.id })
        assertEquals(THUMBNAIL, state.images[TYPE_2025]?.obverse?.thumbnail)
        assertEquals(1_700_000_000_000, state.fichaFetchedAt[TYPE_2025])
        assertTrue(state.unclassified.isEmpty())
    }

    /**
     * The residue comes out of the same door as the cards. A piece whose ficha has not been
     * downloaded yet is not a card and not a hole in one: it waits in «Sin clasificar» saying why.
     *
     * Its catalog is evidenced all the same — evidence is by type and asks nothing of the ficha —
     * so what keeps the plate shut here is that there is no collection yet, which is exactly the
     * `NotACollection` of `resolvePlate` and not a missing-evidence case.
     */
    @Test
    fun `a piece whose ficha never arrived is reported rather than dropped`() = runTest {
        val repository = repository(
            items = listOf(item(id = 1, typeId = TYPE_2025)),
            types = emptyList(),
        )

        val state = repository.observeState().first()

        assertTrue(state.index.isEmpty())
        assertEquals(UnclassifiedReason.MissingTypeMetadata, state.unclassified.single().reason)
        assertEquals(setOf(SOUTHERN_CROSS.id), state.evidencedCatalogIds)
        assertNull(state.derivedCollectionFor(SOUTHERN_CROSS.key()))
    }

    /**
     * A box the collector typed reaches the index through the same assembly as everything else, and
     * lands in the no-ratio stretch of the one comparator (ADR 0021 §6, §11).
     */
    @Test
    fun `a box the collector typed becomes a card of the index, ratioless`() = runTest {
        val repository = repository(
            items = listOf(item(id = 1, typeId = TYPE_2025), item(id = 2, typeId = LOOSE_TYPE)),
            types = listOf(type(TYPE_2025), type(LOOSE_TYPE, family = null)),
        )

        repository.createOwnGrouping("Las de la caja de puros", listOf(LOOSE_TYPE))
        val state = repository.observeState().first()

        val box = assertIs<IndexCard.Box>(state.index.last())
        assertEquals("Las de la caja de puros", box.name)
        assertNull(box.coverage)
        assertEquals(listOf(2L), box.box.items.map { it.id })
        // La pieza no se mudó: sigue en el residuo, porque una caja es una segunda lectura.
        assertEquals(LOOSE_TYPE, state.unclassified.single().item.typeId)
    }

    /**
     * Dropping the last type of a box takes the box with it — the rule lives in the DAO's own
     * `@Transaction` body, so the fake reimplements its two queries and never the rule.
     */
    @Test
    fun `dropping the last type of a box leaves no heading over nothing`() = runTest {
        val repository = repository(
            items = listOf(item(id = 2, typeId = LOOSE_TYPE)),
            types = listOf(type(LOOSE_TYPE, family = null)),
        )
        val boxId = repository.createOwnGrouping("Las de la caja de puros", listOf(LOOSE_TYPE))

        repository.removeFromOwnGrouping(boxId, LOOSE_TYPE)

        assertTrue(repository.observeState().first().ownGroupings.isEmpty())
    }

    /**
     * The stand-in has to answer what Room answers, or every test above it measures a fiction.
     *
     * `INSERT OR IGNORE` collapses a repeated `(groupingId, typeId)` inside a single batch too, and
     * `create` — the `@Transaction` body this fake inherits rather than reimplements — hands it the
     * list unfiltered. A fake that kept both rows would have the box counting one coin twice.
     */
    @Test
    fun `the same type twice in one box is one member, as the database has it`() = runTest {
        val dao = FakeOwnGroupingDao()

        dao.create("Las de la caja de puros", listOf(LOOSE_TYPE, LOOSE_TYPE), 0L)

        assertEquals(1, dao.members.value.size)
    }

    /**
     * The price book arrives with the listings that address its prices (#493).
     *
     * This is the one piece of plumbing the plate's cost of closing needed: `type_issues` was read
     * only inside the pass, so 111 of the father's 121 holes could not tell which issue they are and
     * therefore could not find the price already on the phone. Read together with the prices and the
     * spot, and not a moment apart, or a plate could total a price under one issue and stamp it into a
     * casilla the newer listing addresses to another.
     */
    @Test
    fun `the price book carries the listings that say which issue a hole is`() = runTest {
        val prices = FakePriceDao().apply {
            this.prices.value = listOf(IssuePriceEntity(TYPE_2025, issueId = 900, grade = "unc", eur = 84.0))
            spots.value = listOf(MetalSpotEntity(SILVER_SYMBOL, eurPerTroyOunce = 55.23, readAt = 1L))
            typeIssueReads.value = listOf(TypeIssueReadEntity(typeId = TYPE_2025, readAt = 1L))
            typeIssues.value = listOf(
                TypeIssueEntity(
                    typeId = TYPE_2025,
                    issueId = 900,
                    position = 0,
                    year = 2_025,
                    gregorianYear = null,
                ),
            )
        }

        val book = repository(items = emptyList(), types = emptyList(), prices = prices)
            .observePrices()
            .first()

        assertEquals(84.0, book.of(TYPE_2025, 900, "unc"))
        assertEquals(55.23, book.spot?.eurPerTroyOunce)
        assertEquals(
            900,
            book.listings.issueOf(PlateHole("southern-cross", typeId = TYPE_2025, year = 2_025)),
        )
    }

    private fun repository(
        items: List<CollectedItemEntity>,
        types: List<TypeMetaEntity>,
        prices: FakePriceDao = FakePriceDao(),
    ): CoindexRepository {
        val collectedItemDao = FakeCollectedItemDao().apply { rows.value = items }
        val typeMetaDao = FakeTypeMetaDao().apply { rows.value = types }
        return CoindexRepository(
            collectedItemDao = collectedItemDao,
            typeMetaDao = typeMetaDao,
            ownGroupingDao = FakeOwnGroupingDao(),
            priceDao = prices,
            curation = Curation(catalogs = listOf(SOUTHERN_CROSS)),
        )
    }

    private fun item(id: Long, typeId: Int, quantity: Int = 1) = CollectedItemEntity(
        id = id,
        typeId = typeId,
        quantity = quantity,
        title = null,
        issuerCode = "niue",
        issueYear = 2025,
        gregorianYear = 2025,
        grade = null,
        price = null,
        forSwap = null,
        collectionName = null,
        raw = "{}",
        syncedAt = 0L,
    )

    private fun type(
        typeId: Int,
        family: String? = "Southern Cross",
        fetchedAt: Long = 1_700_000_000_000,
    ) = TypeMetaEntity(
        typeId = typeId,
        title = "Southern Cross",
        family = family,
        issuerCode = "niue",
        minYear = 2025,
        maxYear = 2025,
        weightGrams = 31.1035,
        obverseUrl = "https://en.numista.com/catalogue/photos/anverso-original.jpg",
        reverseUrl = null,
        raw = "{}",
        fetchedAt = fetchedAt,
        obverseThumbnailUrl = THUMBNAIL,
    )

    private companion object {
        /**
         * Identificadores que **no existen** en `data/numista-type-cache.json`, y `fetchedAt`
         * siempre distinto de cero.
         *
         * Los memos de `Mappers.kt` viven en el proceso y se clavan por `(typeId, fetchedAt)`, así
         * que una ficha inventada aquí con el número de una real y `fetchedAt = 0` le contesta a
         * cualquier otra prueba de la misma JVM que lea la caché sembrada — `NotebookPagesTest`
         * midió el diámetro de esta y encontró el «{}» de aquí.
         */
        const val TYPE_2025 = 990_025

        /** Un tipo que ningún fichero curado nombra: sirve de contenido para una caja propia. */
        const val LOOSE_TYPE = 990_777

        const val THUMBNAIL = "https://en.numista.com/catalogue/photos/anverso-180.jpg"

        /** El mismo catálogo abierto de dos casillas emitidas y una anunciada de #21. */
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
                ),
            ),
        )
    }
}
