package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.LadderUnit
import com.jenarvaezg.coindex.domain.Referent
import com.jenarvaezg.coindex.domain.ValueSource
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Every string «Las cifras» prints, in one place (ADR 0026 §6).
 *
 * The page is the densest thing in the app by number of figures and the thinnest by furniture: almost
 * nothing here is a label that explains a control, and the few sentences that exist are **the
 * statements of the figures themselves**. «una al lado de otra llegan a» is not decoration — without
 * it the two lower ladders are two lines with animals on them.
 */
object FiguresLabels {
    /** The destination's own name, which the bottom bar and the heading both print. */
    const val DESTINATION: String = "Las cifras"

    const val SENTENCE: String = "Lo que pesa tu colección, y lo que vale."

    const val MONEY_HEADING: String = "El valor"

    /** Where the amount comes from, said under it: a number nobody can check is not a figure. */
    const val MONEY_ORIGIN: String =
        "El mayor de tres precios en cada moneda: el catálogo de Numista, lo que pagaste o su plata."

    const val MATTER_HEADING: String = "La materia"
    const val METAL_HEADING: String = "El metal, por masa"
    const val PORTRAIT_HEADING: String = "El retrato"
    const val ARC_HEADING: String = "El arco"
    const val SIZE_HEADING: String = "El tamaño, a la misma escala"
    const val MARGIN_HEADING: String = "Al margen"

    /** What each ladder measures. Five words that are the figure's enunciation, not furniture. */
    fun ladderStatement(kind: LadderKind): String = when (kind) {
        LadderKind.Weight -> "todas juntas pesan"
        LadderKind.Row -> "una al lado de otra llegan a"
        LadderKind.Stack -> "una encima de otra levantan"
    }

    /** What a referent is called, under its drawing. */
    fun referent(referent: Referent): String = when (referent) {
        Referent.Brick -> "ladrillo"
        Referent.Cat -> "gato"
        Referent.BowlingBall -> "bola de bolos"
        Referent.Tyre -> "neumático"
        Referent.Labrador -> "labrador"
        Referent.Bicycle -> "bici"
        Referent.Car -> "coche"
        Referent.Bus -> "autobús"
        Referent.Lorry -> "camión"
        Referent.Whale -> "ballena"
        Referent.Stool -> "taburete"
        Referent.Shepherd -> "pastor"
        Referent.Countertop -> "encimera"
        Referent.Doorknob -> "pomo"
        Referent.Person -> "persona"
    }

    /** Where a piece's own value came from, in the ficha and in the plate's header. */
    fun valueOrigin(source: ValueSource, grade: String?): String = when (source) {
        ValueSource.Market -> "precio de catálogo en ${grade.orEmpty()}"
        ValueSource.NeighbouringGrade -> "precio de catálogo en ${grade.orEmpty()}, el grado vecino"
        ValueSource.Silver -> "su plata"
        ValueSource.Paid -> "lo que pagaste"
    }
}

/**
 * The count the bottom bar's third cell prints: grams, and **never money**.
 *
 * An amount in a permanent bar is a pocket ticker — it changes on its own, with nobody touching
 * anything, and puts the collector's estate in front of whoever glances at the phone. The weight only
 * changes when a coin arrives (#316).
 */
fun figuresCellCount(grams: Double): String = kilogramsLabel(grams)

/**
 * The one sentence of the valuation, in the settings screen.
 *
 * The pass is silent everywhere else — this line exists for the same reason the photographs' does:
 * «faltan y están cayendo» and «faltan porque se acabó el presupuesto» look identical from outside and
 * need different things from the collector (ADR 0028 §6).
 */
fun valuationLabel(status: ValuationStatus): String {
    if (status.wanted == 0) {
        return "Todavía no hay emisiones que tasar en este teléfono."
    }
    if (status.settled && status.held == null) {
        return "Los precios de las ${status.wanted} emisiones de tu colección están en este teléfono."
    }
    val head = "Faltan los precios de ${status.missing} de ${status.wanted} emisiones. "
    return head + when (status.held) {
        null -> "Se traen solos con la app abierta."
        ValuationRefusal.Syncing -> "Esperan a que termine el sincronizado."
        ValuationRefusal.BudgetExhausted ->
            "Se acabó el presupuesto de llamadas de este mes: seguirán el mes que viene."
        ValuationRefusal.Offline -> "Esperan a que haya red."
        ValuationRefusal.NoApiKey -> "Faltan las credenciales de Numista."
    }
}

private val SPANISH = Locale.forLanguageTag("es-ES")

/**
 * The locale is pinned rather than read off the device, like the photograph cache's megabytes: the
 * sentences around these numbers are written in Spanish, and «6.91 kg» in the middle of one reads as a
 * typo rather than as a setting.
 */
private fun decimal(value: Double, decimals: Int): String =
    String.format(SPANISH, "%,.${decimals}f", value)

fun kilogramsLabel(grams: Double): String = "${decimal(grams / 1_000.0, 2)} kg"

fun fineOuncesLabel(ounces: Double): String = "${decimal(ounces, 1)} oz finas"

/**
 * A magnitude on a ladder, in that ladder's own unit.
 *
 * @param approximate the stack, and only the stack: `thickness` is missing in a third of the types, so
 *   it is measured over the pieces that have one and scaled to all of them. «unos» is the whole of the
 *   declaration, and it is the one figure of the page that carries it (`docs/ux/cifras-316.md`).
 */
fun ladderAmountLabel(unit: LadderUnit, amount: Double, approximate: Boolean): String {
    val decimals = if (unit == LadderUnit.Centimetres) 0 else 2
    val number = "${decimal(amount, decimals)} ${unit.suffix}"
    return if (approximate) "unos $number" else number
}

/**
 * What the ladder is for, in one sentence: what has just been passed and what is within reach.
 *
 * «Más que un gato y a 310 g de una bola de bolos» is the figure — the comparison does not decorate it.
 * And it is what no lone number gives: the ladder is fixed, so as coins arrive the next rung pulls
 * forward. It is *revela, no reprocha* applied to matter (#304).
 *
 * Under the first rung there is nothing passed yet, and over the last there is nothing left to reach —
 * which is the day the list of referents has to grow, and it is a datum of the app, not of the collector.
 */
fun ladderComparison(reading: LadderReading): String {
    val unit = reading.ladder.unit
    val passed = reading.placement.justPassed
    val next = reading.placement.nextUp
    return when {
        passed == null && next != null ->
            "todavía por debajo de ${withArticle(next.referent)}"
        next == null && passed != null ->
            "por encima de ${withArticle(passed.referent)}, que era el último referente"
        passed != null && next != null -> {
            val gap = gapLabel(unit, next.amount - reading.amount)
            "más que ${withArticle(passed.referent)} y a $gap de ${withArticle(next.referent)}"
        }
        else -> ""
    }
}

/**
 * The gap to the next rung, in the smallest unit that keeps it a whole number.
 *
 * «a 310 g de una bola de bolos» and not «a 0,31 kg»: a distance rounded into two decimals of a kilo is
 * the sort of number a dashboard prints and nobody pictures.
 */
private fun gapLabel(unit: LadderUnit, gap: Double): String = when (unit) {
    LadderUnit.Kilograms ->
        if (gap < 1.0) "${Math.round(gap * 1_000)} g" else "${decimal(gap, 2)} kg"
    LadderUnit.Metres ->
        if (gap < 1.0) "${Math.round(gap * 100)} cm" else "${decimal(gap, 2)} m"
    LadderUnit.Centimetres -> "${Math.round(gap)} cm"
}

/**
 * The referent with the article Spanish needs to read as prose.
 *
 * Two of the fifteen are feminine — la bici, la ballena, la bola de bolos, la encimera, la persona — so
 * the article cannot be derived from the ending: `bici` and `taburete` would both take «el».
 */
private fun withArticle(referent: Referent): String {
    val name = FiguresLabels.referent(referent)
    return if (referent in FEMININE) "una $name" else "un $name"
}

private val FEMININE = setOf(
    Referent.BowlingBall,
    Referent.Bicycle,
    Referent.Whale,
    Referent.Countertop,
    Referent.Person,
)

fun squareMetresLabel(squareMetres: Double, sheets: Double): String =
    "${decimal(squareMetres, 2)} m² · ${decimal(sheets, 1)} folios A4"

/** A share as whole percent: the bar and the portrait both read in units nobody has to divide. */
fun percentLabel(share: Double): String = "${Math.round(share * 100).toInt()} %"

fun eurosLabel(amount: Double): String = "${decimal(amount, 0)} €"

/**
 * How many pieces of how many, which is a **coverage** and never a progress (ADR 0028 §7).
 *
 * «El valor de N de tus 574 piezas» is said; «llevo 140 de 223» is not. Complete, it says only the
 * total, because a coverage of everything is a sentence with nothing in it.
 */
fun coverageLabel(valued: Int, pieces: Int): String? =
    if (valued >= pieces) null else "el valor de $valued de tus $pieces piezas"

/**
 * When a figure brought from outside was last read.
 *
 * Every number from outside carries this, and it is what stops the total reading as a quotation
 * (ADR 0028 §5). An expired one still says it, because expired is not deleted.
 */
fun readAtLabel(readAtMillis: Long, nowMillis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays((nowMillis - readAtMillis).coerceAtLeast(0))
    return when {
        days <= 0L -> "plata de hoy"
        days == 1L -> "plata de ayer"
        else -> "plata de hace $days días"
    }
}

/** The arc, which is two years and the distance between them. */
fun arcLabel(years: Int): String = "$years años"

