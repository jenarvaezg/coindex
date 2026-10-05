package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.ui.objectClassChip
import com.jenarvaezg.coindex.ui.plural
import com.jenarvaezg.coindex.ui.seriesLabel

/**
 * Each search box names what it searches, since the three look identical (#515): index cards,
 * inventory types, and in «Explorar» the plates the collector owns nothing of. The possessive marks
 * the side: «tus colecciones», «tus monedas», but `ShowcaseLabels.SEARCH_PLACEHOLDER` says «las
 * láminas» (ADR 0030 §1).
 */
const val INDEX_SEARCH_PLACEHOLDER: String = "Buscar entre tus colecciones"

/** Monedas searches coins rather than cards (ADR 0021 §1). */
const val COINS_SEARCH_PLACEHOLDER: String = "Buscar entre tus monedas"

/**
 * The search box's clear button (#414). The query doesn't survive a launch (ADR 0021 §1) but does
 * survive navigating into a collection and back. Also the name the empty state uses for undoing a
 * search ([clearNarrowingAction]).
 */
const val SEARCH_CLEAR_LABEL: String = "Borrar la búsqueda"

/**
 * The «no filter on this facet» chip, one word for every facet (ADR 0026 §5). «Cualquiera» rather
 * than «Todos» because it agrees with no particular noun and says the chip stops filtering rather
 * than selecting everything.
 */
const val ANY_FILTER: String = "Cualquiera"

/**
 * Facet names, shared by both shelves (ADR 0021 §1) so they can't drift apart; the shelves differ
 * only in which facets they have.
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

/** Undo for a shelf the filter chips emptied. */
const val CLEAR_FILTERS_ACTION: String = "Quitar los filtros"

/** Undo for a shelf emptied by filters and search together (#515). */
const val CLEAR_EVERYTHING_ACTION: String = "Quitar los filtros y la búsqueda"

/**
 * What is narrowing a shelf, which the empty state names and undoes (#515). Filters and search are
 * different things: filters persist and hide in the folded shelf, the query is always visible and
 * gone next launch (ADR 0021 §1).
 */
enum class ShelfNarrowing { None, Filters, Search, Both }

fun shelfNarrowing(filters: Int, query: String): ShelfNarrowing = when {
    filters > 0 && query.isNotBlank() -> ShelfNarrowing.Both
    filters > 0 -> ShelfNarrowing.Filters
    query.isNotBlank() -> ShelfNarrowing.Search
    else -> ShelfNarrowing.None
}

/**
 * The index's empty state. While the database is still loading it says so, rather than claiming
 * there are no collections.
 */
fun indexEmptyLabel(loading: Boolean, anyCollections: Boolean, narrowing: ShelfNarrowing): String =
    if (loading) {
        "Leyendo tu colección…"
    } else {
        emptyShelfLabel("colección", "colecciones", anyCollections, narrowing)
    }

/** The Monedas empty state. */
fun coinsEmptyLabel(anyCoins: Boolean, narrowing: ShelfNarrowing): String =
    emptyShelfLabel("moneda", "monedas", anyCoins, narrowing)

/**
 * Shared empty-state sentences. This works because «colección» and «moneda» are both feminine; a
 * masculine noun would need its own sentences («Ninguna tipo»).
 *
 * «Lo que has puesto» only when both filters and search narrow; otherwise the sentence names which
 * one. [ShelfNarrowing.None] happens when a country or year axis is empty with no narrowing at all.
 */
private fun emptyShelfLabel(
    singular: String,
    plural: String,
    any: Boolean,
    narrowing: ShelfNarrowing,
): String = if (!any) {
    "Todavía no hay $plural. Sincroniza para traer tu colección de Numista."
} else {
    when (narrowing) {
        ShelfNarrowing.Filters -> "Ninguna $singular pasa por los filtros."
        ShelfNarrowing.Search -> "Ninguna $singular responde a lo que has escrito."
        ShelfNarrowing.Both -> "Ninguna $singular pasa por lo que has puesto."
        ShelfNarrowing.None -> "Ninguna $singular aparece en este eje."
    }
}

/**
 * The undo action for exactly what narrows (#515), or null when nothing does. A search alone is
 * offered under the clear button's name ([SEARCH_CLEAR_LABEL]), even with the box in view.
 */
fun clearNarrowingAction(narrowing: ShelfNarrowing): String? = when (narrowing) {
    ShelfNarrowing.None -> null
    ShelfNarrowing.Filters -> CLEAR_FILTERS_ACTION
    ShelfNarrowing.Search -> SEARCH_CLEAR_LABEL
    ShelfNarrowing.Both -> CLEAR_EVERYTHING_ACTION
}

/**
 * Explains that «Alta más reciente» is by Numista row id, not purchase date: `collected_items` has
 * no dates.
 */
const val RECENTLY_ADDED_NOTE: String =
    "Numista no guarda fecha de compra, así que este orden es el del alta en Numista."

fun shelfDisclosure(expanded: Boolean): String = if (expanded) "▾ " else "▸ "

/**
 * The folded shelf's one-line summary. The shelf opens folded (ADR 0021 §1), so this line must say
 * which filters are on. The sort is named only when it isn't the default (§6).
 *
 * The axis likewise, and only while folded (ADR 0026 §9, atlas-315): open, its chip is in the first
 * row. Filters stay named when open, since a chosen year may be many rows down (#414).
 */
fun indexShelfSummary(shelf: IndexShelf, expanded: Boolean = false): String =
    shelfSummary(
        filters = shelf.namedFilters(),
        sort = shelf.sort.label.takeIf { shelf.sort != IndexSort.MostComplete },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/** The Monedas summary, with its own default sort (ADR 0021 §1). */
fun coinsShelfSummary(shelf: CoinsShelf, expanded: Boolean = false): String =
    shelfSummary(
        filters = shelf.namedFilters(),
        sort = shelf.sort.label.takeIf { shelf.sort != CoinSort.ByCountry },
        axis = shelf.axis.summaryName().takeIf { !expanded },
    )

/**
 * The chosen chips by name, in the shelf's row order (#414), so the line and the open shelf read
 * alike.
 */
private fun IndexShelf.namedFilters(): List<String> = listOfNotNull(
    issuer?.let { named(COUNTRY_FACET, it) },
    weight?.let { named(WEIGHT_FACET, it.label) },
    startsIn?.let { named(STARTS_IN_FACET, it.label) },
    status?.let { named(STATUS_FACET, it.label) },
    // «Cerrada» means nothing without its facet, so the noun is always added.
    series?.let { "$SERIES_FACET ${seriesLabel(it)}" },
)

/** The Monedas chips by name, in [CoinsFacet] order. */
private fun CoinsShelf.namedFilters(): List<String> = listOfNotNull(
    issuer?.let { named(COUNTRY_FACET, it) },
    weight?.let { named(WEIGHT_FACET, it.label) },
    year?.let { named(YEAR_FACET, it.label) },
    // Always prefixed: «Monedas» alone would read as the screen's name.
    objectClass?.let { "$CLASS_FACET ${objectClassChip(it)}" },
    membership?.let { named(MEMBERSHIP_FACET, it.label) },
)

/**
 * One chosen chip for the folded line: «Año 1960», «Venezuela», «Sin peso». Labels starting with a
 * digit or symbol get their facet's name, since there is no eyebrow out here; labels starting with
 * a letter already say which facet they belong to. Serie and Clase are handled by their callers.
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
 * The summary line for both shelves. Sort and axis are named only when not the default. The count
 * comes first because the line truncates from the right (see [FilterShelf]): sort and axis go
 * first, then the last chip names, never the count.
 */
private fun shelfSummary(filters: List<String>, sort: String?, axis: String? = null): String {
    val counted = filters.takeIf { it.isNotEmpty() }?.let { plural(it.size, "filtro", "filtros") }
    val order = sort?.let { "orden ${it.lowercase()}" }
    val namedAxis = axis?.let { "Eje $it" }
    val parts = listOfNotNull(counted) + filters + listOfNotNull(order, namedAxis)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Filtros y orden"
}

/**
 * How much of a list is showing, next to the shelf line: «N de M» only while something narrows, or
 * just the total. All four tallies share this shape so it doesn't change with the axis.
 */
private fun tally(shown: Int, total: Int, one: String, many: String): String =
    if (shown == total) plural(total, one, many) else "$shown de ${plural(total, one, many)}"

/** Year-axis years holding something owned, out of all of them. */
fun yearAxisTally(ownedYears: Int, totalYears: Int): String =
    tally(ownedYears, totalYears, "año", "años")

/**
 * «×N» on a year seat holding more than one piece (#406), as in Monedas; a bare digit was easy to
 * miss.
 */
fun yearAxisQuantityMark(quantity: Int): String? =
    "×$quantity".takeIf { quantity > 1 }

/**
 * Owned measurable slots on the country axis, out of the total. Says «casillas» like the year axis
 * says «años» (#416): a bare ratio out here had no unit.
 */
fun countryAxisTally(ownedSlots: Int, totalSlots: Int): String =
    tally(ownedSlots, totalSlots, "casilla", "casillas")

/**
 * The fold label at the end of a country's absences (#417). It gives the hidden count rather than
 * «ver más», reading on from the ratio: «… y faltan 66». Open, it offers to fold them back.
 */
fun countryAxisFoldLabel(hidden: Int, expanded: Boolean): String = when {
    expanded && hidden == 1 -> "Plegar la que falta"
    expanded -> "Plegar las $hidden"
    hidden == 1 -> "… y falta 1"
    else -> "… y faltan $hidden"
}

/** The fold's click label for screen readers. */
fun countryAxisFoldAction(hidden: Int, expanded: Boolean): String = if (expanded) {
    "Volver a plegar las casillas que faltan"
} else {
    "Ver ${plural(hidden, "casilla que falta", "casillas que faltan")}"
}

fun indexTally(shown: Int, total: Int): String = tally(shown, total, "colección", "colecciones")

fun coinsTally(shown: Int, total: Int): String = tally(shown, total, "tipo", "tipos")
