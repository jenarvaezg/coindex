package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionFigures
import com.jenarvaezg.coindex.domain.CollectionValue
import com.jenarvaezg.coindex.domain.Ladder
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.LadderPlacement
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.collectionFigures
import com.jenarvaezg.coindex.domain.collectionValue
import com.jenarvaezg.coindex.domain.fineSilverGrams
import com.jenarvaezg.coindex.domain.pieceValue
import com.jenarvaezg.coindex.domain.place
import com.jenarvaezg.coindex.domain.saturatingAdd

/**
 * What one ladder reads for this collection.
 *
 * @param amount in the ladder's own unit, which is what the label prints.
 * @param approximate the stack and only the stack: `thickness` is missing in a third of the types, so it
 *   is measured over the pieces that have one and scaled to all of them, and the word «unos» is the whole
 *   of that declaration.
 */
data class LadderReading(
    val ladder: Ladder,
    val amount: Double,
    val placement: LadderPlacement,
    val approximate: Boolean,
)

/**
 * The country that portrays the collection, in the three or four numbers that say it at once.
 *
 * «Venezuela es el 62 % de tus piezas, el 33 % de su peso y el 33 % de su plata» — the three together
 * say what none of them says alone: that they are a lot of small coins. The fourth, the share of the
 * **value**, is money and comes and goes with it.
 */
data class CountryPortrait(
    val country: String,
    val pieces: Int,
    val pieceShare: Double,
    val massShare: Double,
    val silverShare: Double,
    /** Null while the market has not landed, and null on paper with money switched off. */
    val valueShare: Double?,
)

/** The amount, where it came from, and the day the silver behind it was read. */
data class MoneyReading(val value: CollectionValue, val spot: SilverSpot)

/**
 * Everything «Las cifras» draws, assembled once.
 *
 * @param money **absent, and not zero, while the market is still arriving** (ADR 0028 §7). Without it
 *   the value is `max(silver, paid)`, some 60 % of the real figure, which is the «only the silver floor»
 *   #316 rejected: a total at 60 % is not incomplete, it is false. Every other field of this type comes
 *   out of the APK and is there on a phone that has never called Numista.
 */
data class FiguresSubject(
    val figures: CollectionFigures,
    val money: MoneyReading?,
    val ladders: List<LadderReading>,
    val portrait: CountryPortrait?,
)

/**
 * Assembles the page from what the phone holds.
 *
 * @param moneyAllowed the export's money switch, and **only** the export's (#228, ADR 0021 §13). On
 *   screen it is always true: there is nothing to switch off on a page the collector opened on purpose.
 *   Off, it withdraws the amount **and every figure derived from one** — «Venezuela · 30 % del valor» is
 *   money as much as the total is, and the prototype let it through with the money off.
 * @param settled whether the valuation pass has finished. False leaves [FiguresSubject.money] absent
 *   however many prices happen to be on the phone.
 */
fun figuresSubject(
    state: CollectionState,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
    settled: Boolean,
    moneyAllowed: Boolean = true,
): FiguresSubject {
    val figures = collectionFigures(state.items, state.typeMeta)
    val money = if (!moneyAllowed || !settled || spot == null) {
        null
    } else {
        MoneyReading(
            collectionValue(state.items, state.typeMeta, spot, prices),
            spot,
        )
    }
    return FiguresSubject(
        figures = figures,
        money = money,
        ladders = listOf(
            reading(Ladders.weight, figures.weight.value / 1_000.0, approximate = false),
            reading(Ladders.row, figures.row.value, approximate = false),
            // The one figure of the page that is scaled rather than measured; a collection with no
            // thickness at all reads zero and sits at the foot of the ladder, which is honest.
            reading(
                Ladders.stack,
                figures.stack.extrapolated ?: 0.0,
                approximate = !figures.stack.complete,
            ),
        ),
        portrait = portrait(state, figures, money, spot, prices),
    )
}

private fun reading(ladder: Ladder, amount: Double, approximate: Boolean) = LadderReading(
    ladder = ladder,
    amount = amount,
    placement = ladder.place(amount),
    approximate = approximate,
)

/** Which ladder a reading is, for a screen that lays the three of them out differently. */
fun LadderReading.isStack(): Boolean = ladder.kind == LadderKind.Stack

/**
 * The country with the most pieces, and its share of the four things.
 *
 * One country and not a ranking: this is the collection's portrait and not a breakdown, and a table of
 * 34 issuers is the dashboard this page is not. The shelf of Coins is where a country is looked *up*;
 * this is where one is *noticed*, which is why it leads there.
 */
private fun portrait(
    state: CollectionState,
    figures: CollectionFigures,
    money: MoneyReading?,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): CountryPortrait? {
    if (figures.pieces <= 0) return null
    val byCountry = mutableMapOf<String, MutableCountry>()
    for (item in state.items) {
        val meta = state.typeMeta[item.typeId] ?: continue
        val country = meta.country ?: continue
        val quantity = item.quantity.coerceAtLeast(1)
        val tally = byCountry.getOrPut(country) { MutableCountry() }
        tally.pieces = saturatingAdd(tally.pieces, quantity)
        meta.weightGrams?.let { tally.grams += it * quantity }
        fineSilverGrams(meta)?.let { tally.silver += it * quantity }
        if (money != null) {
            pieceValue(item, meta, spot, prices)?.let { tally.value += it.eur * quantity }
        }
    }
    val (country, tally) = byCountry.entries
        .sortedWith(compareByDescending<Map.Entry<String, MutableCountry>> { it.value.pieces }
            .thenBy { it.key })
        .firstOrNull()
        ?.let { it.key to it.value }
        ?: return null
    return CountryPortrait(
        country = country,
        pieces = tally.pieces,
        pieceShare = tally.pieces.toDouble() / figures.pieces,
        massShare = share(tally.grams, figures.weight.value),
        silverShare = share(tally.silver, figures.fineSilver.value),
        valueShare = money?.let { share(tally.value, it.value.eur) },
    )
}

private fun share(part: Double, whole: Double): Double = if (whole <= 0.0) 0.0 else part / whole

private class MutableCountry {
    var pieces: Int = 0
    var grams: Double = 0.0
    var silver: Double = 0.0
    var value: Double = 0.0
}

/**
 * What one piece is worth, for the ficha and the plate header.
 *
 * The same three sources the page totals, read for one row: the grain is what tells a shopping companion
 * from wealth management (ADR 0026 §10), and per piece it is the companion.
 */
fun pieceReading(
    item: CollectedItem,
    state: CollectionState,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
) = pieceValue(item, state.typeMeta[item.typeId], spot, prices)
