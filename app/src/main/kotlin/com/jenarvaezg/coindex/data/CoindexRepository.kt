package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.data.db.DerivedCollectionPreferenceEntity
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.domain.ClassifiedDerivedCollections
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionTitles
import com.jenarvaezg.coindex.domain.CuratedGrouping
import com.jenarvaezg.coindex.domain.DerivedCollection
import com.jenarvaezg.coindex.domain.DerivedCollectionDisposition
import com.jenarvaezg.coindex.domain.OwnGroupingView
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.VariantKey
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.buildOwnGroupingViews
import com.jenarvaezg.coindex.domain.classifyDerivedCollections
import com.jenarvaezg.coindex.domain.deriveCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Everything the screens need, derived from the local snapshot alone. */
data class CollectionState(
    val items: List<CollectedItem> = emptyList(),
    val derivedCollections: ClassifiedDerivedCollections = ClassifiedDerivedCollections(),
    val unclassified: List<UnclassifiedItem> = emptyList(),
    val typeMeta: Map<Int, TypeMeta> = emptyMap(),
    val images: Map<Int, TypeImages> = emptyMap(),
    val followedKeys: Set<VariantKey> = emptySet(),
    /** Catalogs the collector owns at least one official type of (plate reachability). */
    val evidencedCatalogIds: Set<String> = emptySet(),
    /** The pieces behind each derived collection, for the screen that opens one. */
    val itemsByKey: Map<VariantKey, List<CollectedItem>> = emptyMap(),
    /** The collector's own groupings, an extra view over the same pieces (ADR 0013). */
    val ownGroupings: List<OwnGroupingView> = emptyList(),
) {
    /** Every current derived collection, whatever the collector decided about it. */
    fun allDerivedCollections(): List<DerivedCollection> =
        derivedCollections.followed + derivedCollections.available + derivedCollections.ignored

    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        allDerivedCollections().firstOrNull { it.key() == key }
}

/** Why a catalog plate cannot be opened. Navigability is never guessed at in the UI. */
enum class PlateUnavailable {
    UnknownCatalog,
    NotACollection,
    NotFollowed,
    NoEvidence,
}

sealed interface PlateResult {
    data class Available(
        val catalog: CollectionCatalog,
        val album: CollectionCatalogAlbum,
    ) : PlateResult

    data class Unavailable(val reason: PlateUnavailable) : PlateResult
}

/**
 * Single source of truth for the collector's local data.
 *
 * Collections are always derived, never stored: the only per-collector rows are the
 * collection snapshot, the type cache, the durable dispositions and the API call log.
 */
class CoindexRepository(
    private val database: CoindexDatabase,
    val catalogs: List<CollectionCatalog>,
    val groupings: List<CuratedGrouping> = emptyList(),
) {
    /** What each collection is called on a card (#22). Constant for the process lifetime. */
    val titles: CollectionTitles = CollectionTitles(catalogs, groupings)

    fun observeState(): Flow<CollectionState> = combine(
        database.collectedItems().observeAll(),
        database.typeMeta().observeAll(),
        database.derivedCollectionPreferences().observeAll(),
        database.ownGroupings().observeAll(),
        database.ownGroupings().observeMembers(),
    ) { items, types, preferences, ownGroupings, ownMembers ->
        val domainItems = items.map { it.toDomain() }
        val typeMeta = types.associate { it.typeId to it.toDomain() }
        val derivation = deriveCollection(domainItems, typeMeta, catalogs, groupings)
        val dispositions = preferences.mapNotNull { it.toDomain() }
        CollectionState(
            items = domainItems,
            derivedCollections =
                classifyDerivedCollections(derivation.derivedCollections, dispositions),
            unclassified = derivation.unclassified,
            typeMeta = typeMeta,
            images = types.associate { it.typeId to it.toImages() },
            followedKeys = dispositions
                .filter { it.disposition == DerivedCollectionDisposition.Followed }
                .mapTo(mutableSetOf()) { it.key },
            evidencedCatalogIds = catalogs
                .filter { catalog -> catalog.isEvidencedBy(domainItems) }
                .mapTo(mutableSetOf()) { it.id },
            itemsByKey = derivation.itemsByKey,
            ownGroupings = buildOwnGroupingViews(
                ownGroupings.map { it.toDomain(ownMembers) },
                domainItems,
            ),
        )
    }

    /** Persists a disposition, or clears it when [disposition] is null (back to Available). */
    suspend fun setDisposition(key: VariantKey, disposition: DerivedCollectionDisposition?) {
        val dao = database.derivedCollectionPreferences()
        if (disposition == null) {
            dao.delete(
                key.family,
                key.storedWeightMillioz(),
                key.finishCode(),
                key.metalCode(),
            )
            return
        }
        val now = System.currentTimeMillis()
        dao.upsert(
            DerivedCollectionPreferenceEntity(
                family = key.family,
                weightMillioz = key.storedWeightMillioz(),
                finishCode = key.finishCode(),
                metalCode = key.metalCode(),
                disposition = disposition.asCode(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /**
     * Every Numista type the curated files name, which is exactly the set a plate can be asked
     * to draw and therefore the set the type cache has to hold.
     *
     * An announced member names none, and its `design_type_id` is not one either: that is the
     * design in another variant, and putting it here would seed the cell with the wrong coin.
     */
    fun curatedTypeIds(): Set<Int> = buildSet {
        catalogs.forEach { catalog ->
            catalog.members.forEach { member -> member.numistaTypeId?.let(::add) }
        }
        groupings.forEach { addAll(it.typeIds) }
    }

    /** The catalog matching a variant key, if one was curated for it. */
    fun catalogFor(key: VariantKey): CollectionCatalog? =
        catalogs.firstOrNull { it.key() == key }

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
 * A plate is reachable only when the collection currently exists, the collector follows
 * that exact variant key, a catalog matches it, and at least one official type is owned. Evidence
 * is by type even for date runs, so a plate stays open while years are still missing.
 */
fun resolvePlate(
    state: CollectionState,
    catalogs: List<CollectionCatalog>,
    catalogId: String,
): PlateResult {
    val catalog = catalogs.firstOrNull { it.id == catalogId }
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    val key = catalog.key()
    return when {
        state.derivedCollectionFor(key) == null ->
            PlateResult.Unavailable(PlateUnavailable.NotACollection)
        key !in state.followedKeys -> PlateResult.Unavailable(PlateUnavailable.NotFollowed)
        catalog.id !in state.evidencedCatalogIds ->
            PlateResult.Unavailable(PlateUnavailable.NoEvidence)
        else -> PlateResult.Available(
            catalog,
            buildCollectionCatalogAlbum(catalog, state.items),
        )
    }
}
