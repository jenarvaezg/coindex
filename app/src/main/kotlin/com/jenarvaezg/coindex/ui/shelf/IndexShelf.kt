package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery
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

/**
 * Whatever the five chips can be asked about: a card of the index, or one loose piece (#275).
 *
 * The shelf grew a second kind of subject when the notebook learned to print the coins no
 * collection claims: that lámina is narrowed by the same chips as everything else, so a piece is
 * measured **as if it were a card of one piece with no plate**. Three of the five it answers off
 * its own ficha; the other two it answers the way a card with no catalog already does.
 *
 * [countries] is the país facet: for a plate it is every member's cured country (ADR 0023 / #415),
 * so the chip row speaks the same names the country axis paints. A card or loose piece with one
 * issuer keeps a single-element set; none means the subject has no country to filter by.
 *
 * [weight] is the one that is nullable here and not on a card. A card with no single weight is a
 * box or a set — [OunceBand.Spanning], «Conjunto o caja» — and a loose coin whose ficha declares no
 * weight is neither: it has **no answer**, so it falls out of any weight filter rather than
 * disguising itself as a box.
 */
interface ShelfSubject {
    val countries: Set<String>
    val weight: OunceBand?
    val startsIn: StartBand
    val status: PlateStatus
    val series: SeriesStatus?
}

/** What the shelf of the index is narrowing by, and in what order it leaves what is left. */
data class IndexShelf(
    val sort: IndexSort = IndexSort.MostComplete,
    /**
     * How the sheet is ordered (ADR 0026 §9): by plate, by country or by year.
     *
     * A facet and not a filter — it does not narrow — so the folded line does not count it among
     * them. The default is today's Collections; the summary names it only when it is not that one.
     */
    val axis: NotebookAxis = NotebookAxis.ByPlate,
    val issuer: String? = null,
    val weight: OunceBand? = null,
    val startsIn: StartBand? = null,
    val status: PlateStatus? = null,
    val series: SeriesStatus? = null,
) {
    internal fun matches(subject: ShelfSubject, except: IndexFacet? = null): Boolean =
        (except == IndexFacet.Issuer || issuer == null || issuer in subject.countries) &&
            (except == IndexFacet.Weight || weight == null || subject.weight == weight) &&
            (except == IndexFacet.StartsIn || startsIn == null || subject.startsIn == startsIn) &&
            (except == IndexFacet.Status || status == null || subject.status == status) &&
            (except == IndexFacet.Series || series == null || subject.series == series)
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
    override val countries: Set<String>,
    override val weight: OunceBand,
    override val startsIn: StartBand,
    override val status: PlateStatus,
    override val series: SeriesStatus?,
    /**
     * The highest Numista row id among its pieces, which is the only thing that can order by age.
     *
     * `collected_items` carries no date of any kind — not of purchase, not of entry — so this
     * orders by **when Numista saw the piece**, and the screen says so wherever the order is on.
     * Naming it after the id rather than after a date is what keeps that from being forgotten.
     */
    val latestRowId: Long,
    val haystack: String,
) : ShelfSubject

/**
 * Everything the shelf of the index needs, joined once from the state the screens already read.
 *
 * [catalogs] lets the país facet read **member** countries for evidenced plates — the same cure the
 * country axis already applies (#415 / ADR 0023) — instead of only the card eyebrow, which for a
 * spanning catalog is one header and not the issuers its casillas live in.
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
            memberCountry(catalog, member, typeMeta)
        }.toSet()
        if (fromMembers.isNotEmpty()) return fromMembers
    }
    return setOfNotNull(card.issuer)
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
