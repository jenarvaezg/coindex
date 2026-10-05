package com.jenarvaezg.coindex.ui

/**
 * Every string «Lo que busco» prints (ADR 0026 §6, ADR 0029): «lo busco» on the casilla,
 * «Lo que busco» over the list, «Marcar» and «Quitar». Never «deseo»: it is a list to take to a
 * fair, in the first person.
 */
object WishLabels {
    /**
     * The name of the annex room holding the marked casillas (ADR 0030 §8), printed by its door,
     * its masthead and its heading from one string, as `PrunedVocabularyTest` holds for
     * «Avisos y licencias». The annex itself is «Explorar» (`ShowcaseLabels.DESTINATION`).
     */
    const val DESTINATION: String = "Lo que busco"

    /** What the list is for, said once under its heading: it is a tool for a fair. */
    const val SENTENCE: String = "Las casillas que marcaste, para llevártelas a la feria."

    /**
     * The empty list, reachable only through «Quitar» on the last row, since the door isn't shown
     * at zero. It says where marks are made rather than just «no hay nada».
     */
    const val EMPTY_EXPLANATION: String =
        "No queda ninguna casilla marcada. Se marcan en la lámina de cada colección."

    /**
     * The door into the marking mode, on a plate's header (ADR 0029 §5). A mode rather than a
     * toggle per casilla, which would repeat the cost on every hole (ADR 0026 §5); the same shape
     * as «Hacer una colección» in Monedas.
     */
    const val MARK_ACTION: String = "Marcar lo que busco"

    /** The way out of the mode. The same word the box dialog uses when the typing is over. */
    const val MARK_DONE_ACTION: String = "Hecho"

    /**
     * What the mode says while open: what to touch and what it costs (#282, ADR 0029 §5). The cost
     * is the ceiling: one call a month if the curated file names the issue, two otherwise, and the
     * spend must never be understated.
     */
    const val MARK_HINT: String =
        "Toca las casillas vacías que buscas. Cada una son +2 consultas al mes."

    /**
     * The mark itself, in the hole and on paper: the same lower-case note on screen and in the
     * notebook, like the price beside it (#493).
     */
    const val MARK_WORD: String = "lo busco"

    /** Undoing one mark, from the list. The plate undoes it by touching the casilla again. */
    const val REMOVE_ACTION: String = "Quitar"
}

/**
 * «Lo que busco» with its count, on the two doors that open it (ADR 0026 §8, ADR 0030 §8, #520):
 * the row inside «Explorar» and the row at the head of the index. The door is absent at zero, so
 * the count is never null. The arrow is drawn, not typed: neither font has the glyph (#298).
 */
fun wishDoorLabel(count: Int): String = "${WishLabels.DESTINATION} · $count"

/**
 * The marks the row has no room to draw: «y 5 más» (#520). The row draws the first few as coins at
 * a recognisable size rather than shrinking them all. Null at zero. Lower case and unitless, since
 * it continues the strip of coins.
 */
fun wishDoorMoreLabel(rest: Int): String? = "y $rest más".takeIf { rest > 0 }

/**
 * Shown under the index's «Lo que busco» row while the search box has text (#515, #520). Its count
 * covers the whole collection, since marks are casillas, not cards, so without this it would look
 * stale as the index narrows.
 *
 * Only on this row, not the shelf's, so it isn't said twice (ADR 0026 §5). Not for filters, which
 * persist across launches (ADR 0021 §1) and would show it every session. «Lo que escribes», not
 * «tu búsqueda», beside a row called «Lo que busco».
 */
fun wishDoorNote(searching: Boolean): String? =
    "Lo que escribes arriba no llega hasta aquí.".takeIf { searching }

/**
 * What the list holds, under its heading: «7 casillas en 5 láminas». Casillas first, as what was
 * marked; láminas because the list crosses plates.
 */
fun wishCensusLabel(slots: Int, plates: Int): String =
    "${plural(slots, "casilla", "casillas")} en ${plural(plates, "lámina", "láminas")}"

/**
 * What the marks add to the monthly pass, on «Este teléfono» (ADR 0029 §5): the ceiling of a cold
 * month (`wishCallsPerMonth`). Named, since nothing else on that card is about the marks, and in
 * the unit of [WishLabels.MARK_HINT]'s «+2 consultas al mes». Null with nothing marked. Printed
 * only here, not in the annex (see [WishSubject]).
 */
fun wishBudgetLabel(callsPerMonth: Int): String? =
    "${WishLabels.DESTINATION} · +$callsPerMonth consultas al mes".takeIf { callsPerMonth > 0 }
