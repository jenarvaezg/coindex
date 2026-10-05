package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.OwnGroupingDao
import com.jenarvaezg.coindex.data.db.OwnGroupingMemberEntity
import com.jenarvaezg.coindex.data.db.PriceDao
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.WishDao
import com.jenarvaezg.coindex.data.photos.TypeImages
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.data.prices.SILVER_SYMBOL
import com.jenarvaezg.coindex.data.prices.priceBook
import com.jenarvaezg.coindex.data.prices.toDomain
import com.jenarvaezg.coindex.domain.AlbumSlot
import com.jenarvaezg.coindex.domain.AssembledCollection
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CatalogProgrammes
import com.jenarvaezg.coindex.domain.CoinClaims
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
import com.jenarvaezg.coindex.domain.Wish
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.showcasePlate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Everything the screens need, derived from the local snapshot alone: what [Curation.assemble] made
 * of it, wrapped whole so the index and the inventory come from one assembly (#217), plus what
 * belongs to the app rather than the domain, the catalog photographs and when each ficha arrived.
 */
data class CollectionState(
    val collection: AssembledCollection = AssembledCollection(),
    val images: Map<Int, TypeImages> = emptyMap(),
    /**
     * When each ficha reached this phone, so a card can say «hace ocho meses» (#185). The cache's
     * own `fetchedAt`: restamped when the collector asks for the ficha again, and the arrival day for
     * a ficha from the APK (ADR 0025).
     */
    val fichaFetchedAt: Map<Int, Long> = emptyMap(),
) {
    val items: List<CollectedItem> get() = collection.items
    val index: List<IndexCard> get() = collection.index
    val derivedCollections: List<DerivedCollection> get() = collection.derivedCollections
    val unclassified: List<UnclassifiedItem> get() = collection.unclassified
    val typeMeta: TypeMetaIndex get() = collection.typeMeta
    val albums: CatalogAlbums get() = collection.albums
    val programmeStandings: CatalogProgrammes get() = collection.programmeStandings
    val evidencedCatalogIds: Set<String> get() = collection.evidencedCatalogIds
    val slots: List<AlbumSlot> get() = collection.slots
    val itemsByKey: Map<VariantKey, List<CollectedItem>> get() = collection.itemsByKey
    val ownGroupings: List<OwnGroupingView> get() = collection.ownGroupings
    val emissionLabels: Map<Long, String> get() = collection.emissionLabels
    val claims: CoinClaims get() = collection.claims

    fun derivedCollectionFor(key: VariantKey): DerivedCollection? =
        collection.derivedCollectionFor(key)
}

/**
 * Why a catalog plate cannot be opened, so the UI never guesses at navigability. `NotFollowed` left
 * with the dispositions (ADR 0021 §7).
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
         * collector's progress in it; never part of the plate's own denominator.
         */
        val programmes: List<ProgrammeStanding> = emptyList(),
        /**
         * Whether the plate is the collector's or one of the shelf window's (ADR 0030), derived from
         * the evidence on every read. It decides what the plate offers («Exportar la lámina» or
         * «Tasar esta lámina») and its header's money: a plate holding nothing has no «Valor
         * actual», only the cost of entering (ADR 0030 §6).
         */
        val mine: Boolean = true,
    ) : PlateResult

    data class Unavailable(val reason: PlateUnavailable) : PlateResult
}

/**
 * Single source of truth for the collector's local data. Collections are always derived, and nothing
 * is stored per card (ADR 0021 §7): the stored rows are the collection snapshot, the type cache, the
 * collector's groupings and marked casillas, the prices and the API call log.
 *
 * It takes DAOs rather than `CoindexDatabase` (#217): Room's database class has no stand-in, while
 * the DAOs are interfaces with fakes in `src/test`, which makes [observeState] testable.
 */
class CoindexRepository(
    private val collectedItemDao: CollectedItemDao,
    private val typeMetaDao: TypeMetaDao,
    private val ownGroupingDao: OwnGroupingDao,
    private val priceDao: PriceDao,
    private val wishDao: WishDao,
    /** The curated files, tied once, and the only door into the domain (#217). */
    val curation: Curation,
) {
    /**
     * Every price and the spot, as one value (ADR 0028). Kept apart from [observeState] because
     * prices land one by one during a background pass, and combining them would re-assemble the
     * whole index on every row.
     */
    fun observePrices(): Flow<PriceBook> = combine(
        priceDao.observePrices(),
        priceDao.observeSpot(SILVER_SYMBOL),
        priceDao.observeTypeIssueReads(),
        priceDao.observeTypeIssues(),
        // When each price landed, shown beside amounts that never expire (ADR 0030 §4). In the same
        // `combine` as the prices, so a figure and its date always belong to the same total.
        priceDao.observeReads(),
    ) { prices, spot, reads, issues, priceReads ->
        // `held`, not fresh (#493): a screen only addresses prices it already has, and expired rows
        // stay on screen (ADR 0028 §5).
        priceBook(prices, spot?.toDomain(), IssueListings.held(reads, issues), priceReads, reads)
    }
    /**
     * The marked casillas (ADR 0029). Kept apart from [observeState] like the prices: nothing that
     * reads the inventory joins this table (ADR 0029 §3), and toggling a mark must not re-assemble
     * the index. `wishedSlots` crosses the two where a screen asks for it.
     */
    fun observeWishes(): Flow<List<Wish>> =
        wishDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * Marks one empty casilla, or keeps the existing mark (ADR 0029 §5). An ignored insert keeps the
     * first [Wish.markedAt], which orders the list, so marking twice doesn't reshuffle it.
     */
    suspend fun markWish(key: WishKey) {
        wishDao.mark(Wish(key, System.currentTimeMillis()).toEntity())
    }

    suspend fun unmarkWish(key: WishKey) {
        wishDao.unmark(key.typeId, key.year, key.storedIssueId())
    }

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

    /** Creates one of the collector's own groupings over the types they picked (ADR 0021 §11). */
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
 * Resolves a plate against the current state. A plate opens on evidence (ADR 0021 §7): its
 * collection exists, a catalog matches it and at least one official type is owned. Evidence is by
 * type even for date runs, so a plate stays open while years are missing. Plates of the shelf
 * window open too (ADR 0030).
 *
 * It only reads maps the assembly already built (#537, #539), so it is cheap enough to call once
 * per printed card. It takes the whole [Curation] the assembly was made with, not a slice of it, so
 * a plate is never resolved against another curation's albums.
 */
fun resolvePlate(
    state: CollectionState,
    curation: Curation,
    catalogId: String,
): PlateResult {
    val catalog = curation.catalogs.firstOrNull { it.id == catalogId }
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    // The assembly's album, never a second one (#537), so the plate counts casillas as its card
    // does. Missing means another curation assembled the state: a wiring mistake, reported like an
    // unknown id.
    val album = state.albums[catalog]
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    // The assembly's standings (#539), not a second reading of the programme files.
    val programmes = state.programmeStandings[catalog]
    // Asked first: a catalog the collector owns nothing of has no derived collection, so the checks
    // below would refuse it (ADR 0030 §1, ADR 0021 §7 as amended).
    showcasePlate(catalog, album, state.evidencedCatalogIds)?.let { window ->
        return PlateResult.Available(
            catalog = catalog,
            album = window.album,
            // Programme progress is the collector's, not the plate's, so window plates carry it too.
            programmes = programmes,
            mine = false,
        )
    }
    return when {
        state.derivedCollectionFor(catalog.key()) == null ->
            PlateResult.Unavailable(PlateUnavailable.NotACollection)
        catalog.id !in state.evidencedCatalogIds ->
            PlateResult.Unavailable(PlateUnavailable.NoEvidence)
        else -> PlateResult.Available(catalog, album, programmes)
    }
}
