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
     * stamped with the day it arrived (ADR 0025).
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
        /**
         * Whether this plate is the collector's, or one of the twenty of the shelf window (ADR 0030).
         *
         * **The one bit that tells the two régimes apart**, and it is derived from the evidence on every
         * read like everything else about a card: nothing is stored to make a plate somebody's. What
         * hangs off it is what the plate *offers* — «Exportar la lámina» against «Tasar esta lámina» —
         * and how many figures of money its header can have: a plate holding nothing has no «Valor
         * actual», so its one figure is the cost of entering (§6).
         */
        val mine: Boolean = true,
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
    private val priceDao: PriceDao,
    private val wishDao: WishDao,
    /** The curated files, tied once, and the only door into the domain (#217). */
    val curation: Curation,
) {
    /**
     * Every price and the spot, as one value (ADR 0028).
     *
     * Kept apart from [observeState] rather than folded into it, and the seam is the arrival time: the
     * collection changes when the collector syncs and the prices change while a background pass runs, so
     * combining them would rebuild the whole index — 1.600 fichas through the domain — once per price
     * row that lands.
     */
    fun observePrices(): Flow<PriceBook> = combine(
        priceDao.observePrices(),
        priceDao.observeSpot(SILVER_SYMBOL),
        priceDao.observeTypeIssueReads(),
        priceDao.observeTypeIssues(),
        // When each price landed, which is what an amount that never expires is shown with
        // (ADR 0030 §4). By the same door as the prices themselves, for the reason the listings are:
        // a figure and its date read a moment apart is a date that belongs to another total.
        priceDao.observeReads(),
    ) { prices, spot, reads, issues, priceReads ->
        // Held and not fresh (#493): what a screen does with a listing is address a price it already
        // has, and ADR 0028 §5 keeps showing an expired row rather than emptying the page.
        priceBook(prices, spot?.toDomain(), IssueListings.held(reads, issues), priceReads, reads)
    }
    /**
     * The casillas the collector marked, as the domain reads them (ADR 0029).
     *
     * Apart from [observeState] for the same reason the prices are: **nothing that reads inventory
     * joins this table** (ADR 0029 §3), and folding it in would rebuild the whole index — sixteen
     * hundred fichas through the domain — every time a mark is toggled. What crosses the two is the
     * one reading that has to, `wishedSlots`, and it happens where a screen asks for it.
     */
    fun observeWishes(): Flow<List<Wish>> =
        wishDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /**
     * Marks one empty casilla, or leaves the mark it already had (ADR 0029 §5).
     *
     * The clock is the repository's, like a box's `createdAt`: [Wish.markedAt] is the one order the
     * list has, and an insert that is ignored keeps the date of the first mark — marking twice is not
     * an event, and it must not reshuffle the list under the collector's thumb.
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
 * **It resolves nothing and reads everything** (#539). The album is the assembly's (#537) and so are
 * the programme standings, so what is left here is the three questions of ADR 0021 §7 asked of maps
 * that are already built. That is what makes it safe to call once per printed card: the notebook
 * walks sixty-seven of them, and a function that re-derived a reading on each would be re-deriving
 * the same answer sixty-seven times.
 *
 * Takes the whole [Curation] rather than the catalog list alone: it is the same object the assembly
 * was made with, and passing a slice of it is how a caller ends up resolving a plate of one curation
 * against the albums of another — the mistake the null below is there to name.
 */
fun resolvePlate(
    state: CollectionState,
    curation: Curation,
    catalogId: String,
): PlateResult {
    val catalog = curation.catalogs.firstOrNull { it.id == catalogId }
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    // The album the assembly built for this catalog, and never a second one (#537): the card the
    // collector tapped divided by this instance, so the plate cannot count its casillas its own way.
    // Absent means this collection was assembled by another curation, which is a wiring mistake and
    // not a state of the world — it is said as plainly as an id nobody shipped.
    val album = state.albums[catalog]
        ?: return PlateResult.Unavailable(PlateUnavailable.UnknownCatalog)
    // The standings the assembly resolved for this catalog (#539), never a second reading of the
    // programme files. This function is called once per printed card, and re-deriving thirteen
    // programmes against the whole inventory sixty-seven times answered the same thing every time:
    // what a standing is made of is the snapshot, and the snapshot is what was assembled.
    val programmes = state.programmeStandings[catalog]
    // The shelf window is asked first, and it has to be: a catalog the collector owns nothing of has no
    // derived collection either, so both of the answers below would refuse it before the evidence was
    // ever the question (ADR 0030 §1, ADR 0021 §7 as amended).
    showcasePlate(catalog, album, state.evidencedCatalogIds)?.let { window ->
        return PlateResult.Available(
            catalog = catalog,
            album = window.album,
            // The collector's progress in the programme is theirs and not this plate's, so a plate of
            // the window carries it too: what changes there is what it offers, not what it knows.
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
