package com.jenarvaezg.coindex.ui.shelf

/**
 * The live counts behind one facet's chips (ADR 0021 §1), each computed with this facet's own
 * choice dropped and every other filter kept, so a chip's number is what tapping it gives. [total]
 * is the «all» chip. Chips that would leave nothing are not offered ([populated], [populatedIn]).
 */
data class FacetCounts<T>(val total: Int, val byValue: Map<T, Int>) {
    fun of(value: T?): Int = if (value == null) total else byValue[value] ?: 0

    /** Values that would leave something, in counting order. */
    fun populated(): List<Pair<T, Int>> = byValue.entries
        .filter { (_, count) -> count > 0 }
        .map { (value, count) -> value to count }

    /**
     * Values of [order] that would leave something, in [order]'s sequence, so «Sin peso» or «Sin
     * fecha» stay last. [keep], the current choice, stays visible even at zero, so the collector
     * can see what emptied the list and clear it.
     */
    fun populatedIn(order: Iterable<T>, keep: T? = null): List<Pair<T, Int>> = order.mapNotNull { value ->
        val count = of(value)
        if (count > 0 || value == keep) value to count else null
    }
}

/**
 * Every country with rows, most rows first; shared by both shelves. None is dropped: the shelf
 * opens folded (ADR 0021 §1), and a silent top-N would suggest the rest aren't owned.
 */
fun FacetCounts<String>.issuers(): List<Pair<String, Int>> = populated()
    .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

/**
 * Every year with rows for Monedas, newest first, «Sin año» last. Exact years rather than eras, so
 * a seat on the year axis and its chip agree.
 */
fun FacetCounts<YearFilter>.years(): List<Pair<YearFilter, Int>> {
    val dated = populated()
        .mapNotNull { (value, count) -> (value as? YearFilter.Of)?.let { it to count } }
        .sortedByDescending { (filter, _) -> filter.year }
    val undated = populated().filter { (value, _) -> value is YearFilter.Undated }
    return dated + undated
}

/**
 * Counts one facet over the rows the *other* filters leave, grouped by the value each row has.
 *
 * @param rows every row of the list, before this shelf narrowed anything
 * @param keep the whole shelf with this one facet neutralised
 * @param valueOf what this facet reads off a row; null means the row has no value for it
 */
internal fun <R, T> facetCounts(
    rows: List<R>,
    keep: (R) -> Boolean,
    valueOf: (R) -> T?,
): FacetCounts<T> {
    val kept = rows.filter(keep)
    val byValue = LinkedHashMap<T, Int>()
    for (row in kept) {
        val value = valueOf(row) ?: continue
        byValue[value] = (byValue[value] ?: 0) + 1
    }
    return FacetCounts(kept.size, byValue)
}

/**
 * [facetCounts] for a facet where a row can have several values (#448, #415), e.g. a coin held in
 * three years, or a plate spanning several countries. [FacetCounts.total] still counts rows.
 */
internal fun <R, T> facetCountsOfEach(
    rows: List<R>,
    keep: (R) -> Boolean,
    valuesOf: (R) -> List<T>,
): FacetCounts<T> {
    val kept = rows.filter(keep)
    val byValue = LinkedHashMap<T, Int>()
    for (row in kept) {
        for (value in valuesOf(row).distinct()) {
            byValue[value] = (byValue[value] ?: 0) + 1
        }
    }
    return FacetCounts(kept.size, byValue)
}
