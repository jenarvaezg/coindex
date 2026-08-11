package com.jenarvaezg.coindex.ui.shelf

/**
 * The live count behind one facet's chips (ADR 0021 §1).
 *
 * [total] is what the facet's «all» chip says, and it is the count with **this facet's own choice
 * dropped** and every other filter kept — which is what makes a shelf navigable: the number on a
 * chip is what you would get by tapping it. A chip that would leave nobody is not offered
 * ([populated] / [populatedIn]): the count already conditioned on the other filters is enough
 * signal, and a tap into an empty list is not.
 */
data class FacetCounts<T>(val total: Int, val byValue: Map<T, Int>) {
    fun of(value: T?): Int = if (value == null) total else byValue[value] ?: 0

    /** The values that would leave something, in the order they were counted. */
    fun populated(): List<Pair<T, Int>> = byValue.entries
        .filter { (_, count) -> count > 0 }
        .map { (value, count) -> value to count }

    /**
     * The values of [order] that would leave something, keeping [order]'s sequence.
     *
     * Enum bands use this so «Sin peso» / «Sin fecha» stay last when they have anyone, instead of
     * drifting to first-seen-in-rows order the way [populated] would. [keep] is the shelf's current
     * choice of this facet: even at zero it stays visible so the collector can see what emptied the
     * list and clear it (via «Cualquiera»), rather than a selected chip vanishing under them.
     */
    fun populatedIn(order: Iterable<T>, keep: T? = null): List<Pair<T, Int>> = order.mapNotNull { value ->
        val count = of(value)
        if (count > 0 || value == keep) value to count else null
    }
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
 * The years worth a chip on Coins, newest first — «Sin año» last when it has anyone.
 *
 * Same bargain as [issuers]: every year with a row is offered, and the shelf being folded means a
 * calendar of chips costs nothing until opened. Era bands used to compress this into four buckets;
 * the year axis made that compression a lie the moment a seat opened Monedas on a decade it was not.
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
 * The same count for a facet a row can answer to in **more than one** value (#448).
 *
 * A coin held in three years belongs to three year chips, and tapping any of them has to find it —
 * the chip's number is a promise about what the tap returns. [FacetCounts.total] stays the number of
 * *rows*, not of values, so the «Cualquiera» chip keeps saying how many coins there are.
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
