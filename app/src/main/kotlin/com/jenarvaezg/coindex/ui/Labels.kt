package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.PlateUnavailable
import com.jenarvaezg.coindex.domain.CoverageRatio
import com.jenarvaezg.coindex.domain.Finish
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.ounceLabel
import com.jenarvaezg.coindex.domain.SeriesStatus

const val UNKNOWN_YEAR_LABEL: String = "Sin año"

/**
 * `1000` reads as "1 oz", `250` as "0,25 oz", `804` as "0,804 oz". An absent weight is a set
 * issued as a set, which has no single weight to show (ADR 0012). The figure is [ounceLabel],
 * shared with the names that disambiguate by weight (#565).
 */
fun weightLabel(weightMillioz: Int?): String {
    if (weightMillioz == null) return "Conjunto de varias denominaciones"
    return ounceLabel(weightMillioz)
}

/**
 * A declared finish (#409). An unmarked type has an unknown finish (ADR 0005), and nothing is
 * printed for it: «Sin confirmar» on most cards read as an app half filled in and distinguished
 * nothing, while plates that differ by finish still print the declared word. Not «normal» either:
 * `inferFinish` also returns null for a proof whose title omits the word.
 */
fun finishLabel(finish: Finish): String = when (finish) {
    Finish.Bullion -> "Bullion"
    Finish.Proof -> "Proof"
    Finish.Coloured -> "Coloreado"
    Finish.ProofColoured -> "Proof coloreado"
    Finish.Gilded -> "Dorado"
    Finish.Antiqued -> "Envejecido"
}

/**
 * Whether a catalog's series is still being issued, as the two chips of the index's shelf. Only a
 * filter: cards never print it (ADR 0021 §3).
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
 * The physical variant in one line. A set issued as a set has neither weight nor finish, so it
 * says what it is instead (ADR 0012). The metal is named only when it isn't silver, which nearly
 * every card is (#40).
 */
fun variantLabel(weightMillioz: Int?, finish: Finish?, metal: Metal?): String {
    if (weightMillioz == null) return weightLabel(null)
    return listOfNotNull(
        weightLabel(weightMillioz),
        finish?.let(::finishLabel),
        metal?.takeUnless { it == Metal.Silver }?.let(::metalLabel),
    ).joinToString(" · ")
}

/**
 * The variant and finish as specification rows. No «Acabado» row without a declared finish
 * ([finishLabel]).
 *
 * The weight row is «Variante», not «Peso» (#511): the figure is ADR 0018's variant key in troy
 * ounces, while the coin's own weight is given in grams (see `Bands.kt`). Grams converted back from
 * the key are too imprecise to agree with the curated names, so they aren't printed here.
 */
fun variantEntries(weightMillioz: Int?, finish: Finish?): List<Pair<String, String>> =
    listOfNotNull(
        "Variante" to weightLabel(weightMillioz),
        // A set declares no finish (ADR 0012).
        finish?.takeIf { weightMillioz != null }?.let { declared -> "Acabado" to finishLabel(declared) },
    )

/** «1 pieza» / «22 piezas». Spanish counts nothing in the singular, so zero takes the plural. */
fun plural(count: Int, singular: String, plural: String): String =
    if (count == 1) "$count $singular" else "$count $plural"

/**
 * What the app spends at Numista, in its one unit: «consultas», never «llamadas» (#516). Shared by
 * the sync report, the line under the sync button, the ficha refresh and a lámina's tasación, which
 * all spend the same monthly budget (see also `WishLabels.MARK_HINT`).
 */
fun queriesLabel(count: Int): String = plural(count, "consulta", "consultas")

/**
 * What a collection with no issue list counts: «5 monedas · 5 tipos» (ADR 0021 §3). Coins first,
 * as the collector has them; types second, to tell five different coins from one five times.
 */
fun countLabel(distinctTypes: Int, quantity: Int): String =
    plural(quantity, "moneda", "monedas") + " · " + plural(distinctTypes, "tipo", "tipos")

/**
 * The third line of a card that has an issue list: `4 de 12 · te faltan 8` (ADR 0021 §3). The index
 * is sorted by it (§6), so it has to be shown. A card without an issue list says [countLabel] and
 * nothing about the absence.
 *
 * With nothing missing it stops at the count: «Completa» would claim a closure an open series
 * doesn't have (ADR 0020), and next January 22/22 becomes 22/23.
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
 * The year first, since it usually tells two rows of a type apart; «sin año» is a fact about the
 * row. [DrawnPiece.emissionLabel] replaces it where the year distinguishes nothing, as with
 * Franco's 100 pesetas, all dated 1966 and told apart by the star.
 */
fun pieceLine(piece: DrawnPiece): String {
    val item = piece.item
    val head = piece.emissionLabel ?: item.recordedYear?.toString() ?: UNKNOWN_YEAR_LABEL
    val quantity = if (item.quantity > 1) " · ×${item.quantity}" else ""
    return "$head · Numista ${item.typeId}$quantity"
}

/** What tapping a casilla does, for screen readers only; nothing on the sheet prints it. */
const val TURN_THE_COIN_OVER: String = "Dar la vuelta a la moneda"

/**
 * What a turned hole says when the face that came round has no photograph on this phone yet
 * (#509), instead of a blank disc that reads as a broken image. The only copy inside a hole, short
 * enough for 104 dp. Always temporary: the catalogue has both faces, and ADR 0024 only prefetches
 * on an unmetered network.
 */
const val FACE_NOT_DOWNLOADED: String = "Esta cara no ha bajado todavía"

/**
 * The `contentDescription` of the mark on a hole whose photograph hasn't arrived (#510). The mark
 * has no text because it falls on every hole of a plate at once. Not used for a type that simply
 * has no picture in Numista.
 */
const val PHOTO_NOT_DOWNLOADED: String = "La foto no ha bajado todavía"

/**
 * The completion stamp's one word (ADR 0026 §3), for open series too: a second word such as
 * «al día» would be new vocabulary to explain (ADR 0026 §5). The stamp prints it in capitals.
 */
const val COMPLETE_STAMP_WORD: String = "completa"

/** Why a plate cannot be opened, in terms of what the collector can do about it. */
fun plateUnavailableLabel(reason: PlateUnavailable): String = when (reason) {
    PlateUnavailable.UnknownCatalog -> "No existe ese catálogo curado."
    // The same sentence the collection says when it goes (ADR 0026 §5).
    PlateUnavailable.NotACollection -> COLLECTION_NO_LONGER_EXISTS
    PlateUnavailable.NoEvidence -> "Aún no tienes ninguna emisión oficial de este catálogo."
}

/**
 * What a coin of Coins is, said only when it isn't a coin: exonumia is a small minority, and a
 * medal filed beside coins is worth knowing first (ADR 0021 §1). Why a piece produced no
 * collection is never shown; that lives in the field report (ADR 0021 §12).
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

/**
 * Backing out, in one word wherever it is offered (ADR 0026 §5): the export sheet, an export in
 * progress, naming a box, a selection.
 */
const val CANCEL_ACTION: String = "Cancelar"
