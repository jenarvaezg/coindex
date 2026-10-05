package com.jenarvaezg.coindex.ui

import com.jenarvaezg.coindex.data.CollectionState
import com.jenarvaezg.coindex.data.prices.PriceBook
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
 * @param amount in the ladder's own unit.
 * @param approximate only for the stack, scaled up from the pieces that have a `thickness`; the
 *   label says «unos».
 */
data class LadderReading(
    val ladder: Ladder,
    val amount: Double,
    val placement: LadderPlacement,
    val approximate: Boolean,
)

/**
 * The country that portrays the collection. Its shares of pieces, weight and silver together say
 * what none says alone, such as «many small coins»; the share of the value is money and comes and
 * goes with it.
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
 * The amount, where it came from, and the silver spot behind it.
 *
 * @param paid what was paid for the pieces with a declared price, against their value today. Here
 *   rather than on [FiguresSubject] so the export's money switch withholds it with the total. Null
 *   when no row declares a price.
 */
data class MoneyReading(
    val value: CollectionValue,
    val spot: SilverSpot,
    val paid: PaidComparison?,
)

/**
 * Everything «Las cifras» draws, assembled once.
 *
 * @param money absent, not zero, until the market lands (ADR 0028 §7): without catalog prices the
 *   value is `max(silver, paid)`, a total that is false rather than incomplete (#316). Every other
 *   field comes from the APK.
 */
data class FiguresSubject(
    val figures: CollectionFigures,
    val money: MoneyReading?,
    val ladders: List<LadderReading>,
    val portrait: CountryPortrait?,
    /**
     * Whether the page says the money is missing, in the money's slot (#519). Never true alongside
     * [money], and false when the absence isn't the market's: the export with money off, or a pass
     * about to finish on its own (`ValuationStatus.waiting`).
     */
    val moneyWaiting: Boolean = false,
)

/**
 * Assembles the page from what the phone holds.
 *
 * @param moneyAllowed the export's money switch only (#228, ADR 0021 §13); always true on screen.
 *   Off, it withdraws the amount and every figure derived from it, such as a country's share of the
 *   value.
 * @param settled whether the valuation pass has finished. False leaves [FiguresSubject.money]
 *   absent however many prices are on the phone.
 * @param waiting whether that absence gets a line (#519). The pass's own reading, since only some
 *   of its hold reasons are worth saying.
 */
fun figuresSubject(
    state: CollectionState,
    book: PriceBook,
    settled: Boolean,
    moneyAllowed: Boolean = true,
    waiting: Boolean = false,
): FiguresSubject {
    val figures = collectionFigures(state.items, state.typeMeta)
    val spot = book.spot
    val money = if (!moneyAllowed || !settled || spot == null) {
        null
    } else {
        MoneyReading(
            collectionValue(state.items, state.typeMeta, spot, book::of, book::readAt),
            spot,
            paidComparison(state.items, state.typeMeta, spot, book::of),
        )
    }
    return FiguresSubject(
        figures = figures,
        money = money,
        ladders = listOf(
            reading(Ladders.weight, figures.weight.value / 1_000.0, approximate = false),
            reading(Ladders.row, figures.row.value, approximate = false),
            // Scaled rather than measured; a collection with no thickness at all reads zero.
            reading(
                Ladders.stack,
                figures.stack.extrapolated ?: 0.0,
                approximate = !figures.stack.complete,
            ),
        ),
        portrait = portrait(state, figures, money, book),
        // The export never waits: with money off it was asked not to have the section
        // (ADR 0021 §13).
        moneyWaiting = moneyAllowed && money == null && waiting,
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
 * The country with the most pieces, and its four shares. One country, not a ranking: Coins is where
 * a country is looked up, and the portrait leads there.
 */
private fun portrait(
    state: CollectionState,
    figures: CollectionFigures,
    money: MoneyReading?,
    book: PriceBook,
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
            pieceValue(item, meta, book.spot, book::of)?.let { tally.value += it.eur * quantity }
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
 * What one coin is worth, for its ficha: the page's three sources, for one type. Per piece or per
 * plate this is a buying aid; totalled for the shelf it would be wealth management (ADR 0026 §10).
 *
 * @param pieces how many pieces of the type the total covers, so «×3» isn't read as one coin's
 *   price.
 * @param source and [grade] only when every piece agrees on them.
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
    book: PriceBook,
): CoinValue? {
    val meta = state.typeMeta[typeId]
    val valued = state.items
        .filter { it.typeId == typeId }
        .mapNotNull { item ->
            pieceValue(item, meta, book.spot, book::of)?.let { it to item.quantity.coerceAtLeast(1) }
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

/**
 * What a plate's own coins are worth, for the figure over its title.
 *
 * @param catalogReadAt the oldest catalogue read behind the amount (#494, #594). Null when no
 *   catalogue price was ever asked for its coins.
 */
data class PlateValue(val eur: Double, val pieces: Int, val catalogReadAt: Long? = null)

/**
 * What closing a plate would cost, for the header's second figure (#493).
 *
 * @param holes how many empty casillas the amount covers, not how many the plate has: a hole with
 *   no known issue or no price on the phone adds nothing, so the figure is a floor.
 * @param catalogReadAt its own oldest read, not [PlateValue]'s: a marked casilla is repriced
 *   whatever the plate's shape (ADR 0029 §4), so this line can be months fresher (#594).
 */
data class PlateCost(val eur: Double, val holes: Int, val catalogReadAt: Long? = null)

/**
 * Everything a plate's header says about money, and the price inside each of its empty casillas:
 * one walk of the album behind one gate, since until the market lands there is no value, cost or
 * stamp (ADR 0028 §7).
 *
 * @param holeCosts the price of each empty casilla, by member id, for the stamp inside it. Past the
 *   threshold of ADR 0028 §1 only marked casillas have one (ADR 0029 §4).
 */
data class PlateMoney(
    val value: PlateValue? = null,
    val cost: PlateCost? = null,
    /**
     * What entering costs, on a plate that isn't the collector's (ADR 0030 §6): the whole plate
     * priced (§7), dated because nothing will refresh it (§4). Such a plate leaves [value] and
     * [cost] null; the collector's plates leave this null.
     */
    val entry: ShowcaseCost? = null,
    /**
     * Whether this phone has asked Numista about this plate at all (ADR 0028 §4, ADR 0030 §4).
     * «No price» is an answer, so a plate asked and left without a figure isn't offered «tasar»
     * again as if nobody had asked.
     */
    val entryAsked: Boolean = false,
    val holeCosts: Map<String, Double> = emptyMap(),
    /**
     * Whether the plate says its money is still coming (#519), in place of the readings above. A
     * shelf-window plate never sets it: its prices don't wait on the collection's pass
     * (ADR 0030 §3).
     */
    val waiting: Boolean = false,
)

/**
 * The plate's money, assembled once for the header and the casillas.
 *
 * @param book the whole book, so the header and the casillas agree on when (ADR 0028). The issue
 *   each empty casilla stands for comes from its stored listings (#452), since few curated files
 *   name their issues.
 */
fun plateMoney(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    book: PriceBook,
    /**
     * The casillas of this album the collector marked. They are priced whatever the plate's shape
     * (ADR 0029 §4), so their stamps are drawn, but they don't enter the header's cost.
     */
    wished: Set<WishKey> = emptySet(),
): PlateMoney {
    // One walk of the holes, shared so the header's cost and the casillas' stamps are about the
    // same holes (ADR 0028 §1).
    val withinReach = holesWithinReach(album)
    val closing = withinReach.mapTo(mutableSetOf()) { it.member.id }
    val priced = holeCosts(album, state, book, wished, withinReach)
    return PlateMoney(
        value = plateValue(album, state, book),
        // Only the holes within reach: a marked hole past the threshold is not the cost of closing
        // the plate (ADR 0029 §4). Null rather than zero, which would say closing is free.
        cost = priced
            .filterKeys { it in closing }
            .values
            .takeIf { it.isNotEmpty() }
            ?.let { holes ->
                PlateCost(
                    eur = holes.sumOf { it.eur },
                    holes = holes.size,
                    // The oldest read among these holes only (#494).
                    catalogReadAt = holes.mapNotNull { it.readAt }.minOrNull(),
                )
            },
        holeCosts = priced.mapValues { (_, hole) -> hole.eur },
    )
}

/**
 * One empty casilla's cost and when its catalogue price was fetched, kept together so a sum and its
 * date come from the same reading of the book (ADR 0028, #536).
 */
private data class HolePrice(val eur: Double, val readAt: Long?)

/**
 * What each empty casilla of a plate costs: those within reach ([holesWithinReach]) and the marked
 * ones, the same holes the pass spent its calls on (ADR 0028 §1, ADR 0029 §4). A casilla with no
 * price is absent, not zero.
 */
private fun holeCosts(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    book: PriceBook,
    wished: Set<WishKey>,
    withinReach: List<CollectionCatalogAlbumMember>,
): Map<String, HolePrice> =
    holesToPrice(album, wished, withinReach).mapNotNull { hole ->
        val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
        val issueId = book.listings.issueOf(hole.member)
        val cost = holeValue(
            typeId = typeId,
            issueId = issueId,
            meta = state.typeMeta[typeId],
            spot = book.spot,
            prices = book::of,
        ) ?: return@mapNotNull null
        // Dated by when the catalogue price was asked, as in `showcaseMoney` (ADR 0030 §6), even
        // when the silver beat it.
        hole.member.id to HolePrice(cost.eur, issueId?.let { book.readAt(typeId, it) })
    }.toMap()

/**
 * The empty casillas with a price to say: those within reach, plus the marked ones. The union only
 * widens on plates past ADR 0028 §1's threshold, which is what a mark is for.
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
 * What entering one plate of the shelf window costs, and when its price was fetched (ADR 0030 §6).
 *
 * @param holes how many casillas the amount covers, which is not [slots]: a hole with no Numista
 *   price adds its silver floor, and one with no known issue adds nothing, so the figure is a
 *   floor.
 * @param readAt the oldest read behind the amount (#494).
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
 * A plate never valued shows nothing, although its silver floor would cost no API call
 * (ADR 0028 §9): a floor-only figure can't be told apart from the real price (ADR 0028 §1). The
 * gate is whether this phone asked about the issue ([PriceBook.readAt]), which also dates it.
 */
fun showcaseMoney(
    plate: ShowcasePlate,
    state: CollectionState,
    book: PriceBook,
): PlateMoney {
    var total = 0.0
    var oldest = Long.MAX_VALUE
    var asked = false
    val holeCosts = buildMap {
        for (hole in plate.album.members) {
            if (hole.status !is CollectionCatalogMemberStatus.Missing) continue
            val typeId = hole.member.numistaTypeId ?: continue
            val issueId = book.listings.issueOf(hole.member) ?: continue
            // Never asked: nothing to date and nothing to show, whatever the metal is worth.
            val read = book.readAt(typeId, issueId) ?: continue
            // Asked, whatever came back: «no price» is an answer the plate must show (ADR 0028 §4).
            asked = true
            val cost = holeValue(
                typeId = typeId,
                issueId = issueId,
                meta = state.typeMeta[typeId],
                spot = book.spot,
                prices = book::of,
            ) ?: continue
            put(hole.member.id, cost.eur)
            total += cost.eur
            oldest = minOf(oldest, read)
        }
    }
    return PlateMoney(
        // No «Valor actual»: the plate holds nothing (ADR 0030 §6).
        entry = holeCosts
            .takeIf { it.isNotEmpty() }
            ?.let { ShowcaseCost(total, it.size, plate.slots, oldest) },
        entryAsked = asked,
        holeCosts = holeCosts,
    )
}

/**
 * What each marked casilla of the annex would cost, by key (ADR 0029): the plate's rule
 * (`holeValue`, ADR 0028 §8) over resolved slots, because the list crosses plates.
 */
fun wishCosts(
    slots: List<WishedSlot>,
    state: CollectionState,
    book: PriceBook,
): Map<WishKey, Double> = slots.mapNotNull { slot ->
    val cost = holeValue(
        typeId = slot.typeId,
        issueId = book.listings.issueOf(slot.member),
        meta = state.typeMeta[slot.typeId],
        spot = book.spot,
        prices = book::of,
    ) ?: return@mapNotNull null
    slot.key to cost.eur
}.toMap()

/**
 * The value of what a plate holds: only the pieces filling its casillas, so a type with more loose
 * rows elsewhere counts as the casilla it fills. The cost of closing is a separate figure with its
 * own criterion (#493).
 */
fun plateValue(
    album: CollectionCatalogAlbum,
    state: CollectionState,
    book: PriceBook,
): PlateValue? {
    val filled = album.members
        .mapNotNull { it.status as? CollectionCatalogMemberStatus.Owned }
        .flatMap { owned -> owned.items }
        .map { it.itemId }
        .toSet()
    if (filled.isEmpty()) return null
    var total = 0.0
    var pieces = 0
    var oldest = Long.MAX_VALUE
    for (item in state.items.filter { it.id in filled }) {
        val value = pieceValue(item, state.typeMeta[item.typeId], book.spot, book::of) ?: continue
        val quantity = item.quantity.coerceAtLeast(1)
        total += value.eur * quantity
        pieces = saturatingAdd(pieces, quantity)
        item.issueId
            ?.let { book.readAt(item.typeId, it) }
            ?.let { oldest = minOf(oldest, it) }
    }
    return if (pieces == 0) {
        null
    } else {
        PlateValue(total, pieces, oldest.takeIf { it != Long.MAX_VALUE })
    }
}
