package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.readsAsACountry

/**
 * Persists a shelf across launches (ADR 0021 §1). Filters and sort persist; the search text
 * doesn't, so neither shelf type holds a query and this codec has no key for one.
 *
 * One key per facet holding the enum's name, so an unrecognised value from another version reads
 * back as no filter rather than crashing or selecting the wrong chip.
 *
 * The country is stored as its label, so it gets the same treatment explicitly: a stored country
 * that fails [readsAsACountry], such as a label retired by ADR 0023, reads back as no filter rather
 * than filtering on a string no row produces.
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
        // The default is written explicitly; chosen or not, it reads back the same.
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
     * A stored country, or null if this version no longer produces that label. This is also the
     * migration for the labels ADR 0023 retired; no version key needed.
     */
    private fun country(stored: String?): String? =
        stored?.takeIf { it.isNotBlank() && readsAsACountry(it) }

    /**
     * A stored year filter: a year, «Undated», or null. Values from the old era facet
     * (`SinceTwoThousand`, …) read back as null.
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

    /** An enum by name, or null for a name this version doesn't know. */
    private inline fun <reified T : Enum<T>> named(stored: String?): T? =
        stored?.let { name -> enumValues<T>().firstOrNull { it.name == name } }
}
