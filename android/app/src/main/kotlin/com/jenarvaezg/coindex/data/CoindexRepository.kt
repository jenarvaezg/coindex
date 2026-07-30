package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CoindexDatabase
import com.jenarvaezg.coindex.data.db.ProposalPreferenceEntity
import com.jenarvaezg.coindex.domain.ClassifiedCollectionProposals
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionProposalKey
import com.jenarvaezg.coindex.domain.ProposalDisposition
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.UnclassifiedItem
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.classifyCollectionProposals
import com.jenarvaezg.coindex.domain.deriveCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Catalog picture URLs for one type. Kept out of the domain, which stays free of anything
 * presentational, and loaded straight from Numista by Coil — there is no proxy any more.
 */
data class TypeImages(val obverse: String?, val reverse: String?)

/** Everything the screens need, derived from the local snapshot alone. */
data class CollectionState(
    val items: List<CollectedItem> = emptyList(),
    val proposals: ClassifiedCollectionProposals = ClassifiedCollectionProposals(),
    val unclassified: List<UnclassifiedItem> = emptyList(),
    val typeMeta: Map<Int, TypeMeta> = emptyMap(),
    val images: Map<Int, TypeImages> = emptyMap(),
    val followedKeys: Set<CollectionProposalKey> = emptySet(),
    /** Catalogs the collector owns at least one official type of (plate reachability). */
    val evidencedCatalogIds: Set<String> = emptySet(),
)

/** Why a catalog plate cannot be opened. Navigability is never guessed at in the UI. */
enum class PlateUnavailable {
    UnknownCatalog,
    NotAProposal,
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
 * Proposals are always derived, never stored: the only per-collector rows are the collection
 * snapshot, the type cache, the durable dispositions and the API call log.
 */
class CoindexRepository(
    private val database: CoindexDatabase,
    val catalogs: List<CollectionCatalog>,
) {
    fun observeState(): Flow<CollectionState> = combine(
        database.collectedItems().observeAll(),
        database.typeMeta().observeAll(),
        database.proposalPreferences().observeAll(),
    ) { items, types, preferences ->
        val domainItems = items.map { it.toDomain() }
        val typeMeta = types.associate { it.typeId to it.toDomain() }
        val derivation = deriveCollection(domainItems, typeMeta, catalogs)
        val dispositions = preferences.mapNotNull { it.toDomain() }
        CollectionState(
            items = domainItems,
            proposals = classifyCollectionProposals(derivation.proposals, dispositions),
            unclassified = derivation.unclassified,
            typeMeta = typeMeta,
            images = types.associate { it.typeId to TypeImages(it.obverseUrl, it.reverseUrl) },
            followedKeys = dispositions
                .filter { it.disposition == ProposalDisposition.Followed }
                .mapTo(mutableSetOf()) { it.key },
            evidencedCatalogIds = catalogs
                .filter { catalog -> catalog.isEvidencedBy(domainItems) }
                .mapTo(mutableSetOf()) { it.id },
        )
    }

    /** Persists a disposition, or clears it when [disposition] is null (back to Available). */
    suspend fun setDisposition(key: CollectionProposalKey, disposition: ProposalDisposition?) {
        val dao = database.proposalPreferences()
        if (disposition == null) {
            dao.delete(key.family, key.weightMillioz, key.finishCode())
            return
        }
        val now = System.currentTimeMillis()
        dao.upsert(
            ProposalPreferenceEntity(
                family = key.family,
                weightMillioz = key.weightMillioz,
                finishCode = key.finishCode(),
                disposition = disposition.asCode(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** The catalog matching a proposal variant key, if one was curated for it. */
    fun catalogFor(key: CollectionProposalKey): CollectionCatalog? =
        catalogs.firstOrNull { it.key() == key }
}

/**
 * Resolves a plate against the current state.
 *
 * A plate is reachable only when the proposal currently exists, the collector follows that
 * exact variant key, a catalog matches it, and at least one official type is owned. Evidence
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
    val currentProposal = (state.proposals.followed + state.proposals.available +
        state.proposals.ignored).any { it.key() == key }
    return when {
        !currentProposal -> PlateResult.Unavailable(PlateUnavailable.NotAProposal)
        key !in state.followedKeys -> PlateResult.Unavailable(PlateUnavailable.NotFollowed)
        catalog.id !in state.evidencedCatalogIds ->
            PlateResult.Unavailable(PlateUnavailable.NoEvidence)
        else -> PlateResult.Available(
            catalog,
            buildCollectionCatalogAlbum(catalog, state.items),
        )
    }
}
