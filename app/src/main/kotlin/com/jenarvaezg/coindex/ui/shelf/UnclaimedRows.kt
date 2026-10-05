package com.jenarvaezg.coindex.ui.shelf

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CoinClaims
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.SeriesStatus
import com.jenarvaezg.coindex.ui.fold
import com.jenarvaezg.coindex.ui.matchesQuery
import com.jenarvaezg.coindex.ui.pieceTitle
import java.text.Collator
import java.util.Locale

/**
 * One inventory row no collection claims, reduced to what the shelf asks about (#275); the subject
 * of the notebook's loose-coin lámina. A row, not a type like [CoinRow]: claiming is decided per
 * row (a catalog qualifying by issue, ADR 0019, may claim one row of a type and not another), and
 * the lámina prints one cell per row.
 */
data class UnclaimedFacts(
    val piece: CollectedItem,
    override val countries: Set<String>,
    override val weight: OunceBand?,
    override val startsIn: StartBand,
    /** The row's recorded year, else the type's first. */
    val year: Int?,
    val title: String,
    val haystack: String,
) : ShelfSubject {
    /** A loose coin has no plate. */
    override val status: PlateStatus get() = PlateStatus.NoPlate

    /** No catalog, so no series. */
    override val series: SeriesStatus? get() = null

    /** The coin's country, for reading order. */
    val issuer: String? get() = countries.singleOrNull() ?: countries.firstOrNull()
}

/**
 * Every inventory row no index card claims, in reading order.
 *
 * Measured against the whole index, never one export (#275), using the assembly's [CoinClaims] like
 * the «Sin colección» chip (ADR 0021 §12), so the app and the notebook agree and a filter can't
 * make a boxed coin loose.
 *
 * Not the domain's `unclassified` residue: a box claims rows by type, so a leftover piece may
 * already print on its box's lámina. This is the complement of what the notebook prints.
 */
fun unclaimedFacts(state: CollectionState): List<UnclaimedFacts> {
    val claims = state.claims
    return state.items
        .filter { piece -> piece.quantity > 0 && !claims.claimed(piece) }
        .map { piece ->
            val meta = state.typeMeta[piece.typeId]
            val title = pieceTitle(state, piece)
            val year = piece.recordedYear ?: meta?.minYear
            UnclaimedFacts(
                piece = piece,
                // The cured country, not Numista's issuer name with its period (ADR 0023).
                countries = setOfNotNull(meta?.country),
                // Numista's grams snapped to bullion weights, the same type property the card key
                // uses (#288, #540). Null without a weight, so it matches no weight filter rather
                // than «Varias onzas».
                weight = meta?.weightMillioz?.let { millioz -> OunceBand.of(millioz) },
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
 * The loose pieces this shelf and query leave, in [unclaimedFacts] order. Uses the same `matches`
 * as cards, so the loose-coin lámina honours the chips like the rest of the export.
 *
 * The sort is not applied: [IndexSort] orders collections and means nothing for single coins.
 */
fun IndexShelf.narrowUnclaimed(
    facts: List<UnclaimedFacts>,
    query: String,
): List<CollectedItem> = facts
    .filter { matches(it) && matchesQuery(it.haystack, query) }
    .map { it.piece }

/**
 * Country, then year, then title, as in Monedas. Unknowns go last, as in [coinRows].
 */
private fun unclaimedReadingOrder(): Comparator<UnclaimedFacts> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es"))
    return compareBy<UnclaimedFacts> { it.issuer == null }
        .thenBy(collator) { it.issuer.orEmpty() }
        .thenBy { it.year ?: Int.MAX_VALUE }
        .thenBy(collator) { it.title }
        .thenBy { it.piece.id }
}
