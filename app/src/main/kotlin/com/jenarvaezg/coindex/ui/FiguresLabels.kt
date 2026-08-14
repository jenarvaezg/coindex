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

    /**
     * Where the amount comes from, said under it: a number nobody can check is not a figure.
     *
     * «lo que vale su plata» and not «su plata»: the elliptical form put a coin's metal on the same
     * grammatical footing as a price, and the third source is the only one of the three that is not
     * quoted anywhere — it is weight times fineness times the spot, which is why the stamp under it
     * exists (#398).
     */
    const val MONEY_ORIGIN: String =
        "El mayor de tres precios en cada moneda: el catálogo de Numista, lo que pagaste o lo que " +
            "vale su plata."

    /**
     * The same criterion, short enough for the plate's one-line total (#408).
     *
     * Las cifras spells the three sources under the amount; the plate only has room for the method.
     */
    const val MONEY_CRITERION: String = "al mayor de tres precios"

    /**
     * What the two figures of a plate's header are called, said whole and not abbreviated (#493).
     *
     * The words are the whole of what tells them apart. In a plate one casilla from closing the two
     * amounts are of the same order — the father's 20 escudos has the one inside at 1,4× the one to
     * close — so no type size could distinguish them, and in The Queen's Beasts closing costs 1,75×
     * what is in it, which puts the *larger* number on the second line.
     *
     * «Coste de cerrar» names a purchase and not a lack, which is the sentence ADR 0026 §10 spent a
     * document avoiding. And «Valor actual» is said in full rather than as the short «dentro» the
     * prototype tried first: 22 of the father's 49 reachable plates have no second figure at all, and
     * the short word survived alone on a plate with nothing left to distinguish itself from.
     */
    const val PLATE_VALUE_LABEL: String = "Valor actual"
    const val PLATE_COST_LABEL: String = "Coste de cerrar"

    /**
     * Where the cost of closing comes from, which is **not** where the value comes from.
     *
     * A hole has no «lo que pagaste», so its prices are two and not three, and it is priced in `unc`
     * (ADR 0028 §8) — «sin circular», the same words [uncirculatedSentence] uses of a piece. So the
     * criterion travels with its own amount, which is what #408 asked for and what the plate's first
     * figure has done since.
     *
     * It names the grade the **catalogue** half is asked in, the way [MONEY_CRITERION] names a method
     * rather than the source of any single euro: the second of the two prices is the coin's own metal,
     * which has no grade to be quoted in. If that ever reads as a claim about where the amount came
     * from, the honest line is «el mayor de dos precios, en sin circular» — longer, and it was not what
     * was chosen with the drawing in front of it.
     */
    const val HOLE_CRITERION: String = "en sin circular"

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

    /** Where a piece's own value came from, in the ficha. */
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
 *
 * **Absent while the snapshot is unread** (#418): `null` prints «—» rather than «0,00 kg», which
 * would claim the collection weighs nothing for the second the placeholder is still reading.
 */
fun figuresCellCount(grams: Double?): String =
    if (grams == null) UNKNOWN_COUNT else kilogramsLabel(grams)

/**
 * The one sentence of the valuation, in the settings screen.
 *
 * The pass is silent everywhere else — this line exists for the same reason the photographs' does:
 * «faltan y están cayendo» and «faltan porque se acabó el presupuesto» look identical from outside and
 * need different things from the collector (ADR 0028 §6).
 *
 * Settled ignores `held`: a spent budget (or sync, or offline) is only news while something is still
 * missing — otherwise «faltan 0… seguirán el mes que viene» contradicts itself (#421).
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
 * The fine silver, said in the metal block and **not** in «la materia».
 *
 * It is where the prototype had it («de plata pura, 192 oz») and where it belongs: the bar above it has
 * just split the mass into silver and copper, and this is that silver weighed in the unit bullion is
 * quoted in. In «la materia» it rode next to a `7,14 kg` that the first ladder repeated in display size
 * three lines below (#398).
 *
 * **«que son» and not «de plata pura, …»**, which is what the prototype said and what the phone proved
 * wrong: under `PLATA 6,14 KG (86 %)` the bare phrase reads as a second figure, when 6,14 kg and 196,4 oz
 * are the same silver weighed twice. Said as a conversion it is the unit the collector buys in, and
 * nothing new to reconcile.
 */
fun fineSilverSentence(ounces: Double): String = "que son ${fineOuncesLabel(ounces)} de plata pura"

/** The collection's own census, which is pieces and issuers — the weight is the ladders' to say. */
fun matterCensusLabel(pieces: Int, issuers: Int): String = "$pieces piezas de $issuers emisores"

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
 * What he paid against what those same pieces are worth today, **with its own denominator in front**.
 *
 * The one money question the page did not answer, and it costs nothing: `price` is already on the phone
 * and the value is the total's own rule read over a subset (#491).
 *
 * What it says is «de las 91 piezas cuyo precio anotaste» and never «el 84 % de tus monedas no las
 * compraste». The complement is not missing data — it is what he did not buy, gifts and inheritance —
 * but in the first 140 rows he was not writing prices down yet, so purchases he never noted are mixed
 * in with the presents. Said as a percentage of the collection it would turn a 2019 habit into a claim
 * about his life, and he has no way to check it. Only what is declared is counted.
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
 * The stamp under the amount: **which** silver price bought the metal floor, and when it was read.
 *
 * It is what stops the total reading as a quotation (ADR 0028 §5), and until #398 it could not do that
 * job: it said «plata de hoy» — a date with no figure in it, in the same small caps and the same rust as
 * `EL VALOR` and `LA MATERIA`, so it read as the heading of the block below rather than as a note on
 * the amount above. The price was already in hand: `SilverSpot` carries it next to the timestamp.
 *
 * The hour is the #326's own request («el sello del spot con su hora») and it belongs to the same day:
 * the spot expires daily, so «hoy 11:52» says the floor is this morning's and not last night's. Past
 * that it drops off, because on «hace 12 días» the hour of the twelfth day back explains nothing.
 */
fun spotStampLabel(
    spot: SilverSpot,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = "plata: ${eurosPerOunceLabel(spot.eurPerTroyOunce)} · ${readAtLabel(spot.readAtMillis, nowMillis, zone)}"

fun eurosPerOunceLabel(eurPerTroyOunce: Double): String = "${decimal(eurPerTroyOunce, 2)} €/oz"

/**
 * When the spot was read, in **calendar** days and not elapsed ones.
 *
 * `toDays(now - readAt)` counted 24-hour blocks, so a spot read yesterday at 23:00 and looked at this
 * morning at 08:00 came out as nine hours, which is zero days, which was announced as «hoy». Invisible
 * while the line carried no hour; a plain contradiction the moment it carries one — «hoy 23:00» read
 * before midday (#398).
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

/** The arc, which is two years and the distance between them. */
fun arcLabel(years: Int): String = "$years años"

/**
 * What a country is, as the four shares of it that the portrait leads with.
 *
 * The share of the **value** is money, so it is absent exactly when the money section is (ADR 0028
 * §4) — and absent means the clause is not written, not that it is written as zero.
 *
 * It takes the portrait and not its four numbers, as `plateUnavailableLabel` takes the reason and
 * `syncReportLabel` the record: four `Double`s in a row are four chances to hand over the mass share
 * where the silver one goes, and nothing would go red.
 */
fun portraitSharesLabel(portrait: CountryPortrait): String = buildString {
    append("${percentLabel(portrait.pieceShare)} de tus piezas")
    append(" · ${percentLabel(portrait.massShare)} del peso")
    append(" · ${percentLabel(portrait.silverShare)} de la plata")
    portrait.valueShare?.let { append(" · ${percentLabel(it)} del valor") }
}

/**
 * A diameter under a coin drawn to scale, in whole millimetres.
 *
 * Whole and not one decimal, unlike the printed page: on screen the coin beside it *is* the measure,
 * and the number is there to say which of the two extremes this one is. The paper has no coin to
 * compare against and no ruler on a scaled page, so it keeps the tenth (`printedDiameterLabel`).
 */
fun screenDiameterLabel(millimetres: Double): String = "${millimetres.toInt()} mm"

/**
 * The four sentences «al margen», out of the ficha that was already in the APK.
 *
 * Nobody asked for them. They are what the page has instead of a colophon, and they are the reason
 * it reads as a field guide: 75 % of his coins are no longer money anywhere, and 246 of them were
 * engraved by one man.
 *
 * They were written inside the screen, which is where the copy of the densest page in the app was
 * least visible — `FiguresLabels` promises to hold every string «Las cifras» prints, and four
 * sentences hidden in `FiguresScreen.kt` were the promise's one exception (ADR 0026 §6).
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
 * The fifth sentence at the margin: how the collection is kept, and the only one that is his own typing.
 *
 * **A sentence and not a histogram.** Seven grades in bars are the dashboard `spec.md §0.4` refuses;
 * one line is what the four figures beside it already are.
 *
 * **«o casi» is the second grade and not a hedge.** `au` is *about* uncirculated, and calling it «sin
 * circular» would file 66 of his pieces under a word their own ficha does not use.
 *
 * As a share of **pieces**, like everything else on the page. By row it would read 78 % — the «3 de
 * cada 4» of #491 — and by piece it reads 40 %, because his seven Venezuelan bulks are 298 pieces
 * graded `f`. Both are true; two denominators on one page is a figure nobody can check.
 */
fun uncirculatedSentence(figure: MarginFigure): String =
    "${percentLabel(figure.shareOfPieces())} están sin circular o casi"

/**
 * The share the demonetized figure is a share **of**, which is the whole collection.
 *
 * Its denominator is every piece and not the types Numista answered for: a percentage over a moving
 * denominator is a figure nobody can check.
 */
private fun MarginFigure.shareOfPieces(): Double =
    if (outOf <= 0) 0.0 else pieces.toDouble() / outOf

/**
 * What a coin's ficha says about its value, with the origin.
 *
 * The origin is not an ornament: «un número sin procedencia en una app de dos usuarios es un número que
 * nadie puede comprobar» (#316). It is dropped only when the pieces of the type do not agree on one,
 * because then no single origin is true of all of them.
 */
fun coinValueLabel(value: CoinValue): String {
    val head = eurosLabel(value.eur)
    val origin = value.source?.let { FiguresLabels.valueOrigin(it, value.grade) }
    val pieces = "${value.pieces} piezas".takeIf { value.pieces > 1 }
    return listOfNotNull(head, pieces, origin).joinToString(" · ")
}

/**
 * The amount a plate holds with its provenance, and nothing to name it.
 *
 * A total over the casillas of one plate, which the grain rule allows: per plate it is a plan, and the
 * same sum over the whole shelf would be «te faltan decenas de miles de euros» (ADR 0026 §10). The
 * criterion rides with the amount — «al mayor de tres precios» — so the plate is not the only money
 * surface without a provenance tag (#408).
 *
 * Unnamed because the printed page names it in the label of its own row («Valor»), where a figure that
 * carried its title inside would say the word twice. The screen has no row and no column, so it says
 * the name itself — see [plateValueLabel].
 */
fun plateAmountLabel(value: PlateValue): String =
    "${eurosLabel(value.eur)} · ${FiguresLabels.MONEY_CRITERION}"

/**
 * The first line of a plate's header: what is inside, said with its name (#493).
 *
 * The name is what tells this figure from the one under it, and it is dropped only where something
 * else is already saying it — see [plateAmountLabel], which is the paper's reading of the same amount.
 */
fun plateValueLabel(value: PlateValue): String =
    "${FiguresLabels.PLATE_VALUE_LABEL}: ${plateAmountLabel(value)}"

/**
 * The second line: what closing the plate costs, with **its own** provenance (#493).
 *
 * Not a share of the first line's criterion, and this is the correction the prototype's own variant
 * needed: a hole has no «lo que pagaste» and is priced in `unc`, so the two figures of one header come
 * out of two different rules and each says which (ADR 0028 §8).
 *
 * There is no reading of this for a closed plate. A plate with nothing missing has no cost, which is
 * absence and not a zero: the line is not written rather than written as «0 €».
 */
fun plateCostLabel(cost: PlateCost): String =
    "${FiguresLabels.PLATE_COST_LABEL}: ${eurosLabel(cost.eur)} · ${FiguresLabels.HOLE_CRITERION}"

/**
 * The price stamped inside one empty casilla: the amount alone, and nothing else (#493).
 *
 * The criterion is not repeated here. It was said once in the header, three lines above, and the same
 * seven words under every hole of a plate of ten would be the frequency ADR 0026 §5 prices: what the
 * stamp adds to the header is **which** hole costs what, and the header has already said out of what.
 */
fun holeCostLabel(eur: Double): String = eurosLabel(eur)

