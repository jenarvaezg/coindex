package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.domain.SilverSpot

/** One price's address: an issue in a grade. */
data class PriceKey(val typeId: Int, val issueId: Int, val grade: String)

/**
 * Every price this phone holds, the spot that buys the silver floor, and who each price is addressed
 * to.
 *
 * Read as one value and not as three queries, because a valuation is only ever right when the three
 * agree about *when*: the total is stamped with the spot's date, and a price book assembled from parts
 * read at different moments would put yesterday's silver under today's total.
 *
 * [listings] arrives by the same door and for the same reason (#493): the price of a hole is on the
 * phone under an issue id, and which issue a casilla *is* comes out of the listings the pass stored
 * (#452). Read a moment apart, a plate could add up a price under one issue and stamp it into a
 * casilla the newer listing addresses to another.
 */
data class PriceBook(
    val prices: Map<PriceKey, Double> = emptyMap(),
    val spot: SilverSpot? = null,
    val listings: IssueListings = IssueListings.EMPTY,
    /**
     * When each issue's price reached this phone, by `(typeId, issueId)` (ADR 0030 §4).
     *
     * Here because an amount that never expires has to be **shown with its date**: the shelf window's
     * prices are asked for by a gesture and no pass ever refreshes them, so the date is the whole of
     * what makes the figure readable months later. It is `issue_price_reads.readAt` and nothing
     * derived — the same row that decides expiry for the collection's own prices.
     */
    val readAt: Map<Pair<Int, Int>, Long> = emptyMap(),
) {
    /** The price of one issue in one grade, in the shape the domain's valuation asks for. */
    fun of(typeId: Int, issueId: Int, grade: String): Double? =
        prices[PriceKey(typeId, issueId, grade)]

    /** When this issue was priced, or null if this phone has never asked about it. */
    fun readAt(typeId: Int, issueId: Int): Long? = readAt[typeId to issueId]
}

fun priceBook(
    rows: List<IssuePriceEntity>,
    spot: SilverSpot?,
    listings: IssueListings = IssueListings.EMPTY,
    reads: List<IssuePriceReadEntity> = emptyList(),
): PriceBook = PriceBook(
    prices = rows.associate { PriceKey(it.typeId, it.issueId, it.grade) to it.eur },
    spot = spot,
    listings = listings,
    readAt = reads.associate { (it.typeId to it.issueId) to it.readAt },
)
