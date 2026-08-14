package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.holesWithinReach
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.CollectionFigures
import com.jenarvaezg.coindex.domain.CollectionValue
import com.jenarvaezg.coindex.domain.Ladder
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.LadderPlacement
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.PaidComparison
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.collectionFigures
import com.jenarvaezg.coindex.domain.collectionValue
import com.jenarvaezg.coindex.domain.fineSilverGrams
import com.jenarvaezg.coindex.domain.holeValue
import com.jenarvaezg.coindex.domain.paidComparison
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

/**
 * The amount, where it came from, and the day the silver behind it was read.
 *
 * @param paid what he paid for the pieces he wrote a price for, against what those same pieces are
 *   worth today. It hangs here and not off [FiguresSubject] because two amounts in euros are money as
 *   much as the total is: inside the reading there is no branch that could let it out with the export's
 *   switch off. Null when no row declares a price.
 */
data class MoneyReading(
    val value: CollectionValue,
    val spot: SilverSpot,
    val paid: PaidComparison?,
)

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
            paidComparison(state.items, state.typeMeta, spot, prices),
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
 * What one coin is worth, for its ficha.
 *
 * The same three sources the page totals, read for one type. **The grain is the whole argument**
 * (ADR 0026 §10): read piece by piece or plate by plate this is a shopping companion — the premium is
 * the scale of that purchase — and the same thing totalled for the shelf is wealth management, which
 * stays outside.
 *
 * @param pieces how many pieces of the type the total covers, so «×3» is not read as one coin's price.
 * @param source and [grade] only when **every** piece agrees on them. Two pieces of one type graded
 *   differently are two origins, and one of them printed for both would be the wrong one half the time.
 */
data class CoinValue(
    val eur: Double,
    val pieces: Int,
    val source: ValueSource?,
    val grade: String?,
)

fun coinValue(
    typeId: Int,
    state: CollectionState,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): CoinValue? {
    val meta = state.typeMeta[typeId]
    val valued = state.items
        .filter { it.typeId == typeId }
        .mapNotNull { item ->
            pieceValue(item, meta, spot, prices)?.let { it to item.quantity.coerceAtLeast(1) }
        }
    if (valued.isEmpty()) return null
    val sources = valued.map { (value, _) -> value.source to value.grade }.distinct()
    val agreed = sources.singleOrNull()
    return CoinValue(
        eur = valued.sumOf { (value, quantity) -> value.eur * quantity },
        pieces = valued.fold(0) { total, (_, quantity) -> saturatingAdd(total, quantity) },
        source = agreed?.first,
        grade = agreed?.second,
    )
}

/** What a plate's own coins are worth, for the figure over its title. */
data class PlateValue(val eur: Double, val pieces: Int)

/**
 * What closing a plate would cost, for the second figure of the same header (#493).
 *
 * @param holes how many empty casillas the amount covers, which is **not** how many the plate has: a
 *   hole whose issue nobody knows, or whose price is not on the phone, adds nothing to the total and
 *   is not counted by it. The figure is therefore a floor, and the plate says a cost of closing built
 *   out of the holes it can price rather than nothing at all.
 */
data class PlateCost(val eur: Double, val holes: Int)

/**
 * Everything a plate's header says about money, and the price inside each of its empty casillas.
 *
 * The three readings arrive together because they are one walk of the same album and because they are
 * answered by one gate: while the market has not landed there is no value, no cost and no stamp
 * either (ADR 0028 §7), and a drawer holding this empty cannot print any of the three.
 *
 * @param holeCosts what one empty casilla costs, keyed by member id, for the stamp drawn inside it.
 *   Empty for a plate over the threshold of ADR 0028 §1 — the pass never asked for those prices, so
 *   there is nothing to stamp — and empty for a plate that is closed, which has no casilla to stamp.
 */
data class PlateMoney(
    val value: PlateValue? = null,
    val cost: PlateCost? = null,
    val holeCosts: Map<String, Double> = emptyMap(),
)

/**
 * The plate's money, assembled once for the header and the casillas that share it.
 *
 * @param listings which issue each empty casilla stands for, as the pass stored it (#452). Without it
 *   only the ten holes of a hundred and twenty-one whose curated file names its issues could be
 *   priced at all.
 */
fun plateMoney(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    listings: IssueListings,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PlateMoney {
    val holeCosts = holeCosts(album, state, listings, spot, prices)
    return PlateMoney(
        value = plateValue(album, state, spot, prices),
        // Null and not zero, like every other amount on the page: a plate whose holes are all
        // unpriceable has nothing to say about closing, and «0 €» would say it costs nothing.
        cost = holeCosts.values
            .takeIf { it.isNotEmpty() }
            ?.let { PlateCost(it.sum(), it.size) },
        holeCosts = holeCosts,
    )
}

/**
 * What each empty casilla of a plate costs, or nothing at all when the plate is out of reach.
 *
 * Which holes count is [holesWithinReach]'s answer and not counted again here, so the holes the header
 * adds up are exactly the holes the pass spent its calls on (ADR 0028 §1).
 *
 * A casilla with no price is **absent** rather than zero: without a price on the phone there is no
 * stamp to draw and nothing to add, and neither the amount nor the casilla invents a «—».
 */
private fun holeCosts(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    listings: IssueListings,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): Map<String, Double> =
    holesWithinReach(album).mapNotNull { hole ->
        val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
        val cost = holeValue(
            typeId = typeId,
            issueId = listings.issueOf(hole.member),
            meta = state.typeMeta[typeId],
            spot = spot,
            prices = prices,
        ) ?: return@mapNotNull null
        hole.member.id to cost.eur
    }.toMap()

/**
 * The value of what a plate holds, which is the other place the grain rule allows a total.
 *
 * Only the pieces the plate's own casillas are filled with, so it is the plate's value and not the
 * value of every coin of those types: a type that fills one casilla and sits loose in three more rows is
 * one casilla here.
 *
 * **The cost of closing it is the other figure and not this one** (#493). They share a header and
 * nothing else: this one is a total over what is there, at the maximum of three prices, and the cost
 * is a total over what is not, out of two prices in `unc` — which is why each of them reaches the
 * screen with its own provenance beside it instead of sharing one line of criterion.
 */
fun plateValue(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PlateValue? {
    val filled = album.members
        .mapNotNull { it.status as? CollectionCatalogMemberStatus.Owned }
        .flatMap { owned -> owned.items }
        .map { it.itemId }
        .toSet()
    if (filled.isEmpty()) return null
    var total = 0.0
    var pieces = 0
    for (item in state.items.filter { it.id in filled }) {
        val value = pieceValue(item, state.typeMeta[item.typeId], spot, prices) ?: continue
        val quantity = item.quantity.coerceAtLeast(1)
        total += value.eur * quantity
        pieces = saturatingAdd(pieces, quantity)
    }
    return if (pieces == 0) null else PlateValue(total, pieces)
}
