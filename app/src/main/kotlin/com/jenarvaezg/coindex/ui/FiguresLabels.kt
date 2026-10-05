package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.prices.ValuationRefusal
import com.jenarvaezg.coindex.data.prices.ValuationStatus
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.LadderUnit
import com.jenarvaezg.coindex.domain.MarginFigure
import com.jenarvaezg.coindex.domain.PaidComparison
import com.jenarvaezg.coindex.domain.Referent
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.ValueSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Every string «Las cifras» prints (ADR 0026 §6). Almost none label a control: the sentences are
 * the statements of the figures themselves, such as «una al lado de otra llegan a».
 */
object FiguresLabels {
    /** The destination's own name, which the bottom bar and the heading both print. */
    const val DESTINATION: String = "Las cifras"

    const val SENTENCE: String = "Lo que pesa tu colección, y lo que vale."

    const val MONEY_HEADING: String = "El valor"

    /**
     * What «Las cifras» says in place of the money until the market lands (#519). It starts at the
     * verb because [MONEY_HEADING] is right above it. One sentence for every cause: only
     * «Este teléfono» tells them apart, since ADR 0026 §5 keeps its explanations off notebook
     * screens.
     */
    const val MONEY_WAITING: String = "Llega cuando llegue el mercado."

    /**
     * [MONEY_WAITING] in a plate's header, where there is no heading above it (#519). «El dinero»
     * covers both header figures without promising a «Coste de cerrar» to a plate that won't have
     * one (ADR 0028 §1).
     */
    const val PLATE_MONEY_WAITING: String = "El dinero llega cuando llegue el mercado."

    /**
     * Where the amount comes from, said under it. «lo que vale su plata», not «su plata»: the
     * silver is weight times fineness times the spot, not a quoted price, hence the stamp under it
     * (#398).
     */
    const val MONEY_ORIGIN: String =
        "El mayor de tres precios en cada moneda: el catálogo de Numista, lo que pagaste o lo que " +
            "vale su plata."

    /** The same criterion, short enough for the plate's one-line total (#408). */
    const val MONEY_CRITERION: String = "al mayor de tres precios"

    /**
     * The two figures of a plate's header, in full words (#493). Their amounts can be of the same
     * order, or the cost the larger, so only the words tell them apart. «Coste de cerrar» names a
     * purchase, not a lack (ADR 0026 §10). «Valor actual» is often alone, where a short form such
     * as «dentro» would not read.
     */
    const val PLATE_VALUE_LABEL: String = "Valor actual"
    const val PLATE_COST_LABEL: String = "Coste de cerrar"

    /**
     * The money figure of a plate that isn't the collector's (ADR 0030 §6). «Entrar», not «cerrar»:
     * it buys the first of something not collected yet, and «cerrar» would read as 0/12 of a
     * collection never started (ADR 0026 §10). Kept with the other two header labels on purpose.
     */
    const val SHOWCASE_ENTRY_LABEL: String = "Coste de entrar"

    /**
     * Where the cost of closing comes from, which differs from the value: a hole has no «lo que
     * pagaste», and its catalogue price is asked in `unc` (ADR 0028 §8), the grade
     * [uncirculatedSentence] also calls «sin circular». It names only the catalogue half's grade;
     * the other price is the coin's silver, which has none.
     */
    const val HOLE_CRITERION: String = "en sin circular"

    /**
     * The label of the catalogue prices' date under an amount, beside the silver's (#594).
     *
     * «Numista», not «catálogo»: a plate's spec already has a «Catálogo» row with the curated
     * file's date (`catalogDateLabel`, #518), and one word for two clocks is what #518 undid. It
     * names the source rather than a gesture, unlike `valuedAgeLabel`'s «tasada», because the pass
     * fetched these prices (ADR 0028 §3); the spot comes from elsewhere (ADR 0028 §9).
     */
    const val NUMISTA_STAMP_LABEL: String = "Numista"

    const val MATTER_HEADING: String = "La materia"
    const val METAL_HEADING: String = "El metal, por masa"
    const val PORTRAIT_HEADING: String = "El retrato"
    const val ARC_HEADING: String = "El arco"
    const val SIZE_HEADING: String = "El tamaño, a la misma escala"
    const val MARGIN_HEADING: String = "Al margen"

    /** What each ladder measures, said before its figure. */
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

    /** Where a piece's own value came from, in the ficha. */
    fun valueOrigin(source: ValueSource, grade: String?): String = when (source) {
        ValueSource.Market -> "precio de catálogo en ${grade.orEmpty()}"
        ValueSource.NeighbouringGrade -> "precio de catálogo en ${grade.orEmpty()}, el grado vecino"
        ValueSource.Silver -> "su plata"
        ValueSource.Paid -> "lo que pagaste"
    }
}

/**
 * The count of the bottom bar's third cell: grams, never money (#316). An amount in a permanent
 * bar changes on its own and shows the collector's estate to anyone glancing at the phone.
 * `null` while the snapshot is unread prints «—», not «0,00 kg» (#418).
 */
fun figuresCellCount(grams: Double?): String =
    if (grams == null) UNKNOWN_COUNT else kilogramsLabel(grams)

/**
 * Numista turning the calls away, worded once for the two surfaces that say it (#560).
 *
 * Other refusals are worded as a state on «Este teléfono» and as an answer in a snackbar, but any
 * second wording of this one would claim a cause: it is not the monthly allowance (ADR 0003), and
 * waiting may not fix it, since a `403` can be the key itself. One owner (ADR 0026 §5), held equal
 * by `PrunedVocabularyTest`. It doesn't say what to do: it appears unprompted.
 */
const val NUMISTA_IS_REFUSING: String = "Numista está rechazando las consultas."

/**
 * The valuation pass's one sentence, on «Este teléfono» (ADR 0028 §6). Like the photographs', it
 * tells prices still arriving apart from prices held back.
 *
 * Once settled, `held` is ignored: a refusal is only news while something is missing (#421).
 */
fun valuationLabel(status: ValuationStatus): String {
    if (status.wanted == 0) {
        return "Todavía no hay emisiones que tasar en este teléfono."
    }
    if (status.settled) {
        return "Los precios de las ${status.wanted} emisiones están al día."
    }
    val head = "Faltan los precios de ${status.missing} de ${status.wanted} emisiones. "
    return head + when (status.held) {
        null -> "Se traen solos con la app abierta."
        ValuationRefusal.Syncing -> "Esperan a que termine el sincronizado."
        // Not «el presupuesto de este mes» (#605): the pass stops short of the cap to leave the
        // inventory its calls, so the month's budget may not be spent.
        ValuationRefusal.BudgetExhausted ->
            "Se acabaron las consultas que los precios tienen este mes: seguirán el mes que viene."
        ValuationRefusal.Offline -> "Esperan a que haya red."
        ValuationRefusal.NoApiKey -> "Faltan las credenciales de Numista."
        ValuationRefusal.Rejected -> NUMISTA_IS_REFUSING
    }
}

private val SPANISH = Locale.forLanguageTag("es-ES")

/** Locale pinned like [megabytesLabel]: «6.91 kg» in a Spanish sentence reads as a typo. */
private fun decimal(value: Double, decimals: Int): String =
    String.format(SPANISH, "%,.${decimals}f", value)

fun kilogramsLabel(grams: Double): String = "${decimal(grams / 1_000.0, 2)} kg"

fun fineOuncesLabel(ounces: Double): String = "${decimal(ounces, 1)} oz finas"

/**
 * The fine silver, in the metal block right under the bar that splits the mass (#398). Phrased as
 * a conversion («que son …») because it is the bar's silver again in the unit bullion is bought in,
 * not a second figure.
 */
fun fineSilverSentence(ounces: Double): String = "que son ${fineOuncesLabel(ounces)} de plata pura"

/** The collection's census: pieces and issuers. The weight is the ladders'. */
fun matterCensusLabel(pieces: Int, issuers: Int): String = "$pieces piezas de $issuers emisores"

/**
 * A magnitude on a ladder, in that ladder's own unit.
 *
 * @param approximate only for the stack: `thickness` is missing in many types, so it is measured
 *   over the pieces that have one and scaled up, and says «unos» (`docs/ux/cifras-316.md`).
 */
fun ladderAmountLabel(unit: LadderUnit, amount: Double, approximate: Boolean): String {
    val decimals = if (unit == LadderUnit.Centimetres) 0 else 2
    val number = "${decimal(amount, decimals)} ${unit.suffix}"
    return if (approximate) "unos $number" else number
}

/**
 * What has just been passed and what is within reach: «más que un gato y a 310 g de una bola de
 * bolos». The ladder is fixed, so the next rung gets closer as coins arrive (#304). Past the last
 * rung, the list of referents needs to grow.
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

/** The gap to the next rung, in whole small units under one: «310 g», not «0,31 kg». */
private fun gapLabel(unit: LadderUnit, gap: Double): String = when (unit) {
    LadderUnit.Kilograms ->
        if (gap < 1.0) "${Math.round(gap * 1_000)} g" else "${decimal(gap, 2)} kg"
    LadderUnit.Metres ->
        if (gap < 1.0) "${Math.round(gap * 100)} cm" else "${decimal(gap, 2)} m"
    LadderUnit.Centimetres -> "${Math.round(gap)} cm"
}

/**
 * The referent with its indefinite article. Gender is listed in [FEMININE] because the ending
 * doesn't tell it: «la bici», «el taburete».
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
 * How many pieces are valued, as a coverage and never a progress (ADR 0028 §7). Null when all
 * are: the total says it alone.
 */
fun coverageLabel(valued: Int, pieces: Int): String? =
    if (valued >= pieces) null else "el valor de $valued de tus $pieces piezas"

/**
 * What was paid against what the same pieces are worth today, with its denominator in front
 * (#491): «de las N piezas cuyo precio anotaste». Never a share of the collection: the pieces with
 * no price mix gifts with purchases nobody noted, so only what was declared counts.
 */
fun paidAgainstTodayLabel(comparison: PaidComparison): String {
    val declared = if (comparison.pieces == 1) {
        "De la única pieza cuyo precio anotaste"
    } else {
        "De las ${comparison.pieces} piezas cuyo precio anotaste"
    }
    val worth = if (comparison.pieces == 1) "Hoy vale" else "Hoy valen"
    return "$declared, pagaste ${eurosLabel(comparison.paid)}. " +
        "$worth ${eurosLabel(comparison.today)}."
}

/**
 * The stamp under the amount: the silver price behind the metal floor and when it was read, so the
 * total doesn't read as a quotation (ADR 0028 §5, #398). The hour shows only for today and
 * yesterday (#326), since the spot expires daily.
 */
fun spotStampLabel(
    spot: SilverSpot,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "plata: ${eurosPerOunceLabel(spot.eurPerTroyOunce)} · ${readAtLabel(spot.readAtMillis, nowMillis, zone)}"

fun eurosPerOunceLabel(eurPerTroyOunce: Double): String = "${decimal(eurPerTroyOunce, 2)} €/oz"

/**
 * When the spot was read, in calendar days rather than 24-hour blocks, so a spot read at 23:00
 * yesterday doesn't say «hoy 23:00» this morning (#398).
 */
private fun readAtLabel(readAtMillis: Long, nowMillis: Long, zone: ZoneId): String {
    val read = Instant.ofEpochMilli(readAtMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val days = ChronoUnit.DAYS.between(read.toLocalDate(), now.toLocalDate())
    return when {
        days <= 0L -> "hoy ${read.format(CLOCK)}"
        days == 1L -> "ayer ${read.format(CLOCK)}"
        else -> "hace $days días"
    }
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", SPANISH)

/**
 * How old a price from Numista is, in the coarsest unit that is still true. Every price age in the
 * app goes through here (`valuedAgeLabel`, [numistaStampLabel], #594). Calendar days, like the
 * spot's stamp (#398).
 *
 * Past a month it gives the date instead: catalog prices live ninety days (ADR 0028 §5, amended by
 * #561), and «hace 83 días» can't be placed on a calendar.
 */
fun priceAgeLabel(
    readAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val read = Instant.ofEpochMilli(readAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    // A clock that has gone backwards is not a price from the future: it is today's.
    val days = ChronoUnit.DAYS.between(read, today).coerceAtLeast(0)
    return when {
        days == 0L -> "hoy"
        days == 1L -> "ayer"
        days < 30L -> "hace ${plural(days.toInt(), "día", "días")}"
        else -> "el ${dayMonthYearLabel(read)}"
    }
}

/**
 * When the catalogue half of an amount was fetched (#594), shaped «fuente: cuándo» like the
 * silver's clause so each names what it dates. [readAtMillis] is the oldest read in the amount
 * (#494): a date over a total is a promise about all of it.
 */
fun numistaStampLabel(
    readAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "${FiguresLabels.NUMISTA_STAMP_LABEL}: ${priceAgeLabel(readAtMillis, nowMillis, zone)}"

/**
 * The whole stamp under the total of «Las cifras»: one clause per clock (#594). The spot is read
 * daily outside the budget (ADR 0028 §9) while catalog prices live ninety days (#561), so a single
 * date would misstate one of them (ADR 0028 §5). #494's oldest-read rule applies within the
 * catalogue clause.
 *
 * @param catalogReadAt null when no catalog price feeds the total (only metal and what was paid);
 *   the clause is then left out rather than written as «sin fecha».
 */
fun moneyStampLabel(
    spot: SilverSpot,
    catalogReadAt: Long?,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = listOfNotNull(
    spotStampLabel(spot, nowMillis, zone),
    catalogReadAt?.let { numistaStampLabel(it, nowMillis, zone) },
).joinToString(" · ")

/** The arc, which is two years and the distance between them. */
fun arcLabel(years: Int): String = "$years años"

/**
 * The four shares a country's portrait leads with. The value share is money, so its clause is left
 * out whenever the money section is (ADR 0028 §4). Takes the portrait rather than four `Double`s
 * that could be swapped unnoticed.
 */
fun portraitSharesLabel(portrait: CountryPortrait): String = buildString {
    append("${percentLabel(portrait.pieceShare)} de tus piezas")
    append(" · ${percentLabel(portrait.massShare)} del peso")
    append(" · ${percentLabel(portrait.silverShare)} de la plata")
    portrait.valueShare?.let { append(" · ${percentLabel(it)} del valor") }
}

/**
 * A diameter under a coin drawn to scale, in whole millimetres: on screen the drawing is the
 * measure. Paper keeps the tenth (`printedDiameterLabel`).
 */
fun screenDiameterLabel(millimetres: Double): String = "${millimetres.toInt()} mm"

/**
 * The sentences «al margen», drawn from the fichas already on the phone: the page's colophon. Here
 * rather than in `FiguresScreen.kt` because this file holds every string «Las cifras» prints
 * (ADR 0026 §6).
 */
fun demonetizedSentence(figure: MarginFigure): String =
    "${percentLabel(figure.shareOfPieces())} ya no son dinero en ninguna parte"

fun sameHandSentence(figure: MarginFigure): String =
    "${figure.pieces} las grabó la misma mano: ${figure.subject.orEmpty()}"

fun mintSentence(figure: MarginFigure, distinctMints: Int): String =
    "${figure.pieces} salieron de ${figure.subject.orEmpty()}, de $distinctMints cecas distintas"

fun commonestYearSentence(figure: MarginFigure): String =
    "${figure.pieces} llevan la fecha de ${figure.subject.orEmpty()}"

/**
 * The fifth margin sentence: how the collection is kept, from the grades the collector typed. A
 * sentence, not a histogram (`spec.md §0.4`). «o casi» stands for `au`, about uncirculated. A
 * share of pieces like the rest of the page, not of rows, which can differ widely.
 */
fun uncirculatedSentence(figure: MarginFigure): String =
    "${percentLabel(figure.shareOfPieces())} están sin circular o casi"

/**
 * A share of every piece, not of the types Numista answered for: a moving denominator can't be
 * checked.
 */
private fun MarginFigure.shareOfPieces(): Double =
    if (outOf <= 0) 0.0 else pieces.toDouble() / outOf

/**
 * A coin's value with its origin, which makes it checkable (#316). The origin is dropped only when
 * the type's pieces disagree on one.
 */
fun coinValueLabel(value: CoinValue): String {
    val head = eurosLabel(value.eur)
    val origin = value.source?.let { FiguresLabels.valueOrigin(it, value.grade) }
    val pieces = "${value.pieces} piezas".takeIf { value.pieces > 1 }
    return listOfNotNull(head, pieces, origin).joinToString(" · ")
}

/**
 * A plate's value with its criterion (#408). A total per plate is allowed where one over the shelf
 * isn't (ADR 0026 §10). Unnamed because the printed page labels its row «Valor»; on screen,
 * [plateValueLabel] adds the name.
 */
fun plateAmountLabel(value: PlateValue): String =
    "${eurosLabel(value.eur)} · ${FiguresLabels.MONEY_CRITERION}"

/** The first line of a plate's header: the value, with its name (#493). */
fun plateValueLabel(
    value: PlateValue,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = listOfNotNull(
    "${FiguresLabels.PLATE_VALUE_LABEL}: ${plateAmountLabel(value)}",
    // Its own date: a marked casilla is repriced whatever the plate's shape (ADR 0029 §4), so the
    // cost line can be much fresher than this one (#594).
    value.catalogReadAt?.let { numistaStampLabel(it, nowMillis, zone) },
).joinToString(" · ")

/**
 * The second line: what closing the plate costs, with its own criterion, since holes are priced
 * by a different rule (ADR 0028 §8, #493). A closed plate has no cost line rather than a zero.
 */
fun plateCostLabel(
    cost: PlateCost,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = listOfNotNull(
    "${FiguresLabels.PLATE_COST_LABEL}: ${eurosLabel(cost.eur)}",
    FiguresLabels.HOLE_CRITERION,
    cost.catalogReadAt?.let { numistaStampLabel(it, nowMillis, zone) },
).joinToString(" · ")

/**
 * The price inside one empty casilla: the amount alone (#493). Its criterion and date are on the
 * header's «Coste de cerrar», which totals these holes (ADR 0026 §5, #594).
 *
 * Exception: a marked casilla on a plate past the threshold of ADR 0028 §1 has no cost line above
 * it (ADR 0029 §4), so its amount, like its row in «Lo que busco», stays undated.
 */
fun holeCostLabel(eur: Double): String = eurosLabel(eur)

