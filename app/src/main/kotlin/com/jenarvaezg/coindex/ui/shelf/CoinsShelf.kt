package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery

/**
 * Whether every piece of a coin is in some collection.
 *
 * [InNone] is the chip «Sin colección», which is what the masthead button «Sin clasificar» always
 * was (ADR 0021 §1): the same coins, reached from the hierarchy where they already live instead of
 * from a screen that existed to apologise for them.
 *
 * It is decided **by piece and not by type**, so a coin whose sibling row fell in the residue is
 * under it even though the type has a collection — see [CoinRow.unclaimedPieces]. Read off `claims`
 * instead, the chip would have quietly stopped showing one of the father's two Silver Eagles, which
 * is the one thing this filter inherited from the screen it replaced.
 */
enum class Membership(val label: String) {
    InSome("En alguna colección"),
    InNone("Sin colección"),
}

/**
 * What the shelf of Coins is currently narrowing by. A null facet is that facet's «all» chip.
 *
 * Search is deliberately **not** a field of this type: filters persist between launches and the
 * query does not (ADR 0021 §1), so the thing that is stored and the thing that is typed are two
 * different values, and keeping them one would have made «reopen the app with half the collection
 * hidden» a one-line mistake.
 */
data class CoinsShelf(
    val sort: CoinSort = CoinSort.ByCountry,
    /**
     * How the list is ordered on the axis of the notebook (ADR 0026 §8–§9).
     *
     * Same facet the index carries: the two hierarchies with a list both gain it. It is not a
     * filter, so it is not counted in [active] nor named among them on the folded line.
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

    /** The chips dropped and the axis and sort kept, as on the other shelf (#515). */
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

/** The five chip rows of Coins, named so a facet can be counted with its own choice dropped. */
enum class CoinsFacet { Issuer, Weight, Year, Class, Membership }

private fun membershipOf(row: CoinRow): Membership =
    if (row.unclaimedPieces > 0) Membership.InNone else Membership.InSome

/**
 * How the list of coins is ordered (ADR 0021 §1: «both sides carry filters, **sorting** and a live
 * search»).
 *
 * The default is the reading order of a field notebook, which is the one Coins has no alternative
 * to: with no ratio to rank by, the collector arriving here is looking for a coin they can picture.
 * Everything else answers a question about the pile rather than about one coin — «what is the newest
 * thing I have?», «which is the heavy one?» — and «Más piezas» is where the duplicates surface.
 */
enum class CoinSort(val label: String) {
    ByCountry("Por país"),
    Alphabetical("Alfabético"),
    Newest("Año más reciente"),
    Oldest("Año más antiguo"),
    Heaviest("Más pesadas"),
    MostPieces("Más piezas"),
}

/** The coins this shelf and this query leave, in the order the shelf's axis and sort ask for. */
fun CoinsShelf.narrow(rows: List<CoinRow>, query: String): List<CoinRow> = rows
    .filter { row -> matches(row) && matchesQuery(row.haystack, query) }
    .sortedWith(coinSortOrder(effectiveSort()))

/**
 * The axis is the same facet Collections carries (ADR 0026 §8–§9): on Coins it chooses the reading
 * order when it is not «por lámina». «Por país» and «por año» are the field-notebook orders; the
 * collector's own sort only applies on the default axis.
 */
private fun CoinsShelf.effectiveSort(): CoinSort = when (axis) {
    NotebookAxis.ByPlate -> sort
    NotebookAxis.ByCountry -> CoinSort.ByCountry
    NotebookAxis.ByYear -> CoinSort.Newest
}

/**
 * Every order but the default is built on top of it, never instead of it.
 *
 * `sortedWith` is stable and [coinRows] already left the list in reading order, so «Más pesadas»
 * breaks its own ties by country and year without restating either. Unknowns go last in every order
 * that has one to place: an uncached type says less than a dated one, and opening the list on
 * whatever the last sync had not finished would be the wrong first screen.
 */
private fun coinSortOrder(sort: CoinSort): Comparator<CoinRow> = when (sort) {
    CoinSort.ByCountry -> Comparator { _, _ -> 0 }
    CoinSort.Alphabetical -> coinTitleOrder()
    // By the newest year it holds and the oldest respectively: a type spanning 1879 to 1936 is the
    // collection's oldest coin and also one of its later ones, and each order asks a different end.
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
