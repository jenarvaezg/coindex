package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.SeriesStatus

const val UNKNOWN_YEAR_LABEL: String = "Sin año"

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

/**
 * Whether a catalog's series is still being issued, as the two chips of the index's shelf.
 *
 * Only ever a filter (ADR 0021 §3): the card itself says what it does and never a word about its
 * curation, and «abierta» on sixty cards would be curator's vocabulary printed sixty times.
 */
fun seriesLabel(status: SeriesStatus): String = when (status) {
    SeriesStatus.Open -> "Abierta"
    SeriesStatus.Closed -> "Cerrada"
}

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
 * The metal is named only when it is not silver (#40). Silver is what all but a handful of the cards
 * of these two collections are made of, so printing it everywhere would add a word to every line to
 * distinguish nothing, while «Oro» on the card that is gold is the whole reason the metal entered
 * the key.
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

/** A count of API calls, used in sync and refresh outcomes. */
fun callsLabel(count: Int): String = plural(count, "llamada", "llamadas")

/**
 * What a collection with no issue list counts: **«5 monedas · 5 tipos»** (ADR 0021 §3).
 *
 * The coins come first because that is what the collector has in the house, and the types second
 * because that is what tells «five different coins» from «the same one five times». It used to read
 * «5 tipos distintos · 5 piezas», which put the curator's unit first and spent two words —
 * «distinto», «pieza» — on saying what «tipo» and «moneda» already say.
 */
fun countLabel(distinctTypes: Int, quantity: Int): String =
    plural(quantity, "moneda", "monedas") + " · " + plural(distinctTypes, "tipo", "tipos")

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

/** The compact fraction printed beneath a collection hole in the album index. */
fun indexCoverageLabel(coverage: CoverageRatio): String =
    "${coverage.owned}/${coverage.issued}"

/**
 * The identity line of one inventory row: what tells it apart, its Numista type and how many
 * pieces it is.
 *
 * The year goes first, because it is usually what tells two rows of the same type apart, and
 * «sin año» is a fact about the row rather than a blank: a date run can never fill a year from a
 * row that does not carry one. [DrawnPiece.emissionLabel] replaces it where the year distinguishes
 * nothing — the 100 pesetas of Franco all say 1966 and differ by the star.
 *
 * It takes a [DrawnPiece] and not a row plus an optional label, for the reason that type states.
 */
fun pieceLine(piece: DrawnPiece): String {
    val item = piece.item
    val head = piece.emissionLabel ?: item.recordedYear?.toString() ?: UNKNOWN_YEAR_LABEL
    val quantity = if (item.quantity > 1) " · ×${item.quantity}" else ""
    return "$head · Numista ${item.typeId}$quantity"
}

/**
 * What tapping the body of a casilla does, for the reader that cannot see it happen.
 *
 * Nothing on the sheet writes this: the gesture is announced by the coin having two sides and by
 * nothing else, which is the whole point of a hole you can press. It exists so that the one reader
 * who is told what a control does is told the truth.
 */
const val TURN_THE_COIN_OVER: String = "Dar la vuelta a la moneda"

/**
 * The one word the completion stamp says (ADR 0026 §3).
 *
 * **One word and not two, open series included.** The #304 prototype drew «al día» for a plate whose
 * series is still being issued and that is the only thing the ticket never got to pronounce; it is
 * closed here by default, because the case does not exist today — none of the father's six complete
 * plates is an open series — and a second word is new vocabulary that would have to be explained on
 * the one screen ADR 0026 §5 was pruning words out of.
 *
 * Written in lower case because that is the word; the stamp prints it in capitals, which is a fact
 * about rubber and not about the copy.
 */
const val COMPLETE_STAMP_WORD: String = "completa"

/** Why a plate cannot be opened, in terms of what the collector can do about it. */
fun plateUnavailableLabel(reason: PlateUnavailable): String = when (reason) {
    PlateUnavailable.UnknownCatalog -> "No existe ese catálogo curado."
    PlateUnavailable.NotACollection ->
        "Ya no tienes piezas de esta variante, así que esa colección no existe."
    PlateUnavailable.NoEvidence -> "Aún no tienes ninguna emisión oficial de este catálogo."
}

/**
 * What a coin of Coins is, said only when it is not a coin.
 *
 * Exonumia is a small minority of the seeded types, so printing «Moneda» on all the others would add
 * a word to almost every row to distinguish nothing — the same reasoning that keeps «plata» off
 * [variantLabel]. What earns a line is the medal, because a struck thing filed beside coins is exactly
 * what a collector wants to know before reading the rest of the row (ADR 0021 §1).
 *
 * There is **no reason line** here and nowhere else in the app: ADR 0021 §12 moved the four reasons a
 * piece produced no collection out to the field report, where the curator already looks. «Nothing is
 * discarded silently» became «nothing is discarded» the moment a coin got a hierarchy of its own.
 */
fun objectClassLabel(objectClass: ObjectClass): String? = when (objectClass) {
    ObjectClass.Coin -> null
    ObjectClass.Exonumia -> "Medalla o ficha"
}

/** The same split as the two chips of the class facet, where both sides have to be named. */
fun objectClassChip(objectClass: ObjectClass): String = when (objectClass) {
    ObjectClass.Coin -> "Monedas"
    ObjectClass.Exonumia -> "Medallas y fichas"
}

fun numistaTypeUrl(typeId: Int): String = "https://en.numista.com/catalogue/pieces$typeId.html"
