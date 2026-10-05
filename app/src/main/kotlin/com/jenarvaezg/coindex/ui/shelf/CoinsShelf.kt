package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery

/**
 * Whether every piece of a coin is in some collection. [InNone] is the «Sin colección» chip, which
 * replaced the «Sin clasificar» screen (ADR 0021 §1).
 *
 * Decided per piece, not per type ([CoinRow.unclaimedPieces]): a type with a collection can still
 * have an unclaimed piece.
 */
enum class Membership(val label: String) {
    InSome("En alguna colección"),
    InNone("Sin colección"),
}

/**
 * What the Monedas shelf narrows by; a null facet is its «all» chip.
 *
 * The search query is kept apart because filters persist between launches and the query doesn't
 * (ADR 0021 §1).
 */
data class CoinsShelf(
    val sort: CoinSort = CoinSort.ByCountry,
    /**
     * The notebook axis (ADR 0026 §8–§9), shared with the index. Not a filter, so not counted in
     * [active] nor named on the folded line.
     */
    val axis: NotebookAxis = NotebookAxis.ByPlate,
    val issuer: String? = null,
    val weight: GramBand? = null,
    val year: YearFilter? = null,
    val objectClass: ObjectClass? = null,
    val membership: Membership? = null,
) {
    val active: Int
        get() = listOfNotNull(issuer, weight, year, objectClass, membership).size

    /** Clears the filters, keeping axis and sort, as in the index (#515). */
    fun withoutFilters(): CoinsShelf =
        copy(issuer = null, weight = null, year = null, objectClass = null, membership = null)

    internal fun matches(row: CoinRow, except: CoinsFacet? = null): Boolean =
        (except == CoinsFacet.Issuer || issuer == null || row.issuer == issuer) &&
            (except == CoinsFacet.Weight || weight == null || GramBand.of(row.weightOz) == weight) &&
            (except == CoinsFacet.Year || year == null || year in row.yearFilters) &&
            (
                except == CoinsFacet.Class ||
                    objectClass == null ||
                    row.objectClass == objectClass
                ) &&
            (except == CoinsFacet.Membership || membership == null || membershipOf(row) == membership)
}

/** The filter facets, named so each can be counted with its own choice dropped. */
enum class CoinsFacet { Issuer, Weight, Year, Class, Membership }

private fun membershipOf(row: CoinRow): Membership =
    if (row.unclaimedPieces > 0) Membership.InNone else Membership.InSome

/**
 * The Monedas sort orders (ADR 0021 §1). The default is the field-notebook reading order; the rest
 * answer questions about the whole pile, and «Más piezas» surfaces duplicates.
 */
enum class CoinSort(val label: String) {
    ByCountry("Por país"),
    Alphabetical("Alfabético"),
    Newest("Año más reciente"),
    Oldest("Año más antiguo"),
    Heaviest("Más pesadas"),
    MostPieces("Más piezas"),
}

/** The coins this shelf and query leave, ordered by the shelf's axis and sort. */
fun CoinsShelf.narrow(rows: List<CoinRow>, query: String): List<CoinRow> = rows
    .filter { row -> matches(row) && matchesQuery(row.haystack, query) }
    .sortedWith(coinSortOrder(effectiveSort()))

/**
 * Off the plate axis, the axis sets the order (ADR 0026 §8–§9); the chosen sort applies only on the
 * default axis.
 */
private fun CoinsShelf.effectiveSort(): CoinSort = when (axis) {
    NotebookAxis.ByPlate -> sort
    NotebookAxis.ByCountry -> CoinSort.ByCountry
    NotebookAxis.ByYear -> CoinSort.Newest
}

/**
 * Each order refines the default one: `sortedWith` is stable and [coinRows] returns reading order,
 * so ties fall back to country and year. Unknown values go last.
 */
private fun coinSortOrder(sort: CoinSort): Comparator<CoinRow> = when (sort) {
    CoinSort.ByCountry -> Comparator { _, _ -> 0 }
    CoinSort.Alphabetical -> coinTitleOrder()
    // Newest sorts by a row's latest year and Oldest by its earliest, since a row can span years.
    CoinSort.Newest -> compareBy<CoinRow> { it.newestYear == null }
        .thenByDescending { it.newestYear ?: 0 }
    CoinSort.Oldest -> compareBy<CoinRow> { it.oldestYear == null }
        .thenBy { it.oldestYear ?: 0 }
    CoinSort.Heaviest -> compareBy<CoinRow> { it.weightOz == null }
        .thenByDescending { it.weightOz ?: 0.0 }
    CoinSort.MostPieces -> compareByDescending { it.quantity }
}

/** Every chip's live count, each facet measured with its own choice dropped. */
fun coinsFacetCounts(
    rows: List<CoinRow>,
    shelf: CoinsShelf,
    query: String,
): CoinsFacetCounts {
    fun keeping(facet: CoinsFacet): (CoinRow) -> Boolean = { row ->
        shelf.matches(row, except = facet) && matchesQuery(row.haystack, query)
    }
    return CoinsFacetCounts(
        issuer = facetCounts(rows, keeping(CoinsFacet.Issuer)) { it.issuer },
        weight = facetCounts(rows, keeping(CoinsFacet.Weight)) { GramBand.of(it.weightOz) },
        year = facetCountsOfEach(rows, keeping(CoinsFacet.Year)) { it.yearFilters },
        objectClass = facetCounts(rows, keeping(CoinsFacet.Class)) { it.objectClass },
        membership = facetCounts(rows, keeping(CoinsFacet.Membership), ::membershipOf),
    )
}

data class CoinsFacetCounts(
    val issuer: FacetCounts<String>,
    val weight: FacetCounts<GramBand>,
    val year: FacetCounts<YearFilter>,
    val objectClass: FacetCounts<ObjectClass>,
    val membership: FacetCounts<Membership>,
)
