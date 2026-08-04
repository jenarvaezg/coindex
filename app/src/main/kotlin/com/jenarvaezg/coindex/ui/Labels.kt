package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.UnclassifiedReason

/**
 * `1000` reads as "1 oz", `250` as "0,25 oz", `804` as "0,804 oz". An absent weight is a set
 * issued as a set, which has no single weight to show (ADR 0012).
 */
fun weightLabel(weightMillioz: Int?): String {
    if (weightMillioz == null) return "Conjunto de varias denominaciones"
    val whole = weightMillioz / 1_000
    val fraction = (weightMillioz % 1_000).toString().padStart(3, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole oz" else "$whole,$fraction oz"
}

/** The finish as a specification value, under a row whose label already says «acabado». */
fun finishLabel(finish: Finish?): String = when (finish) {
    null -> "Sin confirmar"
    Finish.Bullion -> "Bullion"
    Finish.Proof -> "Proof"
    Finish.Coloured -> "Coloreado"
    Finish.ProofColoured -> "Proof coloreado"
    Finish.Gilded -> "Dorado"
    Finish.Antiqued -> "Envejecido"
}

/**
 * The same finish where nothing around it says what is unconfirmed.
 *
 * «0,611 oz · Por confirmar» read as a finish called that, or as a variant awaiting review; the
 * unconfirmed thing is the acabado, and on a line of its own it has to say so.
 */
fun standaloneFinishLabel(finish: Finish?): String =
    if (finish == null) "Acabado sin confirmar" else finishLabel(finish)

/** The metal as a specification value. Null is unread prose, not an alloy without a name. */
fun metalLabel(metal: Metal?): String = when (metal) {
    null -> "Sin confirmar"
    Metal.Gold -> "Oro"
    Metal.Silver -> "Plata"
    Metal.Platinum -> "Platino"
    Metal.Palladium -> "Paladio"
    Metal.Copper -> "Cobre"
    Metal.Bronze -> "Bronce"
    Metal.Brass -> "Latón"
    Metal.Cupronickel -> "Cuproníquel"
    Metal.Nickel -> "Níquel"
    Metal.Steel -> "Acero"
    Metal.Zinc -> "Cinc"
    Metal.Aluminium -> "Aluminio"
    Metal.Other -> "Sin metal dominante"
}

/**
 * The physical variant in one line. A set issued as a set has neither weight nor finish to
 * show, so it says what it is instead of showing two blanks (ADR 0012).
 *
 * The metal is named only when it is not silver (#40). Silver is what almost every card of these
 * two collections is made of — 73 of the 75 cards measured — so printing it everywhere would
 * add a word to every line to distinguish nothing, while «Oro» on the one card that is gold is
 * the whole reason the metal entered the key.
 */
fun variantLabel(weightMillioz: Int?, finish: Finish?, metal: Metal?): String {
    if (weightMillioz == null) return weightLabel(null)
    val line = "${weightLabel(weightMillioz)} · ${standaloneFinishLabel(finish)}"
    return if (metal == null || metal == Metal.Silver) line else "$line · ${metalLabel(metal)}"
}

/** Weight and finish as specification rows, omitted entirely for a set. */
fun variantEntries(weightMillioz: Int?, finish: Finish?): List<Pair<String, String>> =
    if (weightMillioz == null) {
        listOf("Variante" to weightLabel(null))
    } else {
        listOf("Peso" to weightLabel(weightMillioz), "Acabado" to finishLabel(finish))
    }

/** «1 pieza» / «22 piezas». Spanish counts nothing in the singular, so zero takes the plural. */
fun plural(count: Int, singular: String, plural: String): String =
    if (count == 1) "$count $singular" else "$count $plural"

/** The API budget is counted on the index, in the settings screen and in the sync report. */
fun callsLabel(count: Int): String = plural(count, "llamada", "llamadas")

fun countLabel(distinctTypes: Int, quantity: Int): String =
    plural(distinctTypes, "tipo distinto", "tipos distintos") +
        " · " + plural(quantity, "pieza", "piezas")

/**
 * The third line of a card that has an issue list: `4 de 12 · te faltan 8` (ADR 0021 §3).
 *
 * It is the same measured fact the index is sorted by (§6), which is why it has to be legible: an
 * order that moves with the ratio while every card counts pieces would look arbitrary. A card
 * without an issue list says [countLabel] instead, and there is no third phrase apologising for the
 * absence — «emisión» is curator's jargon, and the missing progress *is* the signal.
 *
 * With nothing missing the sentence stops at the count. «Completa» would claim a closure the
 * catalog does not have: by ADR 0020 an open series has no completeness to claim, and this January
 * the same 22/22 becomes 22/23.
 */
fun coverageLabel(coverage: CoverageRatio): String {
    val counted = "${coverage.owned} de ${coverage.issued}"
    return if (coverage.nothingMissing) counted else "$counted · te faltan ${coverage.missing}"
}

/**
 * The identity line of one inventory row: what tells it apart, its Numista type and how many
 * pieces it is.
 *
 * The year goes first, because it is usually what tells two rows of the same type apart, and
 * «sin año» is a fact about the row rather than a blank: a date run can never fill a year from a
 * row that does not carry one. [emissionLabel] replaces it where the year distinguishes nothing —
 * the six 100 pesetas of Franco all say 1966 and differ by the star.
 */
fun pieceLine(item: CollectedItem, emissionLabel: String? = null): String {
    val head = emissionLabel ?: item.recordedYear?.toString() ?: "Sin año"
    val quantity = if (item.quantity > 1) " · ×${item.quantity}" else ""
    return "$head · Numista ${item.typeId}$quantity"
}

/** Why a plate cannot be opened, in terms of what the collector can do about it. */
fun plateUnavailableLabel(reason: PlateUnavailable): String = when (reason) {
    PlateUnavailable.UnknownCatalog -> "No existe ese catálogo curado."
    PlateUnavailable.NotACollection ->
        "Ya no tienes piezas de esta variante, así que esa colección no existe."
    PlateUnavailable.NoEvidence -> "Aún no tienes ninguna emisión oficial de este catálogo."
}

/** Nothing is discarded in silence: every ungrouped piece says why. */
fun unclassifiedReasonLabel(reason: UnclassifiedReason): String = when (reason) {
    UnclassifiedReason.MissingTypeMetadata ->
        "Ficha del tipo sin descargar: se completará en el próximo sincronizado."
    UnclassifiedReason.NoFamilyOrCatalog ->
        "Sin familia en Numista y sin catálogo curado que la referencie: candidata a catálogo."
    UnclassifiedReason.IssueNotClaimedByCatalog ->
        "Sin una emisión de Numista incluida en los catálogos curados de este tipo."
    is UnclassifiedReason.UnknownWeight ->
        "«${reason.family}» sin peso en Numista: no se puede identificar la variante física."
}

fun numistaTypeUrl(typeId: Int): String = "https://en.numista.com/catalogue/pieces$typeId.html"
