package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.variantLabel
import java.text.Collator
import java.util.Locale

/**
 * How the index is ordered, with the comparator of ADR 0021 §6 as the default.
 *
 * A selector and not a preference: §6 accepted that the one order is *not* alphabetical, and §1 is
 * where that was paid for. So «Más completas» is the order the ADR decided and every other entry is
 * the collector overriding it on purpose, which is why the default is the one with no adjective of
 * its own.
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
 * What a card can be filtered by on the «estado» row.
 *
 * The three values are the two of the ratio plus its absence, which is exactly the capability split
 * of ADR 0021 §3 — so this facet says nothing the card does not already say out loud, and no word of
 * provenance sneaks back in through a chip.
 */
enum class PlateStatus(val label: String) {
    Complete("Completas"),
    PartlyDone("A medias"),
    NoPlate("Sin lámina"),
}

/** What the shelf of the index is narrowing by, and in what order it leaves what is left. */
data class IndexShelf(
    val sort: IndexSort = IndexSort.MostComplete,
    val issuer: String? = null,
    val weight: OunceBand? = null,
    val startsIn: StartBand? = null,
    val status: PlateStatus? = null,
    val series: SeriesStatus? = null,
) {
    val active: Int
        get() = listOfNotNull(issuer, weight, startsIn, status, series).size

    internal fun matches(facts: IndexFacts, except: IndexFacet? = null): Boolean =
        (except == IndexFacet.Issuer || issuer == null || facts.issuer == issuer) &&
            (except == IndexFacet.Weight || weight == null || facts.weight == weight) &&
            (except == IndexFacet.StartsIn || startsIn == null || facts.startsIn == startsIn) &&
            (except == IndexFacet.Status || status == null || facts.status == status) &&
            (except == IndexFacet.Series || series == null || facts.series == series)
}

/** The five chip rows of the index, named so a facet can be counted with its own choice dropped. */
enum class IndexFacet { Issuer, Weight, StartsIn, Status, Series }

/**
 * One card of the index reduced to what the shelf asks about.
 *
 * Precomputed once per redraw rather than read off the card each time a chip is counted: five facets
 * counted over sixty cards would otherwise walk the inventory thirty times, and the earliest year of
 * a collection is a join across `itemsByKey` and the type cache.
 */
data class IndexFacts(
    val card: IndexCard,
    val issuer: String?,
    val weight: OunceBand,
    val startsIn: StartBand,
    val status: PlateStatus,
    val series: SeriesStatus?,
    /**
     * The highest Numista row id among its pieces, which is the only thing that can order by age.
     *
     * `collected_items` carries no date of any kind — not of purchase, not of entry — so this
     * orders by **when Numista saw the piece**, and the screen says so wherever the order is on.
     * Naming it after the id rather than after a date is what keeps that from being forgotten.
     */
    val latestRowId: Long,
    val haystack: String,
)

/** Everything the shelf of the index needs, joined once from the state the screens already read. */
fun indexFacts(state: CollectionState): List<IndexFacts> = state.index.map { card ->
    val coverage = card.coverage
    val pieces = when (card) {
        is IndexCard.Derived -> state.itemsByKey[card.key].orEmpty()
        is IndexCard.Box -> card.box.items
    }
    val variant = (card as? IndexCard.Derived)?.collection?.let { collection ->
        variantLabel(collection.weightMillioz, collection.finish, collection.metal)
    }
    IndexFacts(
        card = card,
        issuer = card.issuer,
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
        haystack = fold(listOfNotNull(card.name, card.issuer, variant).joinToString(" ")),
    )
}

/** The cards this shelf and this query leave, in the order the shelf's sort asks for. */
fun IndexShelf.narrow(facts: List<IndexFacts>, query: String): List<IndexCard> = facts
    .filter { matches(it) && matchesQuery(it.haystack, query) }
    .sortedWith(sortOrder(sort))
    .map { it.card }

/**
 * Every order but the default is built on top of it, never instead of it.
 *
 * `sortedWith` is stable and the facts arrive in the order the domain comparator already put them
 * in (ADR 0021 §6), so «Más pesadas» breaks its own ties by ratio and then alphabetically without
 * restating either. That is also what keeps the selector from becoming a second definition of the
 * index's order, which §6 is explicit about being the domain's.
 */
private fun sortOrder(sort: IndexSort): Comparator<IndexFacts> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return when (sort) {
        IndexSort.MostComplete -> Comparator { _, _ -> 0 }
        IndexSort.LeastComplete -> compareByDescending<IndexFacts> { it.card.coverage != null }
            .thenBy { it.card.coverage?.value ?: 0.0 }
        IndexSort.Alphabetical -> compareBy(collator) { it.card.name }
        // A collection with no single weight has none to be heavy by, so it sits at the bottom
        // rather than at zero ounces: a box is not lighter than a quarter-ounce, it is unweighed.
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
        issuer = facetCounts(facts, keeping(IndexFacet.Issuer)) { it.issuer },
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
) {
    /** The countries worth a chip, the fullest first. None is dropped — see `CoinsFacetCounts`. */
    fun issuers(): List<Pair<String, Int>> = issuer.populated()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
}
