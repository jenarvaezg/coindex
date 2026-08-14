package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.IssueListings
import com.jenarvaezg.coindex.data.prices.holesWithinReach
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.CollectionFigures
import com.jenarvaezg.coindex.domain.CollectionValue
import com.jenarvaezg.coindex.domain.Ladder
import com.jenarvaezg.coindex.domain.LadderKind
import com.jenarvaezg.coindex.domain.LadderPlacement
import com.jenarvaezg.coindex.domain.Ladders
import com.jenarvaezg.coindex.domain.PaidComparison
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.SilverSpot
import com.jenarvaezg.coindex.domain.ValueSource
import com.jenarvaezg.coindex.domain.WishKey
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.wishKey
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
    /**
     * What entering costs, on a plate that is not the collector's (ADR 0030 §6).
     *
     * The third figure and never a second reading of the second: this one is a **whole** plate priced
     * because no casilla of it reproaches anything (§7), and it carries the date the amount was brought
     * because nothing will ever refresh it (§4). A plate of the collector's leaves it null, and one of
     * the shelf window leaves [value] and [cost] null: they are two régimes and not two styles.
     */
    val entry: ShowcaseCost? = null,
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
    /**
     * The casillas of this album the collector marked, which carry a price whatever the plate's shape
     * (ADR 0029 §4).
     *
     * The pass asked for them, so they are on the phone, so the stamp is drawn — that is the rule #493
     * wrote for every hole and it does not change here. What does **not** change either is the second
     * figure of the header: see [PlateCost].
     */
    wished: Set<WishKey> = emptySet(),
): PlateMoney {
    // The one walk of the album's holes, handed on rather than repeated: the header's second figure and
    // the chips inside the casillas have to be about the same holes (ADR 0028 §1).
    val withinReach = holesWithinReach(album)
    val closing = withinReach.mapTo(mutableSetOf()) { it.member.id }
    val holeCosts = holeCosts(album, state, listings, spot, prices, wished, withinReach)
    return PlateMoney(
        value = plateValue(album, state, spot, prices),
        // **Only the holes within reach**, and a marked one past the threshold is deliberately not
        // added: one hole is not the cost of closing a plate of fifty-one, and adding it would print
        // «Coste de cerrar» over a number that closes nothing (ADR 0029 §4). Null and not zero, like
        // every other amount on the page: «0 €» would say closing costs nothing.
        cost = holeCosts
            .filterKeys { it in closing }
            .values
            .takeIf { it.isNotEmpty() }
            ?.let { PlateCost(it.sum(), it.size) },
        holeCosts = holeCosts,
    )
}

/**
 * What each empty casilla of a plate costs, or nothing at all when the plate is out of reach.
 *
 * Which holes count is [holesWithinReach]'s answer plus whatever the collector marked, and neither is
 * counted again here: what the header adds up and what the pass spent its calls on are the same holes
 * (ADR 0028 §1, ADR 0029 §4).
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
    wished: Set<WishKey>,
    withinReach: List<CollectionCatalogAlbumMember>,
): Map<String, Double> =
    holesToPrice(album, wished, withinReach).mapNotNull { hole ->
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
 * The empty casillas of a plate that have a price to say: the ones within reach, and the marked ones.
 *
 * On a plate within reach the second half adds nothing — a marked hole is already one of them — so the
 * union is only ever wider on the plates ADR 0028 §1 leaves outside, which is exactly what a mark is
 * for: of the 51, this one.
 */
private fun holesToPrice(
    album: CollectionCatalogAlbum,
    wished: Set<WishKey>,
    withinReach: List<CollectionCatalogAlbumMember>,
): List<CollectionCatalogAlbumMember> {
    if (wished.isEmpty()) return withinReach
    val counted = withinReach.mapTo(mutableSetOf()) { it.member.id }
    return withinReach + album.members.filter { candidate ->
        candidate.status is CollectionCatalogMemberStatus.Missing &&
            candidate.member.id !in counted &&
            candidate.member.wishKey() in wished
    }
}

/**
 * What entering one plate of the shelf window costs, and when its price was brought (ADR 0030 §6).
 *
 * @param holes how many casillas the amount covers, which is not [slots]: a hole Numista had no price
 *   for adds its silver floor, and one whose issue nobody can name adds nothing. The figure is a floor,
 *   like the plate's own cost of closing.
 * @param readAt the **oldest** of the reads the amount is made of, which is how a total whose parts
 *   arrived on different days is dated ([#494](https://github.com/jenarvaezg/coindex/issues/494)). A
 *   date over a total is a promise about all of it.
 */
data class ShowcaseCost(
    val eur: Double,
    val holes: Int,
    val slots: Int,
    val readAt: Long,
)

/**
 * The money of a plate the collector owns nothing of: one figure, and the price inside each hole.
 *
 * **A plate that has never been valued says nothing**, and that is a decision rather than a
 * consequence: the silver floor costs no API call at all — the spot is two keyless calls (ADR 0028 §9)
 * and the weight is in every seeded ficha — so an amount *could* be shown over the twenty without
 * anybody pressing anything. It is not, for the reason ADR 0028 §1 gives for the plates over its
 * threshold: what a floor-only figure would say is «entrar cuesta al menos esto», and the collector
 * cannot tell it apart from the real price. So the gate is exactly whether **this phone asked about the
 * issue** — `readAt` — which is the same row that makes the date sayable.
 *
 * @param readAt when each issue's price landed, from [com.jenarvaezg.coindex.data.prices.PriceBook].
 */
fun showcaseMoney(
    plate: ShowcasePlate,
    state: CollectionState,
    listings: IssueListings,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
    readAt: (Int, Int) -> Long?,
): PlateMoney {
    var total = 0.0
    var oldest = Long.MAX_VALUE
    val holeCosts = buildMap {
        for (hole in plate.album.members) {
            if (hole.status !is CollectionCatalogMemberStatus.Missing) continue
            val typeId = hole.member.numistaTypeId ?: continue
            val issueId = listings.issueOf(hole.member) ?: continue
            // Asked about, and therefore sayable. An issue this phone has never priced has no date to
            // show and no figure to show either, whatever its metal is worth.
            val read = readAt(typeId, issueId) ?: continue
            val cost = holeValue(
                typeId = typeId,
                issueId = issueId,
                meta = state.typeMeta[typeId],
                spot = spot,
                prices = prices,
            ) ?: continue
            put(hole.member.id, cost.eur)
            total += cost.eur
            oldest = minOf(oldest, read)
        }
    }
    return PlateMoney(
        // No «Valor actual»: a plate holding nothing has no pieces to total, and that is absence and
        // not a zero (ADR 0030 §6).
        entry = holeCosts
            .takeIf { it.isNotEmpty() }
            ?.let { ShowcaseCost(total, it.size, plate.slots, oldest) },
        holeCosts = holeCosts,
    )
}

/**
 * What each marked casilla of the annex would cost, by key (ADR 0029).
 *
 * The list's own reading of the same rule the plate uses, and it takes the resolved slots rather than
 * an album because the list crosses plates: what a hole costs is `holeValue` — two prices and never
 * three, in `unc` (ADR 0028 §8) — and the issue it is addressed to is the curated file's or a stored
 * listing's, exactly as on the plate.
 */
fun wishCosts(
    slots: List<WishedSlot>,
    state: CollectionState,
    listings: IssueListings,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): Map<WishKey, Double> = slots.mapNotNull { slot ->
    val cost = holeValue(
        typeId = slot.typeId,
        issueId = listings.issueOf(slot.member),
        meta = state.typeMeta[slot.typeId],
        spot = spot,
        prices = prices,
    ) ?: return@mapNotNull null
    slot.key to cost.eur
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
