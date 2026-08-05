package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass

/**
 * Whether any collection claims a coin.
 *
 * [InNone] is the chip «Sin colección», which is what the masthead button «Sin clasificar» always
 * was (ADR 0021 §1): the same coins, reached from the hierarchy where they already live instead of
 * from a screen that existed to apologise for them.
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
    val issuer: String? = null,
    val weight: GramBand? = null,
    val year: YearBand? = null,
    val objectClass: ObjectClass? = null,
    val membership: Membership? = null,
) {
    val active: Int
        get() = listOfNotNull(issuer, weight, year, objectClass, membership).size

    internal fun matches(row: CoinRow, except: CoinsFacet? = null): Boolean =
        (except == CoinsFacet.Issuer || issuer == null || row.issuer == issuer) &&
            (except == CoinsFacet.Weight || weight == null || GramBand.of(row.weightOz) == weight) &&
            (except == CoinsFacet.Year || year == null || YearBand.of(row.year) == year) &&
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
    if (row.claims.isEmpty()) Membership.InNone else Membership.InSome

/**
 * The coins this shelf and this query leave, in the list's own reading order.
 *
 * The order is [coinRows]'s and is never touched here: Coins carries filters and a search but no
 * sort selector — the prototype's shelf says «Filtros» on this side and «Filtros y orden» on the
 * other, because the index has a ratio to rank by and a list of coins does not.
 */
fun CoinsShelf.narrow(rows: List<CoinRow>, query: String): List<CoinRow> =
    rows.filter { row -> matches(row) && matchesQuery(row.haystack, query) }

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
        year = facetCounts(rows, keeping(CoinsFacet.Year)) { YearBand.of(it.year) },
        objectClass = facetCounts(rows, keeping(CoinsFacet.Class)) { it.objectClass },
        membership = facetCounts(rows, keeping(CoinsFacet.Membership), ::membershipOf),
    )
}

data class CoinsFacetCounts(
    val issuer: FacetCounts<String>,
    val weight: FacetCounts<GramBand>,
    val year: FacetCounts<YearBand>,
    val objectClass: FacetCounts<ObjectClass>,
    val membership: FacetCounts<Membership>,
) {
    /**
     * The countries worth a chip, the fullest first.
     *
     * Every country with at least one coin is offered and none is dropped: the shelf is folded on
     * entry (ADR 0021 §1), so a long list costs nothing until the collector opens it, and a silent
     * top-eight would read as «you own nothing from Serbia».
     */
    fun issuers(): List<Pair<String, Int>> = issuer.populated()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
}
