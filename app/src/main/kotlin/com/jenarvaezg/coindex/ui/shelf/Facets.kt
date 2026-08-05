package com.jenarvaezg.coindex.ui.shelf

/**
 * The live count behind one facet's chips (ADR 0021 §1).
 *
 * [total] is what the facet's «all» chip says, and it is the count with **this facet's own choice
 * dropped** and every other filter kept — which is what makes a shelf navigable: the number on a
 * chip is what you would get by tapping it, so a chip that says zero is a dead end you can see
 * before spending a tap on it.
 */
data class FacetCounts<T>(val total: Int, val byValue: Map<T, Int>) {
    fun of(value: T?): Int = if (value == null) total else byValue[value] ?: 0

    /** The values that would leave something, in the order they were counted. */
    fun populated(): List<Pair<T, Int>> = byValue.entries
        .filter { (_, count) -> count > 0 }
        .map { (value, count) -> value to count }
}

/**
 * The countries worth a chip, the fullest first — shared by both shelves.
 *
 * Every country with at least one row is offered and none is dropped: the shelf is folded on entry
 * (ADR 0021 §1), so a long list costs nothing until the collector opens it, and a silent top-eight
 * would read as «you own nothing from Serbia».
 */
fun FacetCounts<String>.issuers(): List<Pair<String, Int>> = populated()
    .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

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
