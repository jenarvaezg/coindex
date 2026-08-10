package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.ui.plural

const val SEARCH_PLACEHOLDER: String = "Buscar"
const val SHELF_ACTION_SEPARATOR: String = " · "

fun shelfDisclosure(expanded: Boolean): String = if (expanded) "▾ " else "▸ "

/**
 * What the folded shelf says about itself.
 *
 * The shelf enters folded (ADR 0021 §1), so this line is the only thing standing between a filter
 * set days ago and a collector wondering where half their collection went: it has to say that
 * something is on, and what. «Filtros y orden» is the resting state, and it names the sort only when
 * the sort is not the one the index would have used anyway (§6).
 *
 * The axis is named the same way — only when it is not «por lámina» — and **only while the shelf is
 * folded** (ADR 0026 §9 / atlas-315): open, the chip is in view, so the line stays quiet about it.
 */
fun indexShelfSummary(shelf: IndexShelf, expanded: Boolean = false): String =
    shelfSummary(
        active = shelf.active,
        sort = shelf.sort.label.takeIf { shelf.sort != IndexSort.MostComplete },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/** The same line on the other side, which carries a sort of its own (ADR 0021 §1). */
fun coinsShelfSummary(shelf: CoinsShelf, expanded: Boolean = false): String =
    shelfSummary(
        active = shelf.active,
        sort = shelf.sort.label.takeIf { shelf.sort != CoinSort.ByCountry },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/** Folded name of a non-default axis: «eje país», never «eje por país». */
private fun NotebookAxis.summaryName(): String? = when (this) {
    NotebookAxis.ByPlate -> null
    NotebookAxis.ByCountry -> "país"
    NotebookAxis.ByYear -> "año"
}

/**
 * One shape for both sides, so the two shelves cannot start describing themselves differently.
 *
 * The sort and the axis are named only when they are not the default: choosing the order the screen
 * would have used anyway is not a deviation to announce.
 */
private fun shelfSummary(active: Int, sort: String?, axis: String? = null): String {
    val filters = active.takeIf { it > 0 }?.let { plural(it, "filtro", "filtros") }
    val order = sort?.let { "orden ${it.lowercase()}" }
    val namedAxis = axis?.let { "eje $it" }
    return listOfNotNull(filters, order, namedAxis).takeIf { it.isNotEmpty() }?.joinToString(" · ")
        ?: "Filtros y orden"
}

/** How many years of the axis carry something the collector owns. */
fun yearAxisTally(ownedYears: Int, totalYears: Int): String =
    if (ownedYears == totalYears) {
        plural(totalYears, "año", "años")
    } else {
        "$ownedYears de ${plural(totalYears, "año", "años")}"
    }

/** How many measurable slots the country axis is showing against the sheet's total. */
fun countryAxisTally(ownedSlots: Int, totalSlots: Int): String =
    if (ownedSlots == totalSlots) {
        plural(totalSlots, "casilla", "casillas")
    } else {
        "$ownedSlots/$totalSlots"
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
