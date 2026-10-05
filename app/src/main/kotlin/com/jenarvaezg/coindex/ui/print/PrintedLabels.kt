package com.jenarvaezg.coindex.ui.print

import com.jenarvaezg.coindex.ui.dayMonthYearLabel
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Words the PDF carries. The export's own UI copy (button, sheet, progress, closing message) lives
 * in `NotebookLabels` and never reaches paper.
 */

/**
 * The diameter as a number, for pages that don't print coins at their real size (#231, #233), so
 * the size can still be checked. A scaled page prints it on its own caption line, a list page at
 * the end of the row.
 *
 * One decimal with a comma, as Numista records it; a whole number drops the decimal, since
 * «40,0 mm» would claim a precision the ficha doesn't. Null when no size is recorded, rather than
 * «0 mm».
 */
fun printedDiameterLabel(millimetres: Float?): String? {
    val tenths = millimetres?.takeIf { it > 0f }?.times(10f)?.roundToInt() ?: return null
    val tenth = tenths % 10
    return if (tenth == 0) "${tenths / 10} mm" else "${tenths / 10},$tenth mm"
}

/**
 * The two mastheads, saying which hierarchy a page came from (ADR 0021 §1): «catálogo curado» is a
 * list the collector is filling, «colección» their own pieces. Shared by the PDF and the PNG, which
 * is a printed page since #431.
 */
const val PLATE_SECTION_EYEBROW: String = "COINDEX · CATÁLOGO CURADO"
const val PIECES_SECTION_EYEBROW: String = "COINDEX · COLECCIÓN"

/**
 * The source of pages of owned pieces (a collection with no issue list, the unclaimed coins), read
 * off the inventory. A plate names its catalog; the wish list says [WISH_SECTION_SOURCE].
 */
const val INVENTORY_SECTION_SOURCE: String = "tu colección en Numista"

/**
 * Labels of the rows of a printed specification. «País» and «Piezas» head a page of owned pieces;
 * the «Piezas» sentence is the subject's own `countSentence` (#226). «Valor» rather than the
 * screen's «Valor actual» (#493): the row is already titled, so `plateAmountLabel` arrives unnamed.
 */
const val COUNTRY_FACT_LABEL: String = "País"
const val PIECES_FACT_LABEL: String = "Piezas"
const val VALUE_FACT_LABEL: String = "Valor"

/**
 * The row under «Valor» dating when its catalog prices were fetched (#594): a printed page is read
 * long after, with no refresh (ADR 0028 §5, amended by #561). «Tasación» rather than «Fecha», which
 * would be confused with the catalog's date and the coin's year on the same page.
 */
const val VALUATION_FACT_LABEL: String = "Tasación"

/**
 * That date as an absolute day. Never a relative age like `priceAgeLabel`'s «hace 12 días»: a
 * printed page is read at any later time.
 */
fun printedValuationLabel(
    readAtMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = dayMonthYearLabel(Instant.ofEpochMilli(readAtMillis).atZone(zone).toLocalDate())

/** The page of the coins in no collection; its eyebrow must not claim «COLECCIÓN». */
const val UNCLAIMED_SECTION_EYEBROW: String = "COINDEX · SIN COLECCIÓN"
const val UNCLAIMED_SECTION_TITLE: String = "Sin colección"

/**
 * The page of what the collector is looking for (ADR 0029 §7). Its own eyebrow: its casillas come
 * from several catalogs, and a page of coins nobody owns must not say «COLECCIÓN». The title is the
 * destination's own string (`WishLabels.DESTINATION`).
 */
const val WISH_SECTION_EYEBROW: String = "COINDEX · LO QUE BUSCO"

/** The census row's label on the wish list: it counts casillas, not pieces. */
const val WISH_SECTION_COUNT_LABEL: String = "Casillas"

/** The wish list's source: the curated catalogs in the APK, since none of these coins is owned. */
const val WISH_SECTION_SOURCE: String = "los catálogos curados de Coindex"

/**
 * Which folio of a section this is. Callers print it only for sections spanning several, where a
 * plate cut in two could pass for two plates.
 */
fun printedPageOfSection(number: Int, pagesInSection: Int): String =
    "PÁGINA $number DE $pagesInSection"

/**
 * The legend under the ruler. It states the scale too, so a page printed at the wrong scale shows
 * it against the bar.
 */
fun printedRulerLabel(millimetres: Int): String = "$millimetres MM · ESCALA 1:1"

/**
 * The foot line naming where this folio's plates came from, so the paper can be checked later.
 * Since #232 a folio can hold plates from several catalogs; each is named once, in print order, and
 * the deduplication lives here rather than in [PrintPage.sources] so every caller gets it.
 */
fun notebookSourceLabel(sources: List<String>): String {
    val named = sources.distinct()
    return when (named.size) {
        0 -> ""
        1 -> "Fuente: ${named.single()}"
        else -> "Fuentes: ${named.joinToString(" · ")}"
    }
}
