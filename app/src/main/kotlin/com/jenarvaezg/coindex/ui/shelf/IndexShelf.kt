package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.countryOf
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery
import com.jenarvaezg.coindex.ui.variantLabel
import java.text.Collator
import java.util.Locale

/**
 * How the index is ordered. «Más completas» is ADR 0021 §6's order and the default; every other
 * entry is the collector overriding it.
 */
enum class IndexSort(val label: String) {
    MostComplete("Más completas"),
    LeastComplete("Menos completas"),
    Alphabetical("Alfabético"),
    Heaviest("Más pesadas"),
    MostPieces("Más piezas"),
    RecentlyAdded("Alta más reciente"),
}

/**
 * The «estado» facet: complete, partly done, or no plate. It mirrors ADR 0021 §3's capability
 * split, so it says nothing the card doesn't already show.
 */
enum class PlateStatus(val label: String) {
    Complete("Completas"),
    PartlyDone("A medias"),
    NoPlate("Sin lámina"),
}

/**
 * What the index's chips filter: a card, or a loose piece (#275), so the notebook's loose-coin
 * lámina narrows like everything else. A loose piece is treated as a one-piece card with no plate.
 *
 * [countries] is the país facet: for a plate, every member's cured country (ADR 0023, #415), as the
 * country axis paints them; empty when there is none.
 *
 * [weight] is null only for a loose coin with no recorded weight, which then matches no weight
 * filter. A card without a single weight is [OunceBand.Spanning] instead.
 */
interface ShelfSubject {
    val countries: Set<String>
    val weight: OunceBand?
    val startsIn: StartBand
    val status: PlateStatus
    val series: SeriesStatus?
}

/** What the index shelf narrows by, and how it sorts the result. */
data class IndexShelf(
    val sort: IndexSort = IndexSort.MostComplete,
    /**
     * By plate, country or year (ADR 0026 §9). Not a filter, so not counted on the folded line,
     * which names it only when it isn't the default.
     */
    val axis: NotebookAxis = NotebookAxis.ByPlate,
    val issuer: String? = null,
    val weight: OunceBand? = null,
    val startsIn: StartBand? = null,
    val status: PlateStatus? = null,
    val series: SeriesStatus? = null,
) {
    /** Number of chosen filter chips (#515). */
    val active: Int
        get() = listOfNotNull(issuer, weight, startsIn, status, series).size

    /**
     * Clears the filters only, which is what «Quitar los filtros» promises (#515): `IndexShelf()`
     * would also reset the axis and sort.
     */
    fun withoutFilters(): IndexShelf =
        copy(issuer = null, weight = null, startsIn = null, status = null, series = null)

    internal fun matches(subject: ShelfSubject, except: IndexFacet? = null): Boolean =
        (except == IndexFacet.Issuer || issuer == null || issuer in subject.countries) &&
            (except == IndexFacet.Weight || weight == null || subject.weight == weight) &&
            (except == IndexFacet.StartsIn || startsIn == null || subject.startsIn == startsIn) &&
            (except == IndexFacet.Status || status == null || subject.status == status) &&
            (except == IndexFacet.Series || series == null || subject.series == series)
}

/** The filter facets, named so each can be counted with its own choice dropped. */
enum class IndexFacet { Issuer, Weight, StartsIn, Status, Series }

/**
 * One index card reduced to what the shelf asks about, precomputed once rather than per chip
 * count: the start year joins `itemsByKey` with the type cache.
 */
data class IndexFacts(
    val card: IndexCard,
    override val countries: Set<String>,
    override val weight: OunceBand,
    override val startsIn: StartBand,
    override val status: PlateStatus,
    override val series: SeriesStatus?,
    /**
     * The highest Numista row id among its pieces, the only proxy for age: `collected_items` has no
     * dates. It orders by when Numista recorded the piece, and the screen says so.
     */
    val latestRowId: Long,
    val haystack: String,
) : ShelfSubject

/**
 * The index shelf's facts, joined once. [catalogs] lets the país facet use member countries for
 * plates (#415, ADR 0023) instead of the card's single eyebrow.
 */
fun indexFacts(
    state: CollectionState,
    catalogs: List<CollectionCatalog> = emptyList(),
): List<IndexFacts> {
    val catalogsById = catalogs.associateBy { it.id }
    return state.index.map { card ->
        val coverage = card.coverage
        val pieces = when (card) {
            is IndexCard.Derived -> state.itemsByKey[card.key].orEmpty()
            is IndexCard.Box -> card.box.items
        }
        val variant = (card as? IndexCard.Derived)?.collection?.let { collection ->
            variantLabel(collection.weightMillioz, collection.finish, collection.metal)
        }
        val countries = countriesOf(card, catalogsById, state.typeMeta)
        IndexFacts(
            card = card,
            countries = countries,
            weight = OunceBand.of((card as? IndexCard.Derived)?.collection?.weightMillioz),
            startsIn = StartBand.of(
                pieces.mapNotNull { piece -> state.typeMeta[piece.typeId]?.minYear }.minOrNull(),
            ),
            status = when {
                coverage == null -> PlateStatus.NoPlate
                coverage.nothingMissing -> PlateStatus.Complete
                else -> PlateStatus.PartlyDone
            },
            series = (card as? IndexCard.Derived)?.seriesStatus,
            latestRowId = pieces.maxOfOrNull { it.id } ?: 0L,
            haystack = fold(
                listOfNotNull(card.name, variant).plus(countries).joinToString(" "),
            ),
        )
    }
}

/**
 * The countries a card belongs to for the país chip: every member of its plate when there is one,
 * else the single eyebrow the card already carries.
 */
internal fun countriesOf(
    card: IndexCard,
    catalogsById: Map<String, CollectionCatalog>,
    typeMeta: Map<Int, TypeMeta>,
): Set<String> {
    val catalog = (card as? IndexCard.Derived)?.plateCatalogId?.let { catalogsById[it] }
    if (catalog != null) {
        val fromMembers = catalog.members.mapNotNull { member ->
            catalog.countryOf(member, typeMeta)
        }.toSet()
        if (fromMembers.isNotEmpty()) return fromMembers
    }
    return setOfNotNull(card.issuer)
}

/** The cards this shelf and query leave, in the shelf's sort order. */
fun IndexShelf.narrow(facts: List<IndexFacts>, query: String): List<IndexCard> = facts
    .filter { matches(it) && matchesQuery(it.haystack, query) }
    .sortedWith(sortOrder(sort))
    .map { it.card }

/**
 * Each order refines the default: `sortedWith` is stable and the facts arrive in the domain's order
 * (ADR 0021 §6), so ties fall back to it without restating it.
 */
private fun sortOrder(sort: IndexSort): Comparator<IndexFacts> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return when (sort) {
        IndexSort.MostComplete -> Comparator { _, _ -> 0 }
        IndexSort.LeastComplete -> compareByDescending<IndexFacts> { it.card.coverage != null }
            .thenBy { it.card.coverage?.value ?: 0.0 }
        IndexSort.Alphabetical -> compareBy(collator) { it.card.name }
        // Collections without a single weight go last rather than sorting as zero.
        IndexSort.Heaviest -> compareBy<IndexFacts> { it.weight == OunceBand.Spanning }
            .thenByDescending { (it.card as? IndexCard.Derived)?.collection?.weightMillioz ?: 0 }
        IndexSort.MostPieces -> compareByDescending { it.card.quantity }
        IndexSort.RecentlyAdded -> compareByDescending { it.latestRowId }
    }
}

/** Every chip's live count, each facet measured with its own choice dropped. */
fun indexFacetCounts(
    facts: List<IndexFacts>,
    shelf: IndexShelf,
    query: String,
): IndexFacetCounts {
    fun keeping(facet: IndexFacet): (IndexFacts) -> Boolean = { row ->
        shelf.matches(row, except = facet) && matchesQuery(row.haystack, query)
    }
    return IndexFacetCounts(
        issuer = facetCountsOfEach(facts, keeping(IndexFacet.Issuer)) { it.countries.toList() },
        weight = facetCounts(facts, keeping(IndexFacet.Weight)) { it.weight },
        startsIn = facetCounts(facts, keeping(IndexFacet.StartsIn)) { it.startsIn },
        status = facetCounts(facts, keeping(IndexFacet.Status)) { it.status },
        series = facetCounts(facts, keeping(IndexFacet.Series)) { it.series },
    )
}

data class IndexFacetCounts(
    val issuer: FacetCounts<String>,
    val weight: FacetCounts<OunceBand>,
    val startsIn: FacetCounts<StartBand>,
    val status: FacetCounts<PlateStatus>,
    val series: FacetCounts<SeriesStatus>,
)
