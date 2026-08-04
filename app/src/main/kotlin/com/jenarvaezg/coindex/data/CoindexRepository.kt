package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionIndex
import com.jenarvaezg.coindex.domain.CollectionTitles
import com.jenarvaezg.coindex.domain.CommemorativeProgramme
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.ProgrammeStanding
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.VariantKey
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.buildOwnGroupingViews
import com.jenarvaezg.coindex.domain.deriveCollection
import com.jenarvaezg.coindex.domain.programmeStandings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the screens need, derived from the local snapshot alone. */
data class CollectionState(
    val items: List<CollectedItem> = emptyList(),
    /**
     * The first level, as one list in one order (ADR 0021 §2, §6): curated catalogs, curated
     * groupings and the collector's own boxes, already sorted by the index comparator.
     */
    val index: List<IndexCard> = emptyList(),
    val derivedCollections: List<DerivedCollection> = emptyList(),
    val unclassified: List<UnclassifiedItem> = emptyList(),
    val typeMeta: Map<Int, TypeMeta> = emptyMap(),
    val images: Map<Int, TypeImages> = emptyMap(),
    /** Catalogs the collector owns at least one official type of (plate reachability). */
    val evidencedCatalogIds: Set<String> = emptySet(),
    /** The pieces behind each derived collection, for the screen that opens one. */
    val itemsByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
    /** The collector's own boxes, which hold only pieces they own (ADR 0021 §11). */
    val ownGroupings: List<OwnGroupingView> = emptyList(),
) {
    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        derivedCollections.firstOrNull { it.key() == key }
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
 */
class CoindexRepository(
    private val database: CoindexDatabase,
    val catalogs: List<CollectionCatalog>,
    val groupings: List<CuratedGrouping> = emptyList(),
    /** Commemorative programmes (ADR 0022): a second reading, never a card and never a family. */
    val programmes: List<CommemorativeProgramme> = emptyList(),
) {
    /** What each collection is called on a card (#22). Constant for the process lifetime. */
    val titles: CollectionTitles = CollectionTitles(catalogs, groupings)

    /** The one list of the first level, built from the same constant seeds (ADR 0021 §6). */
    private val index: CollectionIndex = CollectionIndex(catalogs, groupings, titles)

    fun observeState(): Flow<CollectionState> = combine(
        database.collectedItems().observeAll(),
        database.typeMeta().observeAll(),
        database.ownGroupings().observeAll(),
        database.ownGroupings().observeMembers(),
    ) { items, types, ownGroupings, ownMembers ->
        val domainItems = items.map { it.toDomain() }
        val typeMeta = types.associate { it.typeId to it.toDomain() }
        val derivation = deriveCollection(domainItems, typeMeta, catalogs, groupings)
        val boxes = buildOwnGroupingViews(
            ownGroupings.map { it.toDomain(ownMembers) },
            domainItems,
        )
        CollectionState(
            items = domainItems,
            index = index.build(derivation, boxes, domainItems, typeMeta),
            derivedCollections = derivation.derivedCollections,
            unclassified = derivation.unclassified,
            typeMeta = typeMeta,
            images = types.associate { it.typeId to it.toImages() },
            evidencedCatalogIds = catalogs
                .filter { catalog -> catalog.isEvidencedBy(domainItems) }
                .mapTo(mutableSetOf()) { it.id },
            itemsByKey = derivation.itemsByKey,
            ownGroupings = boxes,
        )
    }

    /**
     * Every Numista type the curated files name, which is exactly the set a plate can be asked
     * to draw and therefore the set the type cache has to hold.
     *
     * An announced member names none, and its `design_type_id` is not one either: that is the
     * design in another variant, and putting it here would seed the cell with the wrong coin.
     *
     * A programme's members count too (ADR 0022), including the ones no catalog claims: the plate
     * names the programme beside its rows, and the 25 escudos of 1977 and 1983 are exactly the
     * coins a collector with «1 de 3» is missing.
     */
    fun curatedTypeIds(): Set<Int> = buildSet {
        catalogs.forEach { catalog ->
            catalog.members.forEach { member -> member.numistaTypeId?.let(::add) }
        }
        groupings.forEach { addAll(it.typeIds) }
        programmes.forEach { programme ->
            programme.members.forEach { member -> add(member.numistaTypeId) }
        }
    }

    /** Creates one of the collector's own groupings over the types they picked (ADR 0013). */
    suspend fun createOwnGrouping(name: String, typeIds: List<Int>): Long =
        database.ownGroupings().create(name, typeIds.distinct(), System.currentTimeMillis())

    suspend fun addToOwnGrouping(groupingId: Long, typeIds: List<Int>) {
        val dao = database.ownGroupings()
        dao.addMembers(typeIds.distinct().map { OwnGroupingMemberEntity(groupingId, it) })
        dao.touch(groupingId, System.currentTimeMillis())
    }

    suspend fun renameOwnGrouping(groupingId: Long, name: String) {
        database.ownGroupings().rename(groupingId, name, System.currentTimeMillis())
    }

    /** Drops one type from a grouping, and the grouping itself if it was the last one. */
    suspend fun removeFromOwnGrouping(groupingId: Long, typeId: Int) {
        database.ownGroupings()
            .removeMemberOrDelete(groupingId, typeId, System.currentTimeMillis())
    }

    suspend fun deleteOwnGrouping(groupingId: Long) {
        database.ownGroupings().delete(groupingId)
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
 */
fun resolvePlate(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    catalogId: String,
    programmes: List<CommemorativeProgramme> = emptyList(),
): PlateResult {
    val catalog = catalogs.firstOrNull { it.id == catalogId }
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    return when {
        state.derivedCollectionFor(catalog.key()) == null ->
            PlateResult.Unavailable(PlateUnavailable.NotACollection)
        catalog.id !in state.evidencedCatalogIds ->
            PlateResult.Unavailable(PlateUnavailable.NoEvidence)
        else -> PlateResult.Available(
            catalog,
            buildCollectionCatalogAlbum(catalog, state.items),
            programmeStandings(catalog, programmes, state.items),
        )
    }
}
