package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.ui.objectClassChip
import com.jenarvaezg.coindex.ui.plural
import com.jenarvaezg.coindex.ui.seriesLabel

const val SEARCH_PLACEHOLDER: String = "Buscar"

/**
 * The way out of a typed search, which until #414 was the backspace key held down.
 *
 * The query is the one narrowing that does **not** survive a launch (ADR 0021 §1), but it does
 * survive walking into a collection and back, so «The» typed once keeps hiding the rest of the
 * shelf for as long as the collector stays on the screen. The aspa is what the shelf's own
 * [CLEAR_FILTERS_ACTION] is for the chips: one tap back to everything.
 */
const val SEARCH_CLEAR_LABEL: String = "Borrar la búsqueda"

/**
 * The chip that means «no filter on this facet», in one word across the ten of them (ADR 0026 §5).
 *
 * Ten chips said six different words — `Todos`, `Todas`, `Todo`, `Cualquiera`, `Cualquier año`, `Da
 * igual` — because each one was written next to the facet it belonged to and agreed with **that
 * facet's** noun. Read as a shelf they are six ways of saying nothing is narrowed, and a collector
 * scanning for the way back to everything had to read six labels to find the same button.
 *
 * `Cualquiera` and not `Todos`: it agrees with nothing in particular, which is exactly what a chip
 * standing in for an absent filter has to do, and it is the only one of the six that says «pick from
 * all of them» rather than «all of them at once» — the chip does not select every country, it stops
 * selecting by country.
 */
const val ANY_FILTER: String = "Cualquiera"

/**
 * What the facets are called, and they are called the same thing on both shelves.
 *
 * Collections and Coins are two hierarchies of one notebook (ADR 0021 §1), and a facet that filters
 * by país is «País» on both: one string is what stops the pair from drifting into «Emisor» on one
 * side. What differs between the shelves is which facets each one has, never their names.
 */
const val AXIS_FACET: String = "Eje"
const val SORT_FACET: String = "Orden"
const val COUNTRY_FACET: String = "País"
const val WEIGHT_FACET: String = "Peso"
const val YEAR_FACET: String = "Año"
const val STARTS_IN_FACET: String = "Empieza en"
const val STATUS_FACET: String = "Estado"
const val SERIES_FACET: String = "Serie"
const val CLASS_FACET: String = "Clase"
const val MEMBERSHIP_FACET: String = "Colección"

/** The way out of a shelf that has hidden everything, offered on the spot on both sides. */
const val CLEAR_FILTERS_ACTION: String = "Quitar los filtros"

/**
 * Why nothing is showing, which is three questions and not one.
 *
 * Reading the collection off the database takes a frame or two, and «todavía no hay colecciones» in
 * that gap is a lie about a collection that is on the device already. A shelf that hides everything
 * is the third case, and it owes the way out on the spot ([CLEAR_FILTERS_ACTION]).
 */
fun indexEmptyLabel(loading: Boolean, anyCollections: Boolean): String =
    if (loading) {
        "Leyendo tu colección…"
    } else {
        emptyShelfLabel("colección", "colecciones", narrowed = anyCollections)
    }

/** The same on the other side, where there is nothing to read off the database first. */
fun coinsEmptyLabel(anyCoins: Boolean): String =
    emptyShelfLabel("moneda", "monedas", narrowed = anyCoins)

/**
 * One pair of sentences for both shelves, which only Spanish makes possible.
 *
 * «Ninguna colección» and «Ninguna moneda» are both feminine singular, and «colecciones» and
 * «monedas» both feminine plural, so the two nouns drop into one shape — the same accident of gender
 * that lets [SharedSheet] carry one export sentence for the plate and the hoja. It is a fact about
 * Spanish and not a guarantee: a masculine grain added here would read «Ninguna tipo», and what
 * guards against that is this note.
 */
private fun emptyShelfLabel(singular: String, plural: String, narrowed: Boolean): String =
    if (narrowed) {
        "Ninguna $singular pasa por lo que has puesto."
    } else {
        "Todavía no hay $plural. Sincroniza para traer tu colección de Numista."
    }

/**
 * Why «recién añadidas» is not «recién compradas».
 *
 * Numista's `collected_items` carries no date of any kind, so the order is by row id — «alta en
 * Numista», not «compra». Said where the order is chosen rather than left to be guessed at from a
 * surprising result.
 */
const val RECENTLY_ADDED_NOTE: String =
    "Numista no guarda fecha de compra, así que este orden es el del alta en Numista."

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
 * The filters are not: the axis is one chip in the first row of an opened shelf, and a chosen year
 * is eight rows down behind a calendar of them, which is the walk #414 is about.
 */
fun indexShelfSummary(shelf: IndexShelf, expanded: Boolean = false): String =
    shelfSummary(
        filters = shelf.namedFilters(),
        sort = shelf.sort.label.takeIf { shelf.sort != IndexSort.MostComplete },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/** The same line on the other side, which carries a sort of its own (ADR 0021 §1). */
fun coinsShelfSummary(shelf: CoinsShelf, expanded: Boolean = false): String =
    shelfSummary(
        filters = shelf.namedFilters(),
        sort = shelf.sort.label.takeIf { shelf.sort != CoinSort.ByCountry },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/**
 * Which chips are chosen, in the order the shelf paints their rows (#414).
 *
 * The count alone — «1 filtro» — said that something was narrowing and left the collector to open
 * the shelf and hunt the green chip through every row, which on Monedas is a calendar of years. The
 * order is the rows' own so that the line and the open shelf are read downwards alike.
 */
private fun IndexShelf.namedFilters(): List<String> = listOfNotNull(
    issuer?.let { named(COUNTRY_FACET, it) },
    weight?.let { named(WEIGHT_FACET, it.label) },
    startsIn?.let { named(STARTS_IN_FACET, it.label) },
    status?.let { named(STATUS_FACET, it.label) },
    // «Cerrada» is an adjective and nothing else: it means nothing away from the eyebrow it was
    // written under, so it takes its facet's noun however it begins.
    series?.let { "$SERIES_FACET ${seriesLabel(it)}" },
)

/** The same on the other side, whose five rows are the ones of [CoinsFacet]. */
private fun CoinsShelf.namedFilters(): List<String> = listOfNotNull(
    issuer?.let { named(COUNTRY_FACET, it) },
    weight?.let { named(WEIGHT_FACET, it.label) },
    year?.let { named(YEAR_FACET, it.label) },
    // The other one that cannot stand alone, and for the opposite reason: «Monedas» is a word this
    // screen is already called, so on its own line it reads as the screen and not as a filter.
    objectClass?.let { "$CLASS_FACET ${objectClassChip(it)}" },
    membership?.let { named(MEMBERSHIP_FACET, it.label) },
)

/**
 * One chosen chip as the folded line has to carry it: «Año 1960», «Venezuela», «Sin peso».
 *
 * A chip label is written to be read **under its facet's eyebrow**, and out here there is no
 * eyebrow. The ones that need the facet's name are the ones that do not begin with a word of their
 * own — «1960», «1950 – 1999», «½ – 1 oz» — while «Sin colección», «Antes de 1950» and «A medias»
 * say which facet they answer already, and «Colección Sin colección» would be the same mistake
 * facing the other way. Serie and Clase are the two the rule cannot reach, and they say so where
 * they are named.
 */
private fun named(facet: String, label: String): String =
    if (label.firstOrNull()?.isLetter() == true) label else "$facet $label"

/** Folded name of a non-default axis: «Eje País», never «eje por país». */
private fun NotebookAxis.summaryName(): String? = when (this) {
    NotebookAxis.ByPlate -> null
    NotebookAxis.ByCountry -> "País"
    NotebookAxis.ByYear -> "Año"
}

/**
 * One shape for both sides, so the two shelves cannot start describing themselves differently.
 *
 * The sort and the axis are named only when they are not the default: choosing the order the screen
 * would have used anyway is not a deviation to announce.
 *
 * The count leads and the names follow it, which is the order of sacrifice: the line is one line
 * and truncates from the right (see [FilterShelf]), so what a shelf narrowed five ways loses first
 * is the sort and the axis at the tail, then the last chips named — never «5 filtros».
 */
private fun shelfSummary(filters: List<String>, sort: String?, axis: String? = null): String {
    val counted = filters.takeIf { it.isNotEmpty() }?.let { plural(it.size, "filtro", "filtros") }
    val order = sort?.let { "orden ${it.lowercase()}" }
    val namedAxis = axis?.let { "Eje $it" }
    val parts = listOfNotNull(counted) + filters + listOfNotNull(order, namedAxis)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Filtros y orden"
}

/**
 * How much of a list is showing, beside the shelf line, whatever the list is made of.
 *
 * It says «N de M» only while something is narrowed, because «58 de 58» invites the reader to look
 * for the filter that is not there. One sentence for the four tallies: the rótulo holds its place
 * while the axis changes the magnitude under it, so what must not change with the axis is the shape
 * of the phrase.
 */
private fun tally(shown: Int, total: Int, one: String, many: String): String =
    if (shown == total) plural(total, one, many) else "$shown de ${plural(total, one, many)}"

/** How many years of the axis carry something the collector owns. */
fun yearAxisTally(ownedYears: Int, totalYears: Int): String =
    tally(ownedYears, totalYears, "año", "años")

/**
 * Rust mark on a year seat when more than one piece lands there (#406).
 *
 * Same «×N» shape as Monedas, so the calendar and the list say multiplicity the same way — and a
 * bare digit was too easy to miss against a dense decade row.
 */
fun yearAxisQuantityMark(quantity: Int): String? =
    "×$quantity".takeIf { quantity > 1 }

/**
 * How many measurable slots the country axis is showing against the sheet's total.
 *
 * Named, and named in the year axis's own shape (#416): the rótulo keeps its place while the
 * magnitude under it changes with the axis, so «70 colecciones» turning into «170/678» was a
 * fraction of nothing in particular offered beside an export button. A ratio is the vocabulary
 * *inside* the sheet, where the block it sits on says which country is being divided; out here the
 * unit has to travel with the number.
 */
fun countryAxisTally(ownedSlots: Int, totalSlots: Int): String =
    tally(ownedSlots, totalSlots, "casilla", "casillas")

/**
 * The fold at the end of a country's absences (#417).
 *
 * Says the number and not «ver más», because the number is the finding: the row of holes above it
 * already shows what an absence looks like, so what the collector still does not know is how many
 * there are. Folded it reads as the sentence the ratio started —«Venezuela 42/115 … y faltan 66»—
 * and open it names the way back.
 */
fun countryAxisFoldLabel(hidden: Int, expanded: Boolean): String = when {
    expanded && hidden == 1 -> "Plegar la que falta"
    expanded -> "Plegar las $hidden"
    hidden == 1 -> "… y falta 1"
    else -> "… y faltan $hidden"
}

/** What the fold does when touched, for a collector who cannot see the holes it opens. */
fun countryAxisFoldAction(hidden: Int, expanded: Boolean): String = if (expanded) {
    "Volver a plegar las casillas que faltan"
} else {
    "Ver ${plural(hidden, "casilla que falta", "casillas que faltan")}"
}

fun indexTally(shown: Int, total: Int): String = tally(shown, total, "colección", "colecciones")

fun coinsTally(shown: Int, total: Int): String = tally(shown, total, "tipo", "tipos")
