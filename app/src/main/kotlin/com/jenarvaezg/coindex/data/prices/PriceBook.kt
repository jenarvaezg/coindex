package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.domain.SilverSpot

/** One price's address: an issue in a grade. */
data class PriceKey(val typeId: Int, val issueId: Int, val grade: String)

/**
 * Every price this phone holds, and the spot that buys the silver floor.
 *
 * Read as one value and not as three queries, because a valuation is only ever right when the three
 * agree about *when*: the total is stamped with the spot's date, and a price book assembled from parts
 * read at different moments would put yesterday's silver under today's total.
 */
data class PriceBook(
    val prices: Map<PriceKey, Double> = emptyMap(),
    val spot: SilverSpot? = null,
) {
    /** The price of one issue in one grade, in the shape the domain's valuation asks for. */
    fun of(typeId: Int, issueId: Int, grade: String): Double? =
        prices[PriceKey(typeId, issueId, grade)]
}

fun priceBook(rows: List<IssuePriceEntity>, spot: SilverSpot?): PriceBook = PriceBook(
    prices = rows.associate { PriceKey(it.typeId, it.issueId, it.grade) to it.eur },
    spot = spot,
)
