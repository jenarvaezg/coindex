package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus

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
 */
object ShelfCodec {
    const val INDEX_SORT = "index_sort"
    const val INDEX_ISSUER = "index_issuer"
    const val INDEX_WEIGHT = "index_weight"
    const val INDEX_STARTS_IN = "index_starts_in"
    const val INDEX_STATUS = "index_status"
    const val INDEX_SERIES = "index_series"

    const val COINS_SORT = "coins_sort"
    const val COINS_ISSUER = "coins_issuer"
    const val COINS_WEIGHT = "coins_weight"
    const val COINS_YEAR = "coins_year"
    const val COINS_CLASS = "coins_class"
    const val COINS_MEMBERSHIP = "coins_membership"

    fun encode(shelf: IndexShelf): Map<String, String?> = mapOf(
        // The default is written as the default and not as an absence, so «Más completas» chosen
        // on purpose and never chosen at all read back the same — which they are.
        INDEX_SORT to shelf.sort.name,
        INDEX_ISSUER to shelf.issuer,
        INDEX_WEIGHT to shelf.weight?.name,
        INDEX_STARTS_IN to shelf.startsIn?.name,
        INDEX_STATUS to shelf.status?.name,
        INDEX_SERIES to shelf.series?.name,
    )

    fun encode(shelf: CoinsShelf): Map<String, String?> = mapOf(
        COINS_SORT to shelf.sort.name,
        COINS_ISSUER to shelf.issuer,
        COINS_WEIGHT to shelf.weight?.name,
        COINS_YEAR to shelf.year?.name,
        COINS_CLASS to shelf.objectClass?.name,
        COINS_MEMBERSHIP to shelf.membership?.name,
    )

    fun decodeIndex(read: (String) -> String?): IndexShelf = IndexShelf(
        sort = named<IndexSort>(read(INDEX_SORT)) ?: IndexSort.MostComplete,
        issuer = read(INDEX_ISSUER)?.takeIf(String::isNotBlank),
        weight = named<OunceBand>(read(INDEX_WEIGHT)),
        startsIn = named<StartBand>(read(INDEX_STARTS_IN)),
        status = named<PlateStatus>(read(INDEX_STATUS)),
        series = named<SeriesStatus>(read(INDEX_SERIES)),
    )

    fun decodeCoins(read: (String) -> String?): CoinsShelf = CoinsShelf(
        sort = named<CoinSort>(read(COINS_SORT)) ?: CoinSort.ByCountry,
        issuer = read(COINS_ISSUER)?.takeIf(String::isNotBlank),
        weight = named<GramBand>(read(COINS_WEIGHT)),
        year = named<YearBand>(read(COINS_YEAR)),
        objectClass = named<ObjectClass>(read(COINS_CLASS)),
        membership = named<Membership>(read(COINS_MEMBERSHIP)),
    )

    /** An enum by name, or nothing: a name this version has never heard of is not a filter. */
    private inline fun <reified T : Enum<T>> named(stored: String?): T? =
        stored?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
}
