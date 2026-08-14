package com.jenarvaezg.coindex.domain

private val SILVER_FINENESS = Regex("""(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)""")

/**
 * The millesimal fineness of a silver alloy, read from Numista's `composition.text`.
 *
 * A rule and not a column, like [inferMetal] and [inferFinish] before it (ADR 0005): Numista has no
 * fineness field either, only prose, and a rule improved tomorrow has to fix fichas cached today.
 *
 * The whole text is searched and not only the head, which is what [inferMetal] reads: «Vellón (plata
 * 400)» names its alloy in the head and its fineness inside the bracket, and it is the only shape in
 * the 916 seeded fichas where the two are apart. The **first** number wins, which is what keeps
 * «Plata 999,9 (Marked "PLATA 1000")» at 999,9.
 *
 * Returns null when the text names no fineness at all — two fichas say «Plata» and nothing else —
 * because a piece with no declared fineness has no silver floor, not a floor of one.
 */
fun silverFineness(composition: String?): Double? {
    if (inferMetal(composition) != Metal.Silver) return null
    val match = SILVER_FINENESS.find(composition?.lowercase() ?: return null) ?: return null
    // The comma is the Spanish decimal separator, which is what «Plata 999,9» is written with.
    val millesimal = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    return (millesimal / 1_000.0).takeIf { it > 0.0 && it <= 1.0 }
}

/**
 * The troy ounce of silver in euros, and the day this phone read it.
 *
 * The date travels with the number and is not derived from it, because it is the whole reason the
 * figure is not a quotation (#316, ADR 0028 §5): an expired spot keeps being shown with the date it
 * was brought rather than deleted, so «when» is as much of this value as «how much».
 */
data class SilverSpot(val eurPerTroyOunce: Double, val readAtMillis: Long)

/** The grades Numista prices, worst to best, which is also the order a neighbour is looked for in. */
val NUMISTA_GRADES: List<String> = listOf("g", "vg", "f", "vf", "xf", "au", "unc")

/** What a hole is valued in, and what a piece with no grade of its own falls back to (ADR 0028 §8). */
const val UNCIRCULATED: String = "unc"

/** Where the number a piece is worth came from. Never left to be guessed at (#316). */
enum class ValueSource {
    /** Numista's estimated price for this issue in this piece's own grade. */
    Market,

    /** Numista's price for the nearest grade it does publish one for. */
    NeighbouringGrade,

    /** Its metal: weight times the fineness of its alloy, times the spot. */
    Silver,

    /** What the collector recorded paying for it. */
    Paid,
}

/**
 * What one piece is worth, and out of which of the three sources.
 *
 * @param eur the value of **one** piece and never of the row: a row of 102 bolívares is 102 pieces,
 *   and multiplying belongs to whoever is totalling.
 */
data class PieceValue(val eur: Double, val source: ValueSource, val grade: String? = null)

/**
 * The fine silver of one piece, in grams, or null when its ficha does not support the claim.
 *
 * Fine and not gross, which is the difference between a silver floor and a lie: a .835 coin is 16,5 %
 * copper (`docs/ux/cifras-326.md`), and spot buys the silver in it and not the coin.
 */
fun fineSilverGrams(meta: TypeMeta?): Double? {
    val grams = meta?.weightGrams ?: return null
    val fineness = meta.fineness ?: return null
    return grams * fineness
}

/**
 * What one piece is worth: **the maximum of three numbers**, piece by piece and never by family
 * (ADR 0026 §10, #316).
 *
 * It is not an occasional tie-break. Catalog prices do not follow the metal, so the order inverts as
 * spot rises: at today's spot the market wins on 517 of his 572 pieces and silver on 14; at 34 % more
 * spot, silver wins on 338. An app that had picked «the market price» as its single source would say
 * a silver duro is worth less than its own silver.
 *
 * The grade is the pricing key and not an analytic (#316): a piece is valued **in its own grade**,
 * with the nearest neighbour when Numista publishes none for it — 188 exact, 22 neighbouring, 19 with
 * none of his 229 rows. A piece the collector never graded is valued in [UNCIRCULATED], which is also
 * what a hole is valued in.
 *
 * Returns null when no source covers the piece at all, which is the case the coverage sentence of ADR
 * 0028 §7 exists for. Today the maximum covers 100 % of his 574.
 */
fun pieceValue(
    item: CollectedItem,
    meta: TypeMeta?,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PieceValue? {
    val candidates = mutableListOf<PieceValue>()
    marketValue(item, prices)?.let(candidates::add)
    if (spot != null) {
        fineSilverGrams(meta)?.let { grams ->
            candidates.add(
                PieceValue(gramsToOunces(grams) * spot.eurPerTroyOunce, ValueSource.Silver),
            )
        }
    }
    // Divided by the quantity, because `price` is what was paid for the row: the six bolívares rows
    // the father bought as lots carry one figure for 102 pieces, and a value per piece is what the
    // maximum compares and what the total multiplies back up.
    item.price?.takeIf { it > 0.0 }?.let { paid ->
        candidates.add(PieceValue(paid / item.quantity.coerceAtLeast(1), ValueSource.Paid))
    }
    return candidates.maxByOrNull { it.eur }
}

/**
 * What one empty casilla would cost to fill: the greater of **two** prices and never of three (#493).
 *
 * The maximum is the same rule [pieceValue] reads, minus the source a hole cannot have: nobody paid
 * for a coin that is not here, so what is left is Numista's catalogue price and the metal.
 *
 * And it is priced **in `unc`** (ADR 0028 §8), which is the grade the pass asked for, without falling
 * to a neighbouring grade the way a piece does. That is not thrift either: the plate's header says «en
 * sin circular» beside this amount, and a figure that had come out of `xf` would make it name a grade
 * its own number did not come from.
 *
 * @param issueId which issue the casilla stands for — declared by the curated file (ADR 0014) or
 *   answered by a stored listing (#452). Null is a hole with nothing to address a price to, and then
 *   only the metal can answer for it.
 */
fun holeValue(
    typeId: Int,
    issueId: Int?,
    meta: TypeMeta?,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PieceValue? {
    val candidates = mutableListOf<PieceValue>()
    issueId
        ?.let { prices(typeId, it, UNCIRCULATED) }
        ?.let { candidates.add(PieceValue(it, ValueSource.Market, UNCIRCULATED)) }
    if (spot != null) {
        fineSilverGrams(meta)?.let { grams ->
            candidates.add(
                PieceValue(gramsToOunces(grams) * spot.eurPerTroyOunce, ValueSource.Silver),
            )
        }
    }
    return candidates.maxByOrNull { it.eur }
}

/**
 * Numista's price for this piece, in its grade or in the nearest one that has a price.
 *
 * The neighbour is the nearest grade in [NUMISTA_GRADES] by distance, and on a tie **the worse
 * grade**: guessing upwards is guessing in the collector's favour, which is the direction a valuation
 * must never round in.
 */
private fun marketValue(item: CollectedItem, prices: (Int, Int, String) -> Double?): PieceValue? {
    val issueId = item.issueId ?: return null
    val grade = item.grade?.lowercase()?.takeIf { it in NUMISTA_GRADES } ?: UNCIRCULATED
    prices(item.typeId, issueId, grade)?.let { own ->
        return PieceValue(own, ValueSource.Market, grade)
    }
    val index = NUMISTA_GRADES.indexOf(grade)
    return NUMISTA_GRADES
        .withIndex()
        .filter { (position, _) -> position != index }
        .sortedWith(
            compareBy({ (position, _) -> kotlin.math.abs(position - index) }, { it.index }),
        )
        .firstNotNullOfOrNull { (_, neighbour) ->
            prices(item.typeId, issueId, neighbour)?.let { price ->
                PieceValue(price, ValueSource.NeighbouringGrade, neighbour)
            }
        }
}

/**
 * What the collector paid for the pieces whose price he wrote down, and what those same pieces are
 * worth today.
 *
 * **Only what is declared, and its own denominator with it.** `price` covers 84 of his 229 rows —
 * which are 91 of his 572 pieces, because what has no price is the Venezuelan bulks
 * (`docs/ux/cifras-316.md`). The complement is not a hole in the data: it is what he did not buy,
 * gifts and inheritance. But in the first 140 rows he was not writing prices down yet, so purchases
 * he never noted are mixed in with the presents, and the figure therefore says how many pieces
 * declared a price and **never** what share of the collection they are (#491).
 *
 * @param paid totalled as `price` comes, which is per **row**: the bulks he bought as lots carry one
 *   figure for 102 pieces.
 * @param today the same pieces under the page's one rule, the maximum of the three sources. Since
 *   what was paid is one of those three, this can never come out under [paid] — see
 *   `ValuationTest`, where that is pinned as a consequence rather than found as a surprise.
 */
data class PaidComparison(val paid: Double, val today: Double, val pieces: Int)

/**
 * The comparison over the rows that declare a price, or null when none does.
 *
 * Null and not zero: «pagaste 0 €» is a sentence about a collection nobody bought, and what it would
 * really be reporting is that the collector does not use the field.
 */
fun paidComparison(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PaidComparison? {
    var paid = 0.0
    var today = 0.0
    var pieces = 0
    for (item in items) {
        val price = item.price?.takeIf { it > 0.0 } ?: continue
        // Unreachable while `price` is one of the three sources, and kept anyway: the two sides of a
        // comparison have to be totalled over the same pieces or the sentence lies by subtraction.
        val value = pieceValue(item, typeMeta[item.typeId], spot, prices) ?: continue
        val quantity = item.quantity.coerceAtLeast(1)
        paid += price
        today += value.eur * quantity
        pieces = saturatingAdd(pieces, quantity)
    }
    return if (pieces == 0) null else PaidComparison(paid, today, pieces)
}

/**
 * What the whole collection is worth, and over how many of its pieces.
 *
 * @param pieces every piece the collection holds, quantities included.
 * @param valued how many of them a source covered. The page says **coverage and never progress**
 *   (ADR 0028 §7): «el valor de N de tus 574 piezas» is said, «llevo 140 de 223» is not.
 */
data class CollectionValue(val eur: Double, val valued: Int, val pieces: Int) {
    val covered: Boolean get() = valued == pieces
}

/**
 * Totals the maximum of the three sources over every piece.
 *
 * Callers must not reach here while the market is still arriving: `max(silver, paid)` gives 10.500 €
 * of the real 16.800, which is literally the «only the silver floor» that #316 rejected, and a total
 * at 60 % is not incomplete but **false** (ADR 0028 §7). Whether the market has landed is a question
 * about the pass and not about the collection, so it is asked before this one and not inside it.
 */
fun collectionValue(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): CollectionValue {
    var total = 0.0
    var valued = 0
    var pieces = 0
    for (item in items) {
        val quantity = item.quantity.coerceAtLeast(1)
        pieces = saturatingAdd(pieces, quantity)
        val value = pieceValue(item, typeMeta[item.typeId], spot, prices) ?: continue
        total += value.eur * quantity
        valued = saturatingAdd(valued, quantity)
    }
    return CollectionValue(total, valued, pieces)
}
