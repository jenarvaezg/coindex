package com.jenarvaezg.coindex.ui.ficha

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.ficha.FichaBodies
import com.jenarvaezg.coindex.data.ficha.FichaDetails
import com.jenarvaezg.coindex.data.ficha.FichaFace
import com.jenarvaezg.coindex.data.ficha.PieceReading
import com.jenarvaezg.coindex.data.ficha.decodeEntities
import com.jenarvaezg.coindex.data.prices.PriceBook
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.NUMISTA_GRADES
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.fineSilverGrams
import com.jenarvaezg.coindex.domain.gramsToOunces
import com.jenarvaezg.coindex.domain.holeLadder
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.FiguresLabels
import com.jenarvaezg.coindex.ui.PlateSubject
import com.jenarvaezg.coindex.ui.coinFichaIdentity
import com.jenarvaezg.coindex.ui.coinValue
import com.jenarvaezg.coindex.ui.coverageLabel
import com.jenarvaezg.coindex.ui.eurosLabel
import com.jenarvaezg.coindex.ui.shelf.coinRowOf
import com.jenarvaezg.coindex.ui.shelf.coinYearsLabel
import java.time.LocalDate
import java.time.ZoneId

/**
 * Everything the coin's screen draws, worded (#643, ADR 0026 §13), as [PlateSubject] is for a
 * plate. [value] is null on a hole and until the market lands (ADR 0028 §7); [missing] is an empty
 * casilla, whose [cost] the screen prints instead.
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
    val prices = FichaPrices(typeId, meta, book, settled, nowMillis, zone)
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

/** «Valor actual», with its origin when the pieces agree on one and its coverage when partial. */
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
    prices: FichaPrices,
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
    val count = pieceCount(items)
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

/** A row of ×N is N pieces. */
private fun pieceCount(items: List<CollectedItem>): Int =
    items.fold(0) { total, item -> saturatingAdd(total, item.quantity.coerceAtLeast(1)) }

private fun dateKey(year: Int?): String = year?.toString() ?: NO_DATE

private const val NO_DATE = "—"

private fun dateTiles(
    items: List<CollectedItem>,
    pieces: List<FichaPiece>,
    dated: Int?,
): List<DateTile> = items.groupBy { dateKey(it.recordedYear) }.map { (key, rows) ->
    val count = pieceCount(rows)
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

/** What the collector recorded about the purchase, the issue's mintage and the swap flag. */
private fun pieceFacts(item: CollectedItem, reading: PieceReading): String? {
    val paid = item.price?.takeIf { it > 0.0 }
    return listOfNotNull(
        when {
            paid != null -> FichaPageLabels.paid(paid, item.quantity, reading.acquiredOn)
            reading.acquiredOn != null -> FichaPageLabels.arrived(reading.acquiredOn)
            else -> null
        },
        reading.mintage?.let(FichaPageLabels::mintage),
        FichaPageLabels.FOR_SWAP.takeIf { item.forSwap == true },
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")
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
private fun legalTender(demonetized: Boolean?, on: LocalDate?): String? = when (demonetized) {
    true -> on?.let(FichaPageLabels::demonetizedOn) ?: FichaPageLabels.DEMONETIZED
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
