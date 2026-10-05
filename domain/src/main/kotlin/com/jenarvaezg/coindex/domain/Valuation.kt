package com.jenarvaezg.coindex.domain

private val SILVER_FINENESS = Regex("""(?:plata|silver)\s*\.?([0-9]{3}(?:[.,][0-9]+)?)""")

/**
 * The millesimal fineness of a silver alloy, read by rule from Numista's `composition.text` like
 * [inferMetal] (ADR 0005), so an improved rule also fixes fichas already cached.
 *
 * The whole text is searched, not only the head [inferMetal] reads, because «Vellón (plata 400)»
 * puts the fineness inside the bracket. The first number wins, which keeps «Plata 999,9 (Marked
 * "PLATA 1000")» at 999,9. Null when the text names no fineness: such a piece has no silver floor.
 */
fun silverFineness(composition: String?): Double? {
    if (inferMetal(composition) != Metal.Silver) return null
    val match = SILVER_FINENESS.find(composition?.lowercase() ?: return null) ?: return null
    // Spanish decimal comma, as in «Plata 999,9».
    val millesimal = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    return (millesimal / 1_000.0).takeIf { it > 0.0 && it <= 1.0 }
}

/**
 * The troy ounce of silver in euros and when this phone read it. An expired spot is still shown,
 * with its date, so the date is part of the value (#316, ADR 0028 §5).
 */
data class SilverSpot(val eurPerTroyOunce: Double, val readAtMillis: Long)

/** The grades Numista prices, worst to best. */
val NUMISTA_GRADES: List<String> = listOf("g", "vg", "f", "vf", "xf", "au", "unc")

/** The grade holes are valued in and ungraded pieces fall back to (ADR 0028 §8). */
const val UNCIRCULATED: String = "unc"

/** Where a piece's value came from, so the screen can always say it (#316). */
enum class ValueSource {
    /** Numista's estimated price for this issue in this piece's own grade. */
    Market,

    /** Numista's price for the nearest grade it does publish one for. */
    NeighbouringGrade,

    /** Its fine silver weight times the spot. */
    Silver,

    /** What the collector recorded paying for it. */
    Paid,
}

/**
 * What one piece is worth, and from which source.
 *
 * @param eur the value of one piece, never of the row; whoever totals multiplies by quantity.
 */
data class PieceValue(val eur: Double, val source: ValueSource, val grade: String? = null)

/**
 * The fine silver of one piece in grams, or null when its ficha lacks weight or fineness. Fine, not
 * gross: the spot buys the silver in a .835 coin, not its copper (`docs/ux/cifras-326.md`).
 */
fun fineSilverGrams(meta: TypeMeta?): Double? {
    val grams = meta?.weightGrams ?: return null
    val fineness = meta.fineness ?: return null
    return grams * fineness
}

/**
 * What one piece is worth: the maximum of market, silver and paid, piece by piece and never by
 * family (ADR 0026 §10, #316). Catalogue prices do not follow the metal, so which source wins
 * shifts as the spot moves.
 *
 * The grade is the pricing key (#316): a piece is valued in its own grade, or the nearest one
 * Numista prices. An ungraded piece is valued in [UNCIRCULATED], like a hole.
 *
 * Null when no source covers the piece (the coverage sentence of ADR 0028 §7).
 */
fun pieceValue(
    item: CollectedItem,
    meta: TypeMeta?,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
): PieceValue? {
    val candidates = mutableListOf<PieceValue>()
    marketValue(item, prices)?.let(candidates::add)
    silverFloor(meta, spot)?.let(candidates::add)
    // `price` is what was paid for the whole row (a lot carries one figure for many pieces), and
    // the maximum compares per piece.
    item.price?.takeIf { it > 0.0 }?.let { paid ->
        candidates.add(PieceValue(paid / item.quantity.coerceAtLeast(1), ValueSource.Paid))
    }
    return candidates.maxByOrNull { it.eur }
}

/**
 * What one empty casilla would cost to fill: the greater of Numista's price and the metal (#493),
 * since nobody paid for it. The price is always in `unc` (ADR 0028 §8), never a neighbouring
 * grade, because the plate header labels this amount «en sin circular».
 *
 * @param issueId the issue the casilla stands for, declared by the curated file (ADR 0014) or
 *   answered by a stored listing (#452). Null leaves only the metal.
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
    silverFloor(meta, spot)?.let(candidates::add)
    return candidates.maxByOrNull { it.eur }
}

/**
 * What the metal of one piece is worth, or null where the ficha or the spot cannot support it.
 * Shared by [pieceValue] and [holeValue].
 */
private fun silverFloor(meta: TypeMeta?, spot: SilverSpot?): PieceValue? {
    if (spot == null) return null
    val grams = fineSilverGrams(meta) ?: return null
    return PieceValue(gramsToOunces(grams) * spot.eurPerTroyOunce, ValueSource.Silver)
}

/**
 * Numista's price for this piece in its grade, or in the nearest grade of [NUMISTA_GRADES] that has
 * one. Ties go to the worse grade, so a guess never favours the collector.
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
 * What the collector paid for the pieces with a recorded price, and what those pieces are worth
 * today.
 *
 * Only rows that declare a price count, and they are their own denominator. The unpriced rest mixes
 * gifts with purchases recorded before prices were, so the figure never says what share of the
 * collection was bought (#491, `docs/ux/cifras-316.md`).
 *
 * @param paid summed as `price` comes, which is per row.
 * @param today the same pieces at the maximum of the three sources. Paid is one of them, so this is
 *   never below [paid] (pinned in `ValuationTest`).
 */
data class PaidComparison(val paid: Double, val today: Double, val pieces: Int)

/**
 * The comparison over the rows that declare a price, or null when none does: «pagaste 0 €» would
 * only report that the field is unused.
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
        // Unreachable while `price` is one of the three sources; kept so both sides always total
        // the same pieces.
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
 * @param valued how many of them a source covered; the page states coverage, never progress (ADR
 *   0028 §7).
 * @param catalogReadAt the oldest catalogue read behind the total, which dates a total whose parts
 *   arrived on different days (#494). Null when no piece was ever asked about, so the total comes
 *   from metal and paid prices only (#594).
 */
data class CollectionValue(
    val eur: Double,
    val valued: Int,
    val pieces: Int,
    val catalogReadAt: Long? = null,
) {
    val covered: Boolean get() = valued == pieces
}

/**
 * Totals the maximum of the three sources over every piece.
 *
 * Callers must not call this while market prices are still arriving: without them the total is
 * the silver floor #316 rejected, which is false rather than incomplete (ADR 0028 §7). Whether the
 * market has landed is a question about the pass, asked before this.
 */
fun collectionValue(
    items: List<CollectedItem>,
    typeMeta: TypeMetaIndex,
    spot: SilverSpot?,
    prices: (Int, Int, String) -> Double?,
    /**
     * When this phone asked Numista about an issue, to date the total (#594). A lambda beside
     * [prices] so a total and its date come from the same reading of the catalogue.
     *
     * Every asked issue counts, not only the priced ones, as in `showcaseMoney` (ADR 0030 §6): a
     * piece whose silver beat its catalogue price still had that price read on that day. A date may
     * only err older.
     */
    readAt: (Int, Int) -> Long?,
): CollectionValue {
    var total = 0.0
    var valued = 0
    var pieces = 0
    var oldest = Long.MAX_VALUE
    for (item in items) {
        val quantity = item.quantity.coerceAtLeast(1)
        pieces = saturatingAdd(pieces, quantity)
        val value = pieceValue(item, typeMeta[item.typeId], spot, prices) ?: continue
        total += value.eur * quantity
        valued = saturatingAdd(valued, quantity)
        item.issueId
            ?.let { readAt(item.typeId, it) }
            ?.let { oldest = minOf(oldest, it) }
    }
    return CollectionValue(total, valued, pieces, oldest.takeIf { it != Long.MAX_VALUE })
}
