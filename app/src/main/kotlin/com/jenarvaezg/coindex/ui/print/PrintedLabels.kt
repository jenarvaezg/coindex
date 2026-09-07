package com.jenarvaezg.coindex.ui.print

import kotlin.math.roundToInt

/**
 * What a printed cell says, as opposed to what the app says *about* printing.
 *
 * `NotebookLabels` is the copy of the export — the button, the sheet, the progress line, the message
 * that closes it — and none of it reaches the paper. This is the other half: words the PDF carries,
 * which outlive the app and are read with a coin in the other hand.
 */

/**
 * The diameter of a coin the page is not printing at its real size, as a number (#231, #233).
 *
 * A notebook of lines has no ruler and nothing at 1:1 to hold one against, so the measure that was
 * the whole point of #169 has to be **said** instead of shown — the collector at a fair still wants
 * to know whether the piece in the tray is the 38,6 mm one or the 40,9. A scaled album page is the
 * other one that has to say it (#233), and it is the same sentence: what a ruler cannot be laid
 * against is measured in words or it is not measured at all.
 *
 * On a page of coins it gets a **line of its own** at the foot of the caption, and it was printed the
 * other way first: appended to the year it shared a line with «1977 · Numista 681», and an ellipsis
 * eating the millimetres —«1977 · Numista 681 · 4…»— is a page that dropped the one thing it promised
 * in place of the ruler. On a page of lines it closes the row instead, where the width runs left to
 * right and there is nothing to stack it under.
 *
 * One decimal and a comma, because that is how Numista records it and how the collector says it. A
 * whole number of millimetres drops the decimal rather than printing «40,0 mm»: the zero would read
 * as a precision the ficha never claimed. Null where nobody recorded a size — an announced member, an
 * unlisted one — because that is a blank and not a «0 mm».
 */
fun printedDiameterLabel(millimetres: Float?): String? {
    val tenths = millimetres?.takeIf { it > 0f }?.times(10f)?.roundToInt() ?: return null
    val tenth = tenths % 10
    return if (tenth == 0) "${tenths / 10} mm" else "${tenths / 10},$tenth mm"
}

/**
 * The two mastheads of the notebook, which say which of the two hierarchies a page came out of.
 *
 * They were the mastheads of the shared PNG first, and they are the same words for the same reason: a
 * page arrives in somebody's hands with no app around it, so «catálogo curado» is somebody else's list
 * the collector is filling and «colección» is the collector's own pieces (ADR 0021 §1). Since #431 the
 * PNG **is** a printed page, so the two strings have one home and this is it.
 *
 * A page that said «curado» about a list nobody curated could not be taken back: paper outlives the
 * app, which is why the claim is made in the copy file and not at the printer.
 */
const val PLATE_SECTION_EYEBROW: String = "COINDEX · CATÁLOGO CURADO"
const val PIECES_SECTION_EYEBROW: String = "COINDEX · COLECCIÓN"

/**
 * Where the coins of a page of owned pieces are named from, which is nobody's catalog.
 *
 * Said by the two pages that print what is in the house — a collection with no issue list, and the
 * coins no collection claims — because both are read off the same inventory. The hunt says
 * [WISH_SECTION_SOURCE] instead, and a plate names the catalog it was curated from.
 */
const val INVENTORY_SECTION_SOURCE: String = "tu colección en Numista"

/**
 * The rows of a printed specification, in the label each one carries.
 *
 * «País» and «Piezas» are the heading of a page of owned pieces — where the coins are from, when they
 * agree on it, and how many there are. Only the labels are here: the sentence in the second row is the
 * subject's own `countSentence`, which is what keeps the folio counting what the screen counts (#226).
 *
 * «Valor» is **not** «Valor actual» (#493). The screen says the name of the figure in full because it
 * has neither row nor column to be read against; on paper the row is already titled, so an amount
 * carrying its own title would say the word twice — which is why `plateAmountLabel` arrives here
 * unnamed. The census row of the hunt is [WISH_SECTION_COUNT_LABEL], which counts casillas and not
 * pieces.
 */
const val COUNTRY_FACT_LABEL: String = "País"
const val PIECES_FACT_LABEL: String = "Piezas"
const val VALUE_FACT_LABEL: String = "Valor"

/**
 * The page of the coins that are in no collection.
 *
 * The eyebrow is not «COLECCIÓN», which is what a page that *is* one says: this is the page of the
 * ones that are in none, and the header is where that is said once instead of cell by cell.
 */
const val UNCLAIMED_SECTION_EYEBROW: String = "COINDEX · SIN COLECCIÓN"
const val UNCLAIMED_SECTION_TITLE: String = "Sin colección"

/**
 * The page of what the collector is looking for (ADR 0029 §7).
 *
 * Its own eyebrow because it is neither of the two the notebook had: it is not a curated catalog — the
 * casillas on it come from as many as they come from — and it is emphatically not a collection, which
 * is the one thing a page of coins nobody owns must not claim. The paper outlives the app, and a sheet
 * that said «COLECCIÓN» over seven coins in a dealer's tray would be a false claim in somebody else's
 * hands.
 *
 * The title is the destination's own string, so the screen, the door and the paper cannot drift.
 */
const val WISH_SECTION_EYEBROW: String = "COINDEX · LO QUE BUSCO"

/** What the census row is called on paper, in the label of its own row. */
const val WISH_SECTION_COUNT_LABEL: String = "Casillas"

/**
 * Where the coins of this list are named from, which is not anybody's inventory.
 *
 * «tu colección en Numista» is what every other page says and it would be a lie here — none of these
 * coins is in it. What names them is the curated shelf that travels in the APK.
 */
const val WISH_SECTION_SOURCE: String = "los catálogos curados de Coindex"

/**
 * Which sheet of a section this folio is, printed only where there is a break to explain.
 *
 * «PÁGINA 1 DE 1» is noise on paper, so the caller asks for this only when the section spans more
 * than one — the sentence exists because a plate cut across two folios is otherwise indistinguishable
 * from two plates.
 */
fun printedPageOfSection(number: Int, pagesInSection: Int): String =
    "PÁGINA $number DE $pagesInSection"

/**
 * The legend under the ruler, which is the promise the ruler makes.
 *
 * It says the scale as well as the length, because a bar of millimetres on a page that turned out
 * not to be 1:1 is worse than no bar: the collector would measure a coin against it.
 */
fun printedRulerLabel(millimetres: Int): String = "$millimetres MM · ESCALA 1:1"

/**
 * Where what is printed on this folio came from, which is a line the paper has to carry.
 *
 * The strip at the foot is **once per folio** and the heading is once per plate, so since #232 the
 * source can be a plural: two plates sharing a page can come from two different catalogs, and a page
 * that names only the first one would attribute the second to it.
 *
 * **Named once each, and that is this function's job and not its caller's**: a folio of five plates
 * of one catalog says it once, in the order they are printed. Deduplicating here rather than in
 * [PrintPage.sources] is what makes «Fuentes» honest wherever the line is assembled — a caller that
 * handed over the same catalog twice would otherwise print it twice under a word promising it had
 * not.
 *
 * It outlives the app, which is why it is on the paper at all: a list that does not say where it came
 * from cannot be checked later either.
 */
fun notebookSourceLabel(sources: List<String>): String {
    val named = sources.distinct()
    return when (named.size) {
        0 -> ""
        1 -> "Fuente: ${named.single()}"
        else -> "Fuentes: ${named.joinToString(" · ")}"
    }
}
