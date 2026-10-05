package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CoinClaims
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.placementYear
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.UNKNOWN_YEAR_LABEL
import com.jenarvaezg.coindex.ui.coinName
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.numistaCodeLabel
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.pieceRawTitle
import java.text.Collator
import java.util.Locale

/**
 * A collection that claims a coin, as a link to it (ADR 0021 §1, §9). Who claims what is resolved
 * by [CoinClaims]; this holds only the name and the destination.
 */
data class CoinClaim(val name: String, val destination: CardDestination)

private fun doorTo(card: IndexCard) = CoinClaim(card.name, destinationOf(card))

/**
 * One coin in Monedas: a Numista type and every piece of it the collector owns. Also used, through
 * [coinRowOf], for a type with no pieces, so the coin sheet reads the same from a casilla (#508).
 *
 * By type, not by collection row, as boxes store type ids: the selection (#173) picks coins, and
 * chip counts count coins.
 *
 * No reason line (ADR 0021 §12): «Sin colección» says which coins no collection claims; why is in
 * the field report.
 */
data class CoinRow(
    val typeId: Int,
    val name: CoinName,
    /** The untouched ficha title, searchable even where the cartouche omits words. */
    val rawTitle: String,
    /**
     * The country in Spanish, null when the type is uncached. [TypeMeta.country] rather than the
     * ficha's `issuer.name`, which carries periods like «Federación de Rusia (1991-presente)»
     * (ADR 0023).
     */
    val issuer: String?,
    /**
     * The years of the collector's pieces, oldest first; the ficha's when none is dated, empty when
     * neither (#448). From the pieces, not `TypeMeta.minYear`, which is when the type opens, as
     * `pieceLine` and `placementYear` already do. A list because one row covers every year held;
     * [coinYearsLabel] prints the span and the chips take every year in it.
     */
    val years: List<Int>,
    val objectClass: ObjectClass,
    val weightOz: Double?,
    /** Pieces of this type, saturating rather than overflowing. */
    val quantity: Int,
    /** The collections that claim it, in index order (ADR 0021 §6). */
    val claims: List<CoinClaim>,
    /**
     * Pieces of this type no collection claims, which can be non-zero even when [claims] isn't: a
     * catalog qualifying members by issue (ADR 0019) may claim one row of a type and not another.
     * The «Sin colección» chip relies on it (ADR 0021 §12).
     */
    val unclaimedPieces: Int,
    /**
     * Year-axis seats that lead to this coin without being printed on its cartouche (#550):
     *
     * - Where the axis places its pieces: `placementYear`, Gregorian first, can differ from the
     *   engraved year on the card (ADR 0014).
     * - The years of casillas on plates it is evidence for, so a date-run hole opens the coin the
     *   collector does hold.
     *
     * Not the ficha's `minYear`–`maxYear` run: a restrike type filed over centuries would answer to
     * almost every seat.
     */
    val axisYears: List<Int> = emptyList(),
) {
    val title: String get() = name.text

    /** What «Más antiguas» and «Más nuevas» sort on; null rows go last in both. */
    val oldestYear: Int? get() = years.firstOrNull()
    val newestYear: Int? get() = years.lastOrNull()

    /**
     * The year chips this row matches: its years or «Sin año» (#448), plus [axisYears] (#550). Axis
     * years add to «Sin año» rather than replacing it.
     */
    val yearFilters: List<YearFilter> = YearFilter.of(years) + axisYears.map(YearFilter::Of)

    /**
     * What the search matches against, folded once. Includes every year, [axisYears] too, so typing
     * a year finds what its chip finds.
     */
    val haystack: String = fold(
        listOf(rawTitle)
            .plus(listOfNotNull(issuer))
            .plus(years.map(Int::toString))
            .plus(axisYears.map(Int::toString))
            .plus(typeId.toString())
            .plus(claims.map { it.name })
            .joinToString(" "),
    )
}

/**
 * Every coin the collector owns, with the collections that claim it, built from [CollectionState]
 * alone (ADR 0021 §1). Includes coins no collection claims.
 */
fun coinRows(state: CollectionState, slots: SlotYears = SlotYears.none): List<CoinRow> {
    val claimed = state.claims
    val byType = LinkedHashMap<Int, MutableList<CollectedItem>>()
    // [coinRow] counts a zero quantity as one piece, as [collectionFigures] does, so the bottom
    // bar and Monedas agree (#426).
    for (item in state.items) {
        byType.getOrPut(item.typeId) { mutableListOf() }.add(item)
    }
    return byType
        .map { (typeId, pieces) -> coinRow(state, typeId, pieces, claimed, slots) }
        .sortedWith(coinReadingOrder())
}

/**
 * The row for one type, whether or not the collector owns any of it (#508), for the coin sheet a
 * casilla opens. Never part of [coinRows]; with no pieces it has `quantity` 0 and no `claims`.
 * Built by the same [coinRow] so the sheet reads the same from Monedas and from a casilla.
 */
fun coinRowOf(state: CollectionState, typeId: Int): CoinRow = coinRow(
    state = state,
    typeId = typeId,
    pieces = state.items.filter { it.typeId == typeId },
    claimed = state.claims,
    // [CoinRow.axisYears] only serves the shelf's chips.
    slots = SlotYears.none,
)

private fun coinRow(
    state: CollectionState,
    typeId: Int,
    pieces: List<CollectedItem>,
    claimed: CoinClaims,
    slots: SlotYears,
): CoinRow {
    val meta = state.typeMeta[typeId]
    val held = pieces.firstOrNull()
    val years = if (pieces.isEmpty()) typeYears(meta) else yearsOf(pieces, meta)
    return CoinRow(
        typeId = typeId,
        name = held?.let { pieceName(state, it) } ?: coinName(typeTitle(meta, typeId)),
        rawTitle = held?.let { pieceRawTitle(state, it) } ?: typeTitle(meta, typeId),
        issuer = meta?.country,
        // Without pieces, the type's span: see [typeYears].
        years = years,
        objectClass = objectClassOf(meta?.category),
        weightOz = meta?.weightOz,
        quantity = pieces.fold(0) { total, piece ->
            saturatingAdd(total, piece.quantity.coerceAtLeast(1))
        },
        claims = claimed.of(typeId).map(::doorTo),
        unclaimedPieces = claimed.unclaimedPieces(pieces),
        axisYears = axisYearsOf(pieces, meta, slots.of(typeId), printed = years),
    )
}

/**
 * See [CoinRow.axisYears]. Years already printed are dropped, or the row would count twice on that
 * chip.
 */
private fun axisYearsOf(
    pieces: List<CollectedItem>,
    meta: TypeMeta?,
    slotYears: Set<Int>,
    printed: List<Int>,
): List<Int> = pieces.mapNotNull { placementYear(it, meta) }
    .plus(slotYears)
    .filter { it > 0 && it !in printed }
    .distinct()
    .sorted()

/**
 * A type's name when no piece names it: the ficha title, or its Numista number when there is no
 * ficha (only for types outside the packaged cache).
 */
private fun typeTitle(meta: TypeMeta?, typeId: Int): String =
    meta?.title ?: meta?.displayTitle ?: numistaCodeLabel(typeId)

/**
 * Both ends of a type's span, for a row without pieces (#508). Not `minYear` alone (#448): for a
 * date run that would be the wrong year for the casilla just pressed.
 */
private fun typeYears(meta: TypeMeta?): List<Int> =
    listOfNotNull(meta?.minYear, meta?.maxYear).filter { it > 0 }.distinct().sorted()

/**
 * A coin's years as the card prints them (#448): one year, the span between the ends, or «Sin año».
 * A span rather than a list, since the cartouche has one line.
 */
fun coinYearsLabel(years: List<Int>): String = when (years.size) {
    0 -> UNKNOWN_YEAR_LABEL
    1 -> years.single().toString()
    else -> "${years.first()} – ${years.last()}"
}

/**
 * The pieces' years, oldest first, falling back on the ficha. Uses [CollectedItem.recordedYear],
 * the engraved year, as `pieceLine` does, not the Gregorian one (ADR 0014).
 */
private fun yearsOf(pieces: List<CollectedItem>, meta: TypeMeta?): List<Int> =
    pieces.mapNotNull { it.recordedYear }
        // Numista stores 0 on undated medals; zero is not a year, as in `placementYear` (#460).
        .filter { it > 0 }
        .distinct()
        .sorted()
        .ifEmpty { listOfNotNull(meta?.minYear) }

/** The grid line under the name: years, and the count when more than one. */
fun coinAlbumFootnote(row: CoinRow): String = listOfNotNull(
    coinYearsLabel(row.years),
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")

/**
 * The default Monedas order: country, then year, then title. Unknowns go last in both, so the list
 * doesn't open on whatever the last sync left uncached.
 */
private fun coinReadingOrder(): Comparator<CoinRow> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareBy<CoinRow> { it.issuer == null }
        .thenBy(collator) { it.issuer.orEmpty() }
        .thenBy { it.oldestYear ?: Int.MAX_VALUE }
        .thenBy(collator) { it.title }
        .thenBy { it.typeId }
}

/**
 * Spanish alphabetical order on the title, for «Alfabético». A [Collator], since raw string order
 * puts «Álbum» after «Zeta».
 */
internal fun coinTitleOrder(): Comparator<CoinRow> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareBy(collator) { it.title }
}
