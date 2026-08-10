package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.readsAsACountry

/**
 * How a shelf survives a launch (ADR 0021 §1).
 *
 * Filters and sort persist and the search text does not, so the two live in different places by
 * construction: neither [CoinsShelf] nor [IndexShelf] holds a query, and this codec has no key for
 * one. Reopening the app with a stale word in the box and half the collection hidden was the
 * measured failure that decision came from.
 *
 * The encoding is one key per facet holding the enum's own name, rather than a packed string. It
 * costs nothing and buys the only property that matters here: a value written by an older or newer
 * version that this one does not recognise reads back as «no filter» instead of crashing or, worse,
 * silently selecting the wrong chip.
 *
 * **The country is the one facet that is not an enum**, so that property had to be earned rather than
 * inherited: it stores the label itself, and ADR 0023 retired nine of them. A phone that had
 * «Federación de Rusia (1991-presente)» selected would have reopened filtering on a string no row
 * produces any more — an empty list, a filter badge at 1 and no chip lit — so a stored country that
 * [readsAsACountry] rejects is read back as no filter. `russie` is the emisor of about a third of the
 * seeded fichas, which makes it the likeliest chip on the phone to have been left on.
 */
object ShelfCodec {
    const val INDEX_SORT = "index_sort"
    const val INDEX_AXIS = "index_axis"
    const val INDEX_ISSUER = "index_issuer"
    const val INDEX_WEIGHT = "index_weight"
    const val INDEX_STARTS_IN = "index_starts_in"
    const val INDEX_STATUS = "index_status"
    const val INDEX_SERIES = "index_series"

    const val COINS_SORT = "coins_sort"
    const val COINS_AXIS = "coins_axis"
    const val COINS_ISSUER = "coins_issuer"
    const val COINS_WEIGHT = "coins_weight"
    const val COINS_YEAR = "coins_year"
    const val COINS_CLASS = "coins_class"
    const val COINS_MEMBERSHIP = "coins_membership"

    /** Persistence key for [YearFilter.Undated] — not a Gregorian year string. */
    private const val UNDATED_YEAR = "Undated"

    fun encode(shelf: IndexShelf): Map<String, String?> = mapOf(
        // The default is written as the default and not as an absence, so «Más completas» chosen
        // on purpose and never chosen at all read back the same — which they are.
        INDEX_SORT to shelf.sort.name,
        INDEX_AXIS to shelf.axis.name,
        INDEX_ISSUER to shelf.issuer,
        INDEX_WEIGHT to shelf.weight?.name,
        INDEX_STARTS_IN to shelf.startsIn?.name,
        INDEX_STATUS to shelf.status?.name,
        INDEX_SERIES to shelf.series?.name,
    )

    fun encode(shelf: CoinsShelf): Map<String, String?> = mapOf(
        COINS_SORT to shelf.sort.name,
        COINS_AXIS to shelf.axis.name,
        COINS_ISSUER to shelf.issuer,
        COINS_WEIGHT to shelf.weight?.name,
        COINS_YEAR to yearKey(shelf.year),
        COINS_CLASS to shelf.objectClass?.name,
        COINS_MEMBERSHIP to shelf.membership?.name,
    )

    fun decodeIndex(read: (String) -> String?): IndexShelf = IndexShelf(
        sort = named<IndexSort>(read(INDEX_SORT)) ?: IndexSort.MostComplete,
        axis = named<NotebookAxis>(read(INDEX_AXIS)) ?: NotebookAxis.ByPlate,
        issuer = country(read(INDEX_ISSUER)),
        weight = named<OunceBand>(read(INDEX_WEIGHT)),
        startsIn = named<StartBand>(read(INDEX_STARTS_IN)),
        status = named<PlateStatus>(read(INDEX_STATUS)),
        series = named<SeriesStatus>(read(INDEX_SERIES)),
    )

    fun decodeCoins(read: (String) -> String?): CoinsShelf = CoinsShelf(
        sort = named<CoinSort>(read(COINS_SORT)) ?: CoinSort.ByCountry,
        axis = named<NotebookAxis>(read(COINS_AXIS)) ?: NotebookAxis.ByPlate,
        issuer = country(read(COINS_ISSUER)),
        weight = named<GramBand>(read(COINS_WEIGHT)),
        year = year(read(COINS_YEAR)),
        objectClass = named<ObjectClass>(read(COINS_CLASS)),
        membership = named<Membership>(read(COINS_MEMBERSHIP)),
    )

    /**
     * A stored country, or nothing: a label this version no longer paints is not a filter either.
     *
     * The migration of the nine labels ADR 0023 retired, and it needs no version key to run — the
     * chips are built from what the rows say, so nothing that fails [readsAsACountry] can be written
     * here again.
     */
    private fun country(stored: String?): String? =
        stored?.takeIf { it.isNotBlank() && readsAsACountry(it) }

    /**
     * A stored year filter: a Gregorian year, «Undated», or nothing.
     *
     * The year facet used to be four era names (`SinceTwoThousand`, …). Those strings are not years
     * and are not «Undated», so a phone that still has one selected reopens with no year filter —
     * the same bargain every unknown enum already made.
     */
    private fun year(stored: String?): YearFilter? = when {
        stored.isNullOrBlank() -> null
        stored == UNDATED_YEAR -> YearFilter.Undated
        else -> stored.toIntOrNull()?.let(YearFilter::Of)
    }

    private fun yearKey(year: YearFilter?): String? = when (year) {
        is YearFilter.Of -> year.year.toString()
        YearFilter.Undated -> UNDATED_YEAR
        null -> null
    }

    /** An enum by name, or nothing: a name this version has never heard of is not a filter. */
    private inline fun <reified T : Enum<T>> named(stored: String?): T? =
        stored?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
}
