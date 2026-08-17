package com.jenarvaezg.coindex.ui

/**
 * Every string «Lo que busco» prints, in one place (ADR 0026 §6, ADR 0029).
 *
 * The vocabulary of the whole feature is **four words and one verb**: «lo busco» on the casilla,
 * «Lo que busco» over the list, «Marcar» to open the gesture and «Quitar» to undo it. Nothing here
 * says «deseo» or «lista de deseos»: the collector is not filing a wish, he is going to a fair with a
 * list of what he is looking for, and the first person is what says whose list it is.
 */
object WishLabels {
    /**
     * The annex's own name, printed by the door, by the masthead and by the heading.
     *
     * Three surfaces and one string, which is the rule `PrunedVocabularyTest` already holds for
     * «Avisos y licencias»: a door that opened a screen called something else would read as two
     * features.
     *
     * The annex of ADR 0026 §8 is «Explorar» — `ShowcaseLabels.DESTINATION` — and this names the room
     * inside it that holds the marked casillas (ADR 0030 §8). For one version it was everything behind
     * that door and the screen was called this; since the shelf window arrived the door of the index
     * names both populations and this string names the list, its own door and its masthead.
     */
    const val DESTINATION: String = "Lo que busco"

    /** What the list is for, said once under its heading: it is a tool for a fair. */
    const val SENTENCE: String = "Las casillas que marcaste, para llevártelas a la feria."

    /**
     * What the screen says with nothing on it.
     *
     * It is reachable empty by exactly one route — «Quitar» on the last row — because the door is not
     * printed at zero. So it says where the marks are made instead of apologising: the gesture lives
     * on the lámina, and a screen that only said «no hay nada» would leave the collector on a dead
     * end.
     */
    const val EMPTY_EXPLANATION: String =
        "No queda ninguna casilla marcada. Se marcan en la lámina de cada colección."

    /**
     * The door into the marking mode, on a plate's own header (ADR 0029 §5).
     *
     * A mode and not a control per casilla: fifty-one holes with a toggle each would print the cost
     * line fifty-one times, which is the frequency ADR 0026 §5 prices. It is the same shape «Agrupar
     * piezas» already has in Monedas.
     */
    const val MARK_ACTION: String = "Marcar lo que busco"

    /** The way out of the mode. The same word the box dialog uses when the typing is over. */
    const val MARK_DONE_ACTION: String = "Hecho"

    /**
     * What the mode says while it is open: what to touch, and what it costs.
     *
     * **The spend is named in the gesture** (#282's rule, ADR 0029 §5), and it is named as the
     * ceiling: a casilla whose curated file already names its issue costs one call a month and any
     * other costs two. «+2» is the promise a collector can hold the app to; «1 o 2» is a hedge, and
     * rounding a spend down is the direction this sentence must never err in.
     */
    const val MARK_HINT: String =
        "Toca las casillas vacías que buscas. Cada una son +2 consultas al mes."

    /**
     * The mark itself, inside the hole and on the paper: **two words in lower case**.
     *
     * It is a note left in an empty pocket, like the price beside it (#493), and it is the same string
     * on the screen and in the notebook — a mark that said «Lo busco» on the phone and «Buscada» on
     * the page would be two marks. Lower case because that is how the note is written; it is the
     * plate's own state and not a heading.
     */
    const val MARK_WORD: String = "lo busco"

    /** Undoing one mark, from the list. The plate undoes it by touching the casilla again. */
    const val REMOVE_ACTION: String = "Quitar"
}

/**
 * «Lo que busco» with its count, on the door that opens it (ADR 0026 §8, ADR 0030 §8).
 *
 * **The count is not optional and the zero is not printed**: the door itself is absent while nothing
 * is marked, which is why this takes a live count and never a nullable one — the same clause the sewn
 * edge keeps while it reads (#418).
 *
 * **Two doors hang this label now** (ADR 0030 §8). Inside «Explorar» it is the whole of the row, in the
 * short form §8 wrote — the shelf is what the collector is already looking at, so what this door adds is
 * the list. At the foot of the index it is the first half of [annexDoorLabel], which names the twenty
 * beside it. The arrow is drawn and not typed — neither Bitter nor Barlow has that glyph (#298).
 */
fun wishDoorLabel(count: Int): String = "${WishLabels.DESTINATION} · $count"

/**
 * The door of the index, in the two forms ADR 0026 §8 clause 3 wrote for it.
 *
 * «Y otras 20 láminas que no coleccionas →» while nothing is marked, «Lo que busco · 7, y otras 20
 * láminas →» once something is, and **null** when there is neither — which is the zero that is not
 * printed, the same clause the sewn edge keeps while it reads (#418).
 *
 * The count is the **twenty and not the twenty-three** (ADR 0030 §8 clause 5): what is behind this door
 * that the list above it does not already hold is the shelf window, and counting the collector's own
 * plates here would claim they live somewhere else.
 *
 * With the window empty and marks in hand it falls back to the short form, which is what the annex was
 * called for one version: a collection that has reached every curated catalog has a list behind this
 * door and nothing else. Unreachable today, and it is one `takeIf` rather than a screen nobody can open.
 */
fun annexDoorLabel(wishes: Int, plates: Int): String? = when {
    wishes > 0 && plates > 0 -> "${wishDoorLabel(wishes)}, y ${otherPlates(plates)}"
    wishes > 0 -> wishDoorLabel(wishes)
    plates > 0 -> "Y ${otherPlates(plates)} que no coleccionas"
    else -> null
}

/**
 * What the door owes a collector who is typing in the box above it (#515).
 *
 * The count behind this door is measured over the whole collection and never over the narrowing —
 * which is right, because what is behind it is not in the list above: the shelf window is made of
 * plates the collector owns nothing of, and the marks are casillas rather than cards. But «Y otras
 * 55 láminas» under an index the search had cut to two read as a count that had simply not been
 * updated, which is the one thing a door that names what is behind it cannot afford.
 *
 * So it says so, and only while something is typed: **the filters are not named here**. They persist
 * across launches (ADR 0021 §1), so a collector who left the país chip on would read this line on
 * every screen of every session — which is the frequency ADR 0026 §5 prices — while the query is
 * what is being done right now, in a box in view, over a list that may have gone empty under it.
 *
 * «Lo que escribes» and not «tu búsqueda»: one door down, `WishLabels.DESTINATION` is «Lo que
 * busco», and a line about searching beside a room called that would be read as being about the room.
 */
fun annexDoorNote(searching: Boolean): String? =
    "Lo que escribes arriba no llega hasta aquí.".takeIf { searching }

/**
 * «otras 20 láminas», and «otra lámina» when there is one.
 *
 * The count goes with the plural and **not** with the singular: «otra 1 lámina» is what a `plural`
 * helper produces on its own, and it is the one place in this door where the number has to disappear
 * for the sentence to be Spanish.
 */
private fun otherPlates(plates: Int): String =
    if (plates == 1) "otra lámina" else "otras $plates láminas"

/**
 * What the list holds, under its heading: **«7 casillas en 5 láminas»**.
 *
 * Two units because the list crosses plates and that is the whole of what makes it a list rather than
 * a section of one lámina. The casillas come first, because they are what the collector marked.
 */
fun wishCensusLabel(slots: Int, plates: Int): String =
    "${plural(slots, "casilla", "casillas")} en ${plural(plates, "lámina", "láminas")}"

/**
 * What the marks cost every month, in Ajustes, **with its subject** (ADR 0029 §5).
 *
 * The first elastic spend of the app, and the reason it is printed at all: the monthly pass used to be
 * a fixed number nobody had to be told. It is the ceiling of a cold month — see `wishCallsPerMonth`.
 *
 * **Named, because on that card nothing else is about the marks.** ADR 0029 §5 asks for «lo que busco ·
 * N al mes» where the budget is already shown, and an amount on its own would sit under a sentence
 * about the pass: a number nobody can attribute. One word wider than the ADR's literal, and
 * deliberately — «· 2 al mes» leaves the reader to guess the unit, and what this counts is consultas.
 *
 * It says «+» and the unit in full because it is what the gesture promised: [WishLabels.MARK_HINT] says
 * «+2 consultas al mes» per casilla, and the total has to be read in the same unit or it sounds like
 * the whole of the pass rather than what the marks add to it.
 *
 * **Absent rather than zero** with nothing marked: then the pass is the fixed thing it always was, and
 * a «+0» would be a line about a decision nobody made. There is **one** reading of this figure and not
 * two: the annex deliberately does not print it — see [WishSubject].
 */
fun wishBudgetLabel(callsPerMonth: Int): String? =
    "${WishLabels.DESTINATION} · +$callsPerMonth consultas al mes".takeIf { callsPerMonth > 0 }
