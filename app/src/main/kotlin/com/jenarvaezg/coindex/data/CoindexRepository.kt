package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.OwnGroupingDao
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionSnapshot
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.ProgrammeStanding
import com.jenarvaezg.coindex.domain.TypeMetaIndex
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.VariantKey
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.programmeStandings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Everything the screens need, derived from the local snapshot alone.
 *
 * The collection itself is whatever [Curation.assemble] made of the snapshot, untouched: this type
 * adds the two things that are the app's and not the domain's — the catalog photographs, which the
 * domain stays free of, and when each ficha reached this phone. Wrapping rather than re-listing
 * the eight derived fields is the point of #217: what the app reads comes out of one assembly, so
 * the index and the inventory under it are no longer two things a caller could fill in separately.
 */
data class CollectionState(
    val collection: AssembledCollection = AssembledCollection(),
    val images: Map<Int, TypeImages> = emptyMap(),
    /**
     * When each ficha was brought to this phone, so a card can say «hace ocho meses» instead of
     * leaving it to be guessed at (#185). It is the cache's own `fetchedAt` and nothing derived: a
     * ficha the collector refreshed is stamped again, and a ficha that arrived in the APK is
     * stamped with the day it arrived (ADR 0023).
     */
    val fichaFetchedAt: Map<Int, Long> = emptyMap(),
) {
    val items: List<CollectedItem> get() = collection.items
    val index: List<IndexCard> get() = collection.index
    val derivedCollections: List<DerivedCollection> get() = collection.derivedCollections
    val unclassified: List<UnclassifiedItem> get() = collection.unclassified
    val typeMeta: TypeMetaIndex get() = collection.typeMeta
    val evidencedCatalogIds: Set<String> get() = collection.evidencedCatalogIds
    val itemsByKey: Map<VariantKey, List<CollectedItem>> get() = collection.itemsByKey
    val ownGroupings: List<OwnGroupingView> get() = collection.ownGroupings
    val emissionLabels: Map<Long, String> get() = collection.emissionLabels

    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        collection.derivedCollectionFor(key)
}

/**
 * Why a catalog plate cannot be opened. Navigability is never guessed at in the UI.
 *
 * Three reasons and no more (ADR 0021 §7): `NotFollowed` left with the dispositions, and it was the
 * only one of the four that said nothing about the world — the other three describe the inventory,
 * that one said «tap here first».
 */
enum class PlateUnavailable {
    UnknownCatalog,
    NotACollection,
    NoEvidence,
}

sealed interface PlateResult {
    data class Available(
        val catalog: CollectionCatalog,
        val album: CollectionCatalogAlbum,
        /**
         * The commemorative programmes this catalog touches (ADR 0022), each with the
         * collector's progress in it. Empty for all but the two Portuguese cupronickel
         * catalogs today, and never part of the plate's own denominator.
         */
        val programmes: List<ProgrammeStanding> = emptyList(),
    ) : PlateResult

    data class Unavailable(val reason: PlateUnavailable) : PlateResult
}

/**
 * Single source of truth for the collector's local data.
 *
 * Collections are always derived, never stored, and since ADR 0021 §7 **nothing at all is stored
 * per card**: the only per-collector rows are the collection snapshot, the type cache, the boxes the
 * collector typed and the API call log.
 *
 * It takes the three DAOs it reads and not the database that holds them (#217). Room's
 * `CoindexDatabase` is an abstract class with a generated subclass and no stand-in, so receiving
 * it sealed the seam from the inside: the DAOs are interfaces with two implementations each —
 * Room's in production and the fakes in `src/test` — which is what makes [observeState] testable.
 */
class CoindexRepository(
    private val collectedItemDao: CollectedItemDao,
    private val typeMetaDao: TypeMetaDao,
    private val ownGroupingDao: OwnGroupingDao,
    /** The curated files, tied once, and the only door into the domain (#217). */
    val curation: Curation,
) {
    fun observeState(): Flow<CollectionState> = combine(
        collectedItemDao.observeAll(),
        typeMetaDao.observeAll(),
        ownGroupingDao.observeAll(),
        ownGroupingDao.observeMembers(),
    ) { items, types, ownGroupings, ownMembers ->
        CollectionState(
            collection = curation.assemble(
                CollectionSnapshot(
                    items = items.map { it.toDomain() },
                    typeMeta = types.associate { it.typeId to it.toDomain() },
                    ownGroupings = ownGroupings.map { it.toDomain(ownMembers) },
                ),
            ),
            images = types.associate { it.typeId to it.toImages() },
            fichaFetchedAt = types.associate { it.typeId to it.fetchedAt },
        )
    }

    /** Creates one of the collector's own groupings over the types they picked (ADR 0013). */
    suspend fun createOwnGrouping(name: String, typeIds: List<Int>): Long =
        ownGroupingDao.create(name, typeIds.distinct(), System.currentTimeMillis())

    suspend fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        ownGroupingDao.addMembers(
            typeIds.distinct().map { OwnGroupingMemberEntity(groupingId, it) },
        )
        ownGroupingDao.touch(groupingId, System.currentTimeMillis())
    }

    suspend fun renameOwnGrouping(groupingId: Long, name: String) {
        ownGroupingDao.rename(groupingId, name, System.currentTimeMillis())
    }

    /** Drops one type from a grouping, and the grouping itself if it was the last one. */
    suspend fun removeFromOwnGrouping(groupingId: Long, typeId: Int) {
        ownGroupingDao.removeMemberOrDelete(groupingId, typeId, System.currentTimeMillis())
    }

    suspend fun deleteOwnGrouping(groupingId: Long) {
        ownGroupingDao.delete(groupingId)
    }
}

/**
 * Resolves a plate against the current state.
 *
 * **A plate opens on evidence** (ADR 0021 §7): the collection has to exist right now, a catalog has
 * to match it, and at least one official type has to be owned. Nothing else — curating a catalog for
 * a variant the collector already owns is enough to light its plate, where before ADR 0021 the whole
 * curation stayed invisible until they guessed they had to follow it.
 *
 * Evidence is by type even for date runs, so a plate stays open while years are still missing.
 *
 * Takes the whole [Curation] rather than a catalog list and a programme list threaded separately:
 * both come from the same files, and passing them apart is how a caller ends up handing the
 * repository two things the repository already had (#217).
 */
fun resolvePlate(
    state: CollectionState,
    curation: Curation,
    catalogId: String,
): PlateResult {
    val catalog = curation.catalogs.firstOrNull { it.id == catalogId }
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    return when {
        state.derivedCollectionFor(catalog.key()) == null ->
            PlateResult.Unavailable(PlateUnavailable.NotACollection)
        catalog.id !in state.evidencedCatalogIds ->
            PlateResult.Unavailable(PlateUnavailable.NoEvidence)
        else -> PlateResult.Available(
            catalog,
            buildCollectionCatalogAlbum(catalog, state.items),
            programmeStandings(catalog, curation.programmes, state.items),
        )
    }
}
