package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.ui.plural

/**
 * What the folded shelf says about itself.
 *
 * The shelf enters folded (ADR 0021 §1), so this line is the only thing standing between a filter
 * set days ago and a collector wondering where half their collection went: it has to say that
 * something is on, and what. «Filtros y orden» is the resting state, and it names the sort only when
 * the sort is not the one the index would have used anyway (§6).
 */
fun indexShelfSummary(shelf: IndexShelf): String =
    shelfSummary(
        active = shelf.active,
        sort = shelf.sort.label.takeIf { shelf.sort != IndexSort.MostComplete },
    )

/** The same line on the other side, which carries a sort of its own (ADR 0021 §1). */
fun coinsShelfSummary(shelf: CoinsShelf): String =
    shelfSummary(
        active = shelf.active,
        sort = shelf.sort.label.takeIf { shelf.sort != CoinSort.ByCountry },
    )

/**
 * One shape for both sides, so the two shelves cannot start describing themselves differently.
 *
 * The sort is named only when it is not the default: choosing the order the screen would have used
 * anyway is not a deviation to announce.
 */
private fun shelfSummary(active: Int, sort: String?): String {
    val filters = active.takeIf { it > 0 }?.let { plural(it, "filtro", "filtros") }
    val order = sort?.let { "orden ${it.lowercase()}" }
    return listOfNotNull(filters, order).takeIf { it.isNotEmpty() }?.joinToString(" · ")
        ?: "Filtros y orden"
}

/**
 * How much of the list is showing, beside the shelf line.
 *
 * It says «N de M» only while something is narrowed, because «58 de 58» invites the reader to look
 * for the filter that is not there.
 */
private fun tally(shown: Int, total: Int, one: String, many: String): String =
    if (shown == total) plural(total, one, many) else "$shown de ${plural(total, one, many)}"

fun indexTally(shown: Int, total: Int): String = tally(shown, total, "colección", "colecciones")

fun coinsTally(shown: Int, total: Int): String = tally(shown, total, "tipo", "tipos")
