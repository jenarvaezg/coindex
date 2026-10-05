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
 * `observeState()` end to end over fake DAOs (#217): mappers, derivation, boxes, index order and
 * evidence. The catalog is written here rather than read from `data/` so that curating a new plate
 * doesn't turn this red.
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
     * The piece waits in «Sin clasificar». Its catalog is evidenced all the same (evidence is by
     * type), so the plate stays shut as `NotACollection`, not for missing evidence.
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

    /** It sorts into the no-ratio stretch of the index (ADR 0021 §6, §11). */
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
        // La pieza sigue en el residuo: una caja es una segunda lectura.
        assertEquals(LOOSE_TYPE, state.unclassified.single().item.typeId)
    }

    /** The rule lives in the DAO's `@Transaction` body: the fake only reimplements its queries. */
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
     * Room's `INSERT OR IGNORE` collapses a repeated `(groupingId, typeId)` within one batch too,
     * and the inherited `create` passes the list unfiltered, so the fake must collapse it as well.
     */
    @Test
    fun `the same type twice in one box is one member, as the database has it`() = runTest {
        val dao = FakeOwnGroupingDao()

        dao.create("Las de la caja de puros", listOf(LOOSE_TYPE, LOOSE_TYPE), 0L)

        assertEquals(1, dao.members.value.size)
    }

    /**
     * Read together with prices and spot (#493), so a plate never totals a hole's price under one
     * listing and stamps it under a newer one.
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
            wishDao = FakeWishDao(),
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
        /** Un identificador que no existe en `data/numista-type-cache.json`. */
        const val TYPE_2025 = 990_025

        /** Un tipo que ningún fichero curado nombra: sirve de contenido para una caja propia. */
        const val LOOSE_TYPE = 990_777

        const val THUMBNAIL = "https://en.numista.com/catalogue/photos/anverso-180.jpg"

        /** Catálogo abierto: dos casillas emitidas y una anunciada. */
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
