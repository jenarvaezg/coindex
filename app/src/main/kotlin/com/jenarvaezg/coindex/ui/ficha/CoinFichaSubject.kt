package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.ficha.FichaBodies
import com.jenarvaezg.coindex.data.ficha.FichaDetails
import com.jenarvaezg.coindex.data.ficha.FichaFace
import com.jenarvaezg.coindex.data.ficha.PieceReading
import com.jenarvaezg.coindex.data.ficha.decodeEntities
import com.jenarvaezg.coindex.data.photos.CoinPhoto
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.Metal
import com.jenarvaezg.coindex.domain.NUMISTA_GRADES
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.PieceValue
import com.jenarvaezg.coindex.domain.PriceLadder
import com.jenarvaezg.coindex.domain.PrintedSide
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.fineSilverGrams
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.holeLadder
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.pieceLadder
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.FiguresLabels
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.coinFichaIdentity
import com.jenarvaezg.coindex.ui.coinValue
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.eurosLabel
import com.jenarvaezg.coindex.ui.numistaStampLabel
import com.jenarvaezg.coindex.ui.shelf.coinRowOf
import com.jenarvaezg.coindex.ui.shelf.coinYearsLabel
import com.jenarvaezg.coindex.ui.spotStampLabel
import java.time.LocalDate
import java.time.ZoneId

/**
 * Everything the coin's screen draws, worded (#643, ADR 0026 §13). Built once per reading and
 * request, like [PlateSubject] for a plate, so the screen only lays it out.
 *
 * @param owned whether the collector holds the type, which decides how its hole is drawn.
 * @param value «Valor actual», the sum of each piece's greatest price; null on a hole and until the
 *   market lands (ADR 0028 §7).
 * @param missing opened from an empty casilla: «Te falta», and [cost] says what filling it costs.
 */
data class CoinFichaSubject(
    val typeId: Int,
    val title: String,
    val identity: String,
    val owned: Boolean,
    val backed: Boolean,
    val value: String?,
    val missing: Boolean,
    val cost: String?,
    val sections: List<FichaSection>,
)

/** One folded section: closed it says [summary] on one line (#643). */
sealed interface FichaSection {
    val title: String
    val summary: String

    /** Whether it enters open. Only the pieces ever do, from a date run's casilla. */
    val open: Boolean get() = false
}

/**
 * The collector's pieces of the coin. With more than [MANY_ROWS] rows they are chosen by date:
 * [dates] is the grid, [selected] the date shown on entry.
 */
data class PiecesSection(
    override val title: String,
    override val summary: String,
    override val open: Boolean,
    val pieces: List<FichaPiece>,
    val dates: List<DateTile>,
    val selected: String?,
) : FichaSection

/** What filling the casilla would cost: Numista in `unc` and the metal (ADR 0028 §8). */
data class CostSection(
    override val title: String,
    override val summary: String,
    val rungs: List<FichaRung>,
) : FichaSection

/** The collections that claim the coin (#635), and the plate it came from in miniature. */
data class ClaimsSection(
    override val title: String,
    override val summary: String,
    val claims: List<FichaClaim>,
    val strip: FichaStrip?,
) : FichaSection

/** The cartouche: only the rows the ficha has. */
data class SpecSection(
    override val title: String,
    override val summary: String,
    val rows: List<Pair<String, String>>,
) : FichaSection

data class FacesSection(
    override val title: String,
    override val summary: String,
    val faces: List<FaceBlock>,
) : FichaSection

/** Numista's comments, kept as HTML so their links still open the browser. */
data class CommentsSection(
    override val title: String,
    override val summary: String,
    val html: String,
) : FichaSection

data class RelatedSection(
    override val title: String,
    override val summary: String,
    val rows: List<RelatedRow>,
) : FichaSection

/**
 * One collected row. [rungs] is empty until the market lands (ADR 0028 §7).
 *
 * @param dateKey the date tile it falls under.
 * @param here the piece of the casilla the screen was opened from.
 */
data class FichaPiece(
    val itemId: Long,
    val dateKey: String,
    val heading: String,
    val grade: String?,
    val quantity: String?,
    val here: Boolean,
    val facts: String?,
    val rungs: List<FichaRung>,
    val issueComment: String?,
    val note: String?,
    val photo: CoinPhoto?,
)

/**
 * One price of a piece or a hole, with its whole label and where it comes from; or, empty, why.
 *
 * @param share its length against the greatest rung, for the bar under it.
 */
data class FichaRung(
    val label: String,
    val amount: String?,
    val why: String?,
    val source: String?,
    val greatest: Boolean,
    val share: Float,
)

/** One date of the grid, with its grades and how many pieces it holds. */
data class DateTile(
    val key: String,
    val label: String,
    val grades: String,
    val quantity: String?,
    /** Some piece of the date has a note or a photograph. */
    val noted: Boolean,
    /** The date of the casilla the screen was opened from. */
    val casilla: Boolean,
)

/** The plate the screen was opened from, in miniature, on its own `printed_side`. */
data class FichaStrip(val cells: List<StripCell>, val count: String?, val side: PrintedSide)

data class StripCell(val typeId: Int?, val missing: Boolean, val here: Boolean)

/** A face or the edge, as Numista writes it. [photo] is the edge's, shown apart on request. */
data class FaceBlock(
    val heading: String,
    val lettering: String?,
    val unabridgedLegend: String?,
    val description: String?,
    val photo: CoinPhoto? = null,
)

data class RelatedRow(val typeId: Int, val country: String?, val title: String, val years: String?)

/** More rows than this and the pieces are chosen by date (#643). */
const val MANY_ROWS: Int = 3

/**
 * The coin's screen for [request] (#643).
 *
 * @param here the collection it was opened from, named among the claims but not linked (#635).
 * @param origin that collection's plate, drawn in miniature with the casilla marked.
 * @param settled whether the market has landed; until then no amount is shown (ADR 0028 §7).
 */
fun coinFichaSubject(
    state: CollectionState,
    request: CoinFichaRequest,
    bodies: FichaBodies,
    here: CardDestination?,
    origin: PlateSubject?,
    book: PriceBook,
    settled: Boolean,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): CoinFichaSubject {
    val typeId = request.typeId
    val row = coinRowOf(state, typeId)
    val meta = state.typeMeta[typeId]
    val details = bodies.details
    val casilla = request.casilla
    val prices = Prices(typeId, meta, book, settled, nowMillis, zone)
    val items = state.items
        .filter { it.typeId == typeId }
        .sortedWith(compareBy(nullsLast()) { item: CollectedItem -> item.recordedYear })
    val holeIssue = casilla?.takeIf { it.missing }?.let { missing ->
        HoleIssue(book.listings.issueOf(typeId, missing.year, listOfNotNull(missing.issueId)))
    }
    val hole = holeIssue?.let { holeLadder(typeId, it.issueId, meta, book.spot, book::of) }
    val claims = fichaClaims(row.claims, here)
    return CoinFichaSubject(
        typeId = typeId,
        title = row.rawTitle,
        identity = coinFichaIdentity(row),
        owned = row.quantity > 0,
        backed = row.claims.isNotEmpty(),
        value = coinValueLine(state, book, typeId, row.quantity).takeIf { settled },
        missing = hole != null,
        cost = hole?.greatest?.takeIf { settled }?.let {
            FichaPageLabels.holeCost(eurosLabel(it.eur))
        },
        sections = listOfNotNull(
            hole?.let { costSection(prices.holeRungs(it, holeIssue.issueId)) },
            piecesSection(items, bodies.pieces, casilla, state.emissionLabels, prices),
            claimsSection(claims, origin, typeId, casilla),
            specSection(meta, details, zone),
            facesSection(details),
            commentsSection(details),
            relatedSection(details),
        ),
    )
}

/** «Valor actual: 1.653 € · al mayor de tres precios», with its coverage when it isn't whole. */
private fun coinValueLine(
    state: CollectionState,
    book: PriceBook,
    typeId: Int,
    pieces: Int,
): String? {
    val value = coinValue(typeId, state, book) ?: return null
    val origin = value.source?.let { FiguresLabels.valueOrigin(it, value.grade) }
        ?: FichaPageLabels.GREATEST_OF_THREE
    return listOfNotNull(
        FichaPageLabels.value(eurosLabel(value.eur), origin),
        coverageLabel(value.pieces, pieces),
    ).joinToString(" · ")
}

/** The issue an empty casilla is priced by, which may be unknown until a listing says (#452). */
private class HoleIssue(val issueId: Int?)

private fun costSection(rungs: List<FichaRung>) = CostSection(
    title = FichaPageLabels.COST,
    summary = FichaPageLabels.COST_SUMMARY,
    rungs = rungs,
)

private fun piecesSection(
    items: List<CollectedItem>,
    readings: Map<Long, PieceReading>,
    casilla: CasillaOrigin?,
    emissionLabels: Map<Long, String>,
    prices: Prices,
): PiecesSection? {
    if (items.isEmpty()) return null
    val dated = casilla?.takeIf { it.dated }?.year
    val pieces = items.map { item ->
        val reading = readings[item.id] ?: PieceReading()
        FichaPiece(
            itemId = item.id,
            dateKey = dateKey(item.recordedYear),
            heading = listOfNotNull(
                item.recordedYear?.toString() ?: FichaPageLabels.NO_YEAR,
                reading.mintLetter,
                emissionLabels[item.id],
            ).joinToString(" · "),
            grade = item.grade?.lowercase(),
            quantity = item.quantity.takeIf { it > 1 }?.let(FichaPageLabels::quantity),
            here = dated != null && item.recordedYear == dated,
            facts = pieceFacts(item, reading),
            rungs = prices.pieceRungs(item),
            issueComment = reading.issueComment?.let(FichaPageLabels::issueComment),
            note = reading.privateComment?.let(FichaPageLabels::privateNote),
            photo = reading.pictures.firstOrNull(),
        )
    }
    val count = items.fold(0) { total, item -> total + item.quantity.coerceAtLeast(1) }
    val dates = if (items.size > MANY_ROWS) dateTiles(items, pieces, dated) else emptyList()
    val selected = dates.firstOrNull { it.casilla }?.key ?: dates.firstOrNull()?.key
    val years = items.mapNotNull { it.recordedYear }.distinct().sorted()
    return PiecesSection(
        title = if (count == 1) FichaPageLabels.ONE_PIECE else FichaPageLabels.pieces(count),
        summary = if (dates.isNotEmpty()) {
            FichaPageLabels.piecesByDate(count, dates.size, coinYearsLabel(years))
        } else {
            items.joinToString(" · ") { item ->
                listOfNotNull(
                    item.recordedYear?.toString() ?: FichaPageLabels.NO_YEAR,
                    readings[item.id]?.mintLetter,
                    item.grade?.lowercase(),
                ).joinToString(" ")
            }
        },
        open = dates.any { it.casilla },
        pieces = pieces,
        dates = dates,
        selected = selected,
    )
}

private fun dateKey(year: Int?): String = year?.toString() ?: NO_DATE

private const val NO_DATE = "—"

private fun dateTiles(
    items: List<CollectedItem>,
    pieces: List<FichaPiece>,
    dated: Int?,
): List<DateTile> = items.groupBy { dateKey(it.recordedYear) }.map { (key, rows) ->
    val count = rows.fold(0) { total, item -> total + item.quantity.coerceAtLeast(1) }
    val drawn = pieces.filter { it.dateKey == key }
    DateTile(
        key = key,
        label = key,
        grades = rows
            .mapNotNull { it.grade?.lowercase() }
            .distinct()
            .sortedBy { NUMISTA_GRADES.indexOf(it).let { at -> if (at < 0) Int.MAX_VALUE else at } }
            .joinToString(" "),
        quantity = count.takeIf { it > 1 }?.let(FichaPageLabels::quantity),
        noted = drawn.any { it.note != null || it.photo != null },
        casilla = dated != null && key == dated.toString(),
    )
}

/** «pagaste 400 € el 3 ago 2026 · tirada de 117.000 · para cambio». */
private fun pieceFacts(item: CollectedItem, reading: PieceReading): String? {
    val acquired = reading.acquiredOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val paid = item.price?.takeIf { it > 0.0 }
    return listOfNotNull(
        when {
            paid != null -> FichaPageLabels.paid(paid, acquired)
            acquired != null -> FichaPageLabels.arrived(acquired)
            else -> null
        },
        reading.mintage?.let(FichaPageLabels::mintage),
        FichaPageLabels.FOR_SWAP.takeIf { item.forSwap == true },
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** The rungs of every ladder on the screen, worded against one price book and one clock. */
private class Prices(
    private val typeId: Int,
    private val meta: TypeMeta?,
    private val book: PriceBook,
    private val settled: Boolean,
    private val nowMillis: Long,
    private val zone: ZoneId,
) {
    fun pieceRungs(item: CollectedItem): List<FichaRung> {
        if (!settled) return emptyList()
        val ladder = pieceLadder(item, meta, book.spot, book::of)
        return rungs(
            ladder,
            listOf(
                marketRung(ladder.market, item.issueId, unknown = FichaPageLabels.NO_ISSUE),
                Rung(ladder.paid, FichaPageLabels.PAID, FichaPageLabels.NOT_PAID, source = null),
                silverRung(ladder.silver),
            ),
        )
    }

    fun holeRungs(ladder: PriceLadder, issueId: Int?): List<FichaRung> {
        if (!settled) return emptyList()
        // A hole's issue comes from a listing, so an unknown one was never asked about.
        val market = marketRung(ladder.market, issueId, unknown = FichaPageLabels.NUMISTA_NOT_ASKED)
        return rungs(ladder, listOf(market, silverRung(ladder.silver)))
    }

    private fun marketRung(market: PieceValue?, issueId: Int?, unknown: String): Rung {
        val readAt = issueId?.let { book.readAt(typeId, it) }
        return Rung(
            value = market,
            label = market?.grade
                ?.let { FichaPageLabels.market(it, market.source == ValueSource.NeighbouringGrade) }
                ?: FichaPageLabels.MARKET,
            why = when {
                issueId == null -> unknown
                readAt == null -> FichaPageLabels.NUMISTA_NOT_ASKED
                else -> FichaPageLabels.NUMISTA_HAS_NO_PRICE
            },
            source = readAt?.let { numistaStampLabel(it, nowMillis, zone) },
        )
    }

    private fun silverRung(silver: PieceValue?): Rung {
        val fine = fineSilverGrams(meta)
        val spot = book.spot
        return Rung(
            value = silver,
            label = FichaPageLabels.SILVER,
            why = when {
                meta?.metal != Metal.Silver -> FichaPageLabels.NO_SILVER
                fine == null -> FichaPageLabels.NO_FINE_SILVER
                else -> FichaPageLabels.NO_SPOT
            },
            source = if (fine != null && spot != null) {
                FichaPageLabels.fineSilverSource(fine, spotStampLabel(spot, nowMillis, zone))
            } else {
                null
            },
        )
    }

    private fun rungs(ladder: PriceLadder, rungs: List<Rung>): List<FichaRung> {
        val greatest = ladder.greatest
        val top = rungs.mapNotNull { it.value?.eur }.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
        return rungs.map { rung ->
            val value = rung.value
            FichaRung(
                label = rung.label,
                amount = value?.let { eurosLabel(it.eur) },
                why = rung.why.takeIf { value == null },
                source = rung.source.takeIf { value != null },
                greatest = value != null && value == greatest,
                share = ((value?.eur ?: 0.0) / top).toFloat(),
            )
        }
    }

    private class Rung(
        val value: PieceValue?,
        val label: String,
        val why: String,
        val source: String?,
    )
}

private fun claimsSection(
    claims: List<FichaClaim>,
    origin: PlateSubject?,
    typeId: Int,
    casilla: CasillaOrigin?,
): ClaimsSection? {
    val heading = fichaClaimsHeading(claims) ?: return null
    val strip = origin?.let { plate ->
        FichaStrip(
            cells = plate.cells.map { cell ->
                StripCell(
                    typeId = cell.numistaTypeId,
                    missing = cell.missing,
                    here = cell.numistaTypeId == typeId &&
                        (casilla?.year == null || cell.year == casilla.year.toString()),
                )
            },
            count = plate.ratio,
            side = plate.printedSide,
        )
    }
    return ClaimsSection(
        title = heading,
        summary = (claims.map { it.claim.name } + listOfNotNull(strip?.count)).joinToString(" · "),
        claims = claims,
        strip = strip,
    )
}

private fun specSection(meta: TypeMeta?, details: FichaDetails, zone: ZoneId): SpecSection? {
    val metal = details.composition?.substringBefore(" (")?.trim()
    val weight = meta?.weightGrams?.let(FichaPageLabels::grams)
    val fine = fineSilverGrams(meta)
    val rows = listOf(
        FichaPageLabels.ROW_FACE_VALUE to details.faceValue,
        FichaPageLabels.ROW_CURRENCY to details.currency,
        FichaPageLabels.ROW_RULER to details.rulers.joinedOrNull(),
        FichaPageLabels.ROW_SERIES to details.series,
        FichaPageLabels.ROW_TOPIC to details.topic,
        FichaPageLabels.ROW_YEARS to FichaPageLabels.yearSpan(meta?.minYear, meta?.maxYear),
        FichaPageLabels.ROW_MINT to meta?.mints.orEmpty().joinedOrNull(),
        FichaPageLabels.ROW_METAL to metal,
        FichaPageLabels.ROW_WEIGHT to weight,
        FichaPageLabels.ROW_FINE_SILVER to fine?.let {
            "${FichaPageLabels.grams(it)} · ${FichaPageLabels.ounces(gramsToOunces(it))}"
        },
        FichaPageLabels.ROW_DIAMETER to meta?.sizeMillimetres?.let(FichaPageLabels::millimetres),
        FichaPageLabels.ROW_THICKNESS to
            meta?.thicknessMillimetres?.let(FichaPageLabels::millimetres),
        FichaPageLabels.ROW_EDGE to details.edge?.description?.lineSequence()?.firstOrNull(),
        FichaPageLabels.ROW_ORIENTATION to details.orientation?.let(FichaPageLabels::orientation),
        FichaPageLabels.ROW_TECHNIQUE to details.technique,
        FichaPageLabels.ROW_LEGAL_TENDER to legalTender(meta?.demonetized, details.demonetizedOn),
        FichaPageLabels.ROW_HANDS to meta?.hands.orEmpty().joinedOrNull(),
        FichaPageLabels.ROW_REFERENCES to details.references.takeIf { it.isNotEmpty() }
            ?.let(FichaPageLabels::references),
    ).mapNotNull { (label, value) -> value?.takeIf(String::isNotBlank)?.let { label to it } }
    if (rows.isEmpty()) return null
    val exonumia = objectClassOf(meta?.category) == ObjectClass.Exonumia
    return SpecSection(
        title = if (exonumia) FichaPageLabels.MEDAL else FichaPageLabels.COIN,
        summary = listOfNotNull(metal, weight, meta?.mints?.firstOrNull()).joinToString(" · "),
        rows = rows,
    )
}

private fun List<String>.joinedOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(", ")

/** Silence is not «still money» (#221): no row when Numista doesn't say. */
private fun legalTender(demonetized: Boolean?, on: String?): String? = when (demonetized) {
    true -> on?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.let(FichaPageLabels::demonetizedOn)
        ?: FichaPageLabels.DEMONETIZED
    false -> FichaPageLabels.STILL_MONEY
    null -> null
}

private fun facesSection(details: FichaDetails): FacesSection? {
    val edge = details.edge
    val faces = listOfNotNull(
        details.obverse?.let { faceBlock(FichaPageLabels.OBVERSE, it) },
        details.reverse?.let { faceBlock(FichaPageLabels.REVERSE, it) },
        edge?.let {
            FaceBlock(
                heading = FichaPageLabels.EDGE,
                lettering = it.lettering,
                unabridgedLegend = it.unabridgedLegend,
                description = it.description,
                photo = it.photo,
            )
        },
    )
    if (faces.isEmpty()) return null
    val first = faces.firstNotNullOfOrNull { it.lettering }?.let(::flatLegend)
        ?: faces.firstNotNullOf { it.description ?: it.unabridgedLegend }
    return FacesSection(FichaPageLabels.FACES, oneLine(first, FACES_SUMMARY_CHARACTERS), faces)
}

private fun faceBlock(heading: String, face: FichaFace) =
    FaceBlock(heading, face.lettering, face.unabridgedLegend, face.description)

private fun commentsSection(details: FichaDetails): CommentsSection? {
    val html = details.comments ?: return null
    val text = decodeEntities(html.replace(TAG, " "))
    if (text.isBlank()) return null
    return CommentsSection(
        title = FichaPageLabels.COMMENTS,
        summary = oneLine(text, COMMENTS_SUMMARY_CHARACTERS),
        html = html,
    )
}

private val TAG = Regex("<[^>]+>")

private fun relatedSection(details: FichaDetails): RelatedSection? {
    val related = details.related.takeIf { it.isNotEmpty() } ?: return null
    return RelatedSection(
        title = FichaPageLabels.related(related.size),
        summary = related.mapNotNull { it.country }.distinct().joinToString(", "),
        rows = related.map { type ->
            RelatedRow(
                typeId = type.typeId,
                country = type.country,
                title = type.title,
                years = FichaPageLabels.yearSpan(type.minYear, type.maxYear),
            )
        },
    )
}

/** A summary fits one line: whitespace folded, cut at a word with an ellipsis. */
private fun oneLine(text: String, limit: Int): String {
    val folded = text.replace(WHITESPACE, " ").trim()
    if (folded.length <= limit) return folded
    return folded.take(limit).substringBeforeLast(' ').trimEnd(',', ';', ':') + "…"
}

private val WHITESPACE = Regex("\\s+")

private const val FACES_SUMMARY_CHARACTERS = 44
private const val COMMENTS_SUMMARY_CHARACTERS = 70
