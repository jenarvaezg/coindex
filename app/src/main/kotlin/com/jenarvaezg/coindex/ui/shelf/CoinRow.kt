package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.IndexCard
import com.jenarvaezg.coindex.domain.ObjectClass
import com.jenarvaezg.coindex.domain.TypeMeta
import com.jenarvaezg.coindex.domain.objectClassOf
import com.jenarvaezg.coindex.domain.saturatingAdd
import com.jenarvaezg.coindex.ui.CardDestination
import com.jenarvaezg.coindex.ui.destinationOf
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.pieceTitle
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
    val title: String,
    /**
     * The country, in Spanish, unsaid when the type is uncached.
     *
     * [TypeMeta.country] and not the ficha's `issuer.name`, because Numista names issuing entities
     * with their period of validity: the country of a `russie` type is «Rusia», and «Federación de
     * Rusia (1991-presente)» took a chip row to itself in the shelf below (ADR 0023).
     */
    val issuer: String?,
    /** The type's first year, which for these collections is the year on the coin. */
    val year: Int?,
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
    /** What the search box compares against: everything printed on the row, folded once. */
    val haystack: String = fold(
        listOfNotNull(title, issuer, year?.toString(), typeId.toString())
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
    for (item in state.items.filter { it.quantity > 0 }) {
        byType.getOrPut(item.typeId) { mutableListOf() }.add(item)
    }
    return byType.map { (typeId, pieces) ->
        val meta = state.typeMeta[typeId]
        CoinRow(
            typeId = typeId,
            title = pieceTitle(state, pieces.first()),
            issuer = meta?.country,
            year = meta?.minYear,
            objectClass = objectClassOf(meta?.category),
            weightOz = meta?.weightOz,
            quantity = pieces.fold(0) { total, piece -> saturatingAdd(total, piece.quantity) },
            claims = claimed.byType[typeId].orEmpty(),
            unclaimedPieces = pieces.count { it.id !in claimed.rowIds },
        )
    }.sortedWith(coinReadingOrder())
}

/** How many Numista types the collector owns, which is how many rows [coinRows] draws. */
fun ownedTypeCount(state: CollectionState): Int =
    state.items.filter { it.quantity > 0 }.distinctBy { it.typeId }.size

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
        .thenBy { it.year ?: Int.MAX_VALUE }
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
