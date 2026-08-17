package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
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

/** One collection that claims a coin, as the link back to it (ADR 0021 §1). */
data class CoinClaim(val name: String, val destination: CardDestination)

/**
 * One coin of Coins: a Numista **type** and every piece of it the collector owns.
 *
 * Since #508 it is also the reading of a coin the collector owns **none** of, because a casilla of a
 * lámina opens the same sheet and half of them are holes — see [coinRowOf], which is the only door
 * such a row comes through. It is one shape and not two on purpose: the sheet says the same sentences
 * about a coin whichever surface opened it.
 *
 * **By type and not by row**, for the same reason a box stores type ids: two rows of the same type
 * are the same coin twice, and the gesture born here (#173) picks coins. It is also what makes the
 * chips countable — «6 de 191» is a number of coins, and a ×2 would have made it a number of
 * receipts.
 *
 * There is **no reason line** (ADR 0021 §12). «Nothing is discarded silently» became «nothing is
 * discarded» the moment a piece got a place of its own: what the «Sin colección» filter answers is
 * *which* coins no collection claims, and *why* migrated to the field report, which is where the
 * curator already looks.
 */
data class CoinRow(
    val typeId: Int,
    val name: CoinName,
    /** The untouched ficha title: searchable even where the cartouche deliberately omits words. */
    val rawTitle: String,
    /**
     * The country, in Spanish, unsaid when the type is uncached.
     *
     * [TypeMeta.country] and not the ficha's `issuer.name`, because Numista names issuing entities
     * with their period of validity: the country of a `russie` type is «Rusia», and «Federación de
     * Rusia (1991-presente)» took a chip row to itself in the shelf below (ADR 0023).
     */
    val issuer: String?,
    /**
     * The years of the coins the collector holds, oldest first — the ficha's when none of them is
     * dated, and empty when there is neither (#448).
     *
     * **The piece and not the type.** `TypeMeta.minYear` is the year Numista's type *opens*, which is
     * the year of a coin only where the type has one year. Measured over the father's collection, 63
     * rows of 34 types said otherwise: his ¼ bolívar of 1948 printed 1894, his Libertad of 2024
     * printed 2000, his Morgan of 1898 printed 1878. The rest of the app had it right all along —
     * `pieceLine` reads `recordedYear` and `placementYear` prefers it over the ficha — and Coins was
     * the one surface throwing away the year that arrives with the collection row itself.
     *
     * A list and not one year, because a type is one card however many of it there are: seven of his
     * 170 dated types span several years and one of them spans twenty-one. [coinYearsLabel] prints
     * the arc, and the chips below take **every** year in it, so filtering by 1904 still finds it.
     */
    val years: List<Int>,
    val objectClass: ObjectClass,
    val weightOz: Double?,
    /** How many pieces of this type there are, saturating rather than wrapping. */
    val quantity: Int,
    /** Every collection that claims it, in the order the index shows them (ADR 0021 §6). */
    val claims: List<CoinClaim>,
    /**
     * How many pieces of this type no collection claims, which is **not** always zero when
     * [claims] is not empty.
     *
     * Measured on the father's collection: the American Silver Eagle N#298883 is two rows, issues
     * 760576 and 1059386, and its catalog qualifies members by issue (ADR 0019). One row fills a
     * member and the other is unclassified residue — so the type is in a collection and one of his
     * two coins is not. Counting membership off [claims] alone made that second coin invisible in
     * the one place ADR 0021 §12 leaves for it, which is the whole job of the «Sin colección» chip.
     */
    val unclaimedPieces: Int,
    /**
     * The years of the axis that lead here without being printed on the cartouche (#550).
     *
     * Two readings, and both are seats the collector can press on the year axis:
     *
     * - **Where the axis places its pieces.** The axis places by `placementYear` — Gregorian first —
     *   and the card is dated by the engraved year (ADR 0016), so the 100 pesetas «*67» is painted on
     *   1967 and says 1966. Measured on the father's collection, nine pieces differ that way and four
     *   years painted a coin whose own seat opened an empty page.
     * - **The years of the casillas of its evidenced plates.** A hole of a date run is a year the
     *   collector owns nothing of, so nothing but this makes the ghost of 1901 open the 2 bolívares
     *   the collector does hold.
     *
     * What is deliberately **not** here is the ficha's `minYear`–`maxYear` run: the Maria Theresa
     * Thaler is a posthumous restrike filed 1780–2024, and answering to its 245 years would put it
     * under almost every seat of the axis. What answers is the curated knowledge, not Numista's
     * min/max.
     */
    val axisYears: List<Int> = emptyList(),
) {
    val title: String get() = name.text

    /**
     * The oldest year this coin is placed by, and the newest — what «Más antiguas» and «Más nuevas»
     * sort on. Null on a row with no year at all, which both orders put last.
     */
    val oldestYear: Int? get() = years.firstOrNull()
    val newestYear: Int? get() = years.lastOrNull()

    /**
     * Which year chips this row answers to: one per year it holds, or «Sin año» (#448), plus every
     * [axisYears] seat that leads here (#550).
     *
     * The axis years **join** «Sin año» rather than replacing it: a piece with no date is still a
     * piece with no date, whichever seat of the calendar its plate puts it on.
     */
    val yearFilters: List<YearFilter> = YearFilter.of(years) + axisYears.map(YearFilter::Of)

    /**
     * What the search box compares against: everything printed on the row, folded once.
     *
     * **Every** year and not just the printed arc: a row that spans 1879 to 1936 is found by typing
     * 1904, the same as tapping the 1904 chip finds it — [axisYears] included, because the promise
     * is parity with the chips and the chips come from [yearFilters].
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
 * Every coin the collector owns, each with the collections that claim it.
 *
 * The list is built from [CollectionState] alone, like everything else on screen: Coins is the other
 * hierarchy (ADR 0021 §1), not a second store — no table was added for it, and a coin appears here
 * whether or not any collection claims it.
 */
fun coinRows(state: CollectionState, slots: SlotYears = SlotYears.none): List<CoinRow> {
    val claimed = claimsOf(state)
    val byType = LinkedHashMap<Int, MutableList<CollectedItem>>()
    // Same coerce as [collectionFigures]: a hostile zero is still one piece, so the bottom bar's
    // type count and the rows Coins draws cannot drift (#426).
    for (item in state.items) {
        byType.getOrPut(item.typeId) { mutableListOf() }.add(item)
    }
    return byType
        .map { (typeId, pieces) -> coinRow(state, typeId, pieces, claimed, slots) }
        .sortedWith(coinReadingOrder())
}

/**
 * The row of one type read on its own, **whether or not a piece of it is in the collection** (#508).
 *
 * The casilla of a lámina opens the coin's sheet inside the app now, and half of the casillas are
 * holes: the type is real, its ficha is on the phone — the packaged cache holds one for every one of
 * the 1.172 curated members — and the collector owns nothing of it. Such a row is **not** part of
 * [coinRows] and never reaches the shelf: it is one coin read for its own sheet, and `quantity` 0 with
 * an empty `claims` says exactly what it is, no piece and no collection.
 *
 * It goes through the same [coinRow] as the rows of the grid, which is the whole point of extracting
 * it: the sheet of a coin the collector holds must not word one thing when it is opened from Monedas
 * and another when it is opened from a casilla.
 */
fun coinRowOf(state: CollectionState, typeId: Int): CoinRow = coinRow(
    state = state,
    typeId = typeId,
    pieces = state.items.filter { it.typeId == typeId },
    claimed = claimsOf(state),
    // A sheet has no year chips to answer: [CoinRow.axisYears] exists for the shelf, and the sheet
    // opened from a casilla is one coin already found.
    slots = SlotYears.none,
)

private fun coinRow(
    state: CollectionState,
    typeId: Int,
    pieces: List<CollectedItem>,
    claimed: Claims,
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
        // A row with no piece has no coin to be dated by, so it says what the **type** says and both
        // ends of it: see [typeYears]. Nothing changes for a row that does hold pieces.
        years = years,
        objectClass = objectClassOf(meta?.category),
        weightOz = meta?.weightOz,
        quantity = pieces.fold(0) { total, piece ->
            saturatingAdd(total, piece.quantity.coerceAtLeast(1))
        },
        claims = claimed.byType[typeId].orEmpty(),
        unclaimedPieces = pieces.count { it.id !in claimed.rowIds },
        axisYears = axisYearsOf(pieces, meta, slots.of(typeId), printed = years),
    )
}

/**
 * The seats of the axis that lead to this coin without being printed on it — see [CoinRow.axisYears].
 *
 * The years already on the cartouche are dropped rather than repeated: they are the same chip, and a
 * row answering twice to 1936 would count itself twice on the facet that promises its number is what
 * the tap gives.
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
 * What a type is called when no piece of it names it: the ficha's title, and its Numista number when
 * the phone has no ficha either.
 *
 * The number is the last resort and not an apology: every curated member has a ficha in the packaged
 * cache, so this reaches the screen only on a type nobody has ever read — and «N# 10338» is still the
 * one name such a coin has.
 */
private fun typeTitle(meta: TypeMeta?, typeId: Int): String =
    meta?.title ?: meta?.displayTitle ?: numistaCodeLabel(typeId)

/**
 * The years the **type** covers, for a row the collector holds no piece of (#508).
 *
 * Both ends and not `minYear` alone, which is the very reading #448 took out of the cards of Monedas:
 * the year a type opens is the year of a coin only where the type has one year. With no piece there is
 * no coin's own year to prefer, and a date run's sheet saying «1879 – 1936» is true of the type where
 * «1879» would be a wrong answer about the casilla the collector just pressed.
 */
private fun typeYears(meta: TypeMeta?): List<Int> =
    listOfNotNull(meta?.minYear, meta?.maxYear).filter { it > 0 }.distinct().sorted()

/**
 * The years of a coin, as the card says them (#448).
 *
 * One year when the collector's pieces agree on one, the arc between the ends when they do not, and
 * «Sin año» when there is nothing to say. The arc and not the list: twenty-one years of a Venezuelan
 * 5 bolívares would be a paragraph in a cartouche that has one line, and the year the collector is
 * after is one chip away.
 */
fun coinYearsLabel(years: List<Int>): String = when (years.size) {
    0 -> UNKNOWN_YEAR_LABEL
    1 -> years.single().toString()
    else -> "${years.first()} – ${years.last()}"
}

/**
 * The years of the pieces of one type, oldest first, falling back on the ficha and then on nothing.
 *
 * [CollectedItem.recordedYear] and not the Gregorian reading, which is the same choice `pieceLine`
 * makes: what the card says is what is engraved on the coin, so a Moroccan dirham of 1316 says 1316
 * (ADR 0016) on both screens rather than 1899 on one of them.
 */
private fun yearsOf(pieces: List<CollectedItem>, meta: TypeMeta?): List<Int> =
    pieces.mapNotNull { it.recordedYear }
        // **Zero is not a year**, the same rule `placementYear` states: Numista stores `0` on an
        // undated medal, and the father has two. Printing «0» on their cartouche would be worse than
        // the «Sin año» it replaced — a wrong answer reads as an answer (#460).
        .filter { it > 0 }
        .distinct()
        .sorted()
        .ifEmpty { listOfNotNull(meta?.minYear) }

/** The only identity left in the grid below the two-range name: year and a non-singular count. */
fun coinAlbumFootnote(row: CoinRow): String = listOfNotNull(
    coinYearsLabel(row.years),
    "×${row.quantity}".takeIf { row.quantity > 1 },
).joinToString(" · ")

/**
 * Who claims what, read once off the index.
 *
 * Two answers and not one, because they are asked at different grains: [byType] is what a coin links
 * back to, and a type may be claimed by more than one collection (ADR 0021 §10). [rowIds] is which
 * **pieces** were actually placed, which is finer — an issue-qualified catalog can claim one row of a
 * type and leave its sibling in the residue (ADR 0019).
 */
internal class Claims(
    val byType: Map<Int, List<CoinClaim>>,
    val rowIds: Set<Long>,
)

/**
 * Built by walking the index rather than the catalogs, so the links arrive already in the one order
 * of the first level and a card the inventory no longer derives cannot be linked to.
 */
internal fun claimsOf(state: CollectionState): Claims {
    val byType = LinkedHashMap<Int, MutableList<CoinClaim>>()
    val rowIds = mutableSetOf<Long>()
    for (card in state.index) {
        val pieces = when (card) {
            is IndexCard.Derived -> state.itemsByKey[card.key].orEmpty()
            is IndexCard.Box -> card.box.items
        }
        val claim = CoinClaim(card.name, destinationOf(card))
        pieces.forEach { piece -> rowIds += piece.id }
        for (typeId in pieces.mapTo(LinkedHashSet()) { it.typeId }) {
            byType.getOrPut(typeId) { mutableListOf() }.add(claim)
        }
    }
    return Claims(byType, rowIds)
}

/**
 * The order coins are read in: country, then year, then title.
 *
 * A field notebook's order and not the index's: Coins has no ratio to sort by, and the collector
 * arriving here is looking for a coin they can picture rather than for progress. Unknowns go last in
 * both slots — an uncached type says less than a dated one, and putting it first would open the list
 * on whatever the last sync had not finished.
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
 * Spanish alphabetical order on the coin's own title, for the «Alfabético» sort.
 *
 * The same [Collator] the index's names go through and for the same reason: comparing the strings
 * themselves orders by UTF-16 code unit, where «Álbum» lands after «Zeta».
 */
internal fun coinTitleOrder(): Comparator<CoinRow> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareBy(collator) { it.title }
}
