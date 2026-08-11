package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.CoinName
import com.jenarvaezg.coindex.ui.UNKNOWN_YEAR_LABEL
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.pieceName
import com.jenarvaezg.coindex.ui.pieceRawTitle
import java.text.Collator
import java.util.Locale

/** One collection that claims a coin, as the link back to it (ADR 0021 §1). */
data class CoinClaim(val name: String, val destination: CardDestination)

/**
 * One coin of Coins: a Numista **type** and every piece of it the collector owns.
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
) {
    val title: String get() = name.text

    /**
     * The oldest year this coin is placed by, and the newest — what «Más antiguas» and «Más nuevas»
     * sort on. Null on a row with no year at all, which both orders put last.
     */
    val oldestYear: Int? get() = years.firstOrNull()
    val newestYear: Int? get() = years.lastOrNull()

    /** Which year chips this row answers to: one per year it holds, or «Sin año» (#448). */
    val yearFilters: List<YearFilter> = YearFilter.of(years)

    /**
     * What the search box compares against: everything printed on the row, folded once.
     *
     * **Every** year and not just the printed arc: a row that spans 1879 to 1936 is found by typing
     * 1904, the same as tapping the 1904 chip finds it.
     */
    val haystack: String = fold(
        listOf(rawTitle)
            .plus(listOfNotNull(issuer))
            .plus(years.map(Int::toString))
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
fun coinRows(state: CollectionState): List<CoinRow> {
    val claimed = claimsOf(state)
    val byType = LinkedHashMap<Int, MutableList<CollectedItem>>()
    // Same coerce as [collectionFigures]: a hostile zero is still one piece, so the bottom bar's
    // type count and the rows Coins draws cannot drift (#426).
    for (item in state.items) {
        byType.getOrPut(item.typeId) { mutableListOf() }.add(item)
    }
    return byType.map { (typeId, pieces) ->
        val meta = state.typeMeta[typeId]
        CoinRow(
            typeId = typeId,
            name = pieceName(state, pieces.first()),
            rawTitle = pieceRawTitle(state, pieces.first()),
            issuer = meta?.country,
            years = yearsOf(pieces, meta),
            objectClass = objectClassOf(meta?.category),
            weightOz = meta?.weightOz,
            quantity = pieces.fold(0) { total, piece ->
                saturatingAdd(total, piece.quantity.coerceAtLeast(1))
            },
            claims = claimed.byType[typeId].orEmpty(),
            unclaimedPieces = pieces.count { it.id !in claimed.rowIds },
        )
    }.sortedWith(coinReadingOrder())
}

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
