package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.domain.normalizeWeightMillioz
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery
import com.jenarvaezg.coindex.ui.pieceTitle
import java.text.Collator
import java.util.Locale

/**
 * One inventory row no collection claims, reduced to what the shelf asks about (#275).
 *
 * The subject of the notebook's last lámina. It is a **row and not a type**, unlike [CoinRow]: the
 * lámina prints a cell per row like every other pieces page of the notebook, and the row is also the
 * grain at which being claimed is decided — the American Silver Eagle N#298883 is two rows, one
 * filling a member its catalog qualifies by issue (ADR 0019) and one left over, and printing the
 * type would print a coin that is already in its plate.
 */
data class UnclaimedFacts(
    val piece: CollectedItem,
    override val countries: Set<String>,
    override val weight: OunceBand?,
    override val startsIn: StartBand,
    /** The year to read it by: the row's own where it recorded one, else the type's first. */
    val year: Int?,
    val title: String,
    val haystack: String,
) : ShelfSubject {
    /** A loose coin has no issue list and therefore no ratio: «Sin lámina» is literally true. */
    override val status: PlateStatus get() = PlateStatus.NoPlate

    /** No catalog names it, so no series does — the same silence a card with no catalog keeps. */
    override val series: SeriesStatus? get() = null

    /** The single country a loose coin has, when it has one — for reading order. */
    val issuer: String? get() = countries.singleOrNull() ?: countries.firstOrNull()
}

/**
 * Every row of the inventory that no card of the index claims, in reading order.
 *
 * **Measured against the whole index and never against one export** (#275). Having a collection is a
 * fact about the coin, not about the paper, so this is the same [claimsOf] the «Sin colección» chip
 * of Coins is counted with (ADR 0021 §12) — the app and the notebook cannot disagree about which
 * coins are loose, and a filter the collector put on today cannot orphan a coin that lives in a box.
 *
 * It is **not** the domain's `unclassified` residue, and the two differ exactly at the collector's
 * own boxes: a box claims rows by type, so a piece the derivation left over may already be printed
 * on its box's lámina. What this asks is the question the notebook needs — which coins does no page
 * show? — and the answer is the complement of what gets printed, with nothing repeated.
 */
fun unclaimedFacts(state: CollectionState): List<UnclaimedFacts> {
    val claimed = claimsOf(state).rowIds
    return state.items
        .filter { piece -> piece.quantity > 0 && piece.id !in claimed }
        .map { piece ->
            val meta = state.typeMeta[piece.typeId]
            val title = pieceTitle(state, piece)
            val year = piece.recordedYear ?: meta?.minYear
            UnclaimedFacts(
                piece = piece,
                // The same country the card of a collection with no curated file shows: Numista's
                // issuing entity carries its period of validity, and `country` is «Rusia» where the
                // raw name is «Federación de Rusia (1991-presente)» (ADR 0023).
                countries = setOfNotNull(meta?.country),
                // Numista's own grams, snapped to the common bullion weights: a loose coin is by
                // definition one no curated file has a weight for. This row used to be the only
                // place that read it that way, and the same coin weighed one thing here and
                // another in its card's key; since #288 there is one reading. Null where the ficha
                // declares none, which keeps it out of every weight filter instead of parking it
                // under «Varias onzas».
                weight = meta?.weightOz
                    ?.let { ounces -> normalizeWeightMillioz(ounces) }
                    ?.let { millioz -> OunceBand.of(millioz) },
                startsIn = StartBand.of(year),
                year = year,
                title = title,
                haystack = fold(
                    listOfNotNull(title, meta?.country, year?.toString(), piece.typeId.toString())
                        .joinToString(" "),
                ),
            )
        }
        .sortedWith(unclaimedReadingOrder())
}

/**
 * The loose pieces this shelf and this query leave, in the order [unclaimedFacts] put them in.
 *
 * The twin of [narrow], and deliberately not a mechanism of its own: the export sheet promises «lo
 * que hay en el índice ahora mismo, con los filtros puestos», and a lámina that ignored the chips
 * above it would make that sentence false. The five chips are answered by [UnclaimedFacts] as a card
 * of one piece with no plate, so `matches` is used exactly as it is used for a card.
 *
 * The sort is **not** applied: [IndexSort] orders collections by ratio, weight and count, and none
 * of the six means anything about a single coin. Coins are read by country, year and title.
 */
fun IndexShelf.narrowUnclaimed(
    facts: List<UnclaimedFacts>,
    query: String,
): List<CollectedItem> = facts
    .filter { matches(it) && matchesQuery(it.haystack, query) }
    .map { it.piece }

/**
 * Country, then year, then title: the field-notebook order of Coins and not the index's.
 *
 * This lámina is the overflow of Coins, and thirty coins from twenty countries are read grouped by
 * country. Unknowns go last in both slots, exactly as in [coinRows]: an uncached type says less than
 * a dated one, and putting it first would open the page on whatever the last sync had not finished.
 */
private fun unclaimedReadingOrder(): Comparator<UnclaimedFacts> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareBy<UnclaimedFacts> { it.issuer == null }
        .thenBy(collator) { it.issuer.orEmpty() }
        .thenBy { it.year ?: Int.MAX_VALUE }
        .thenBy(collator) { it.title }
        .thenBy { it.piece.id }
}
