package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
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
    /**
     * When each type's issue listing was read, by `typeId` — the other half of what a **spend** has to be
     * counted against (ADR 0030 §3).
     *
     * [listings] deliberately ignores expiry, because a screen has nothing to spend and ADR 0028 §5 keeps
     * showing an expired row (#493). A gesture that *names its calls* cannot use that reading: a type
     * listed four months ago counts as listed there, and the pass would spend the
     * `/types/{id}/issues` anyway. So the dates travel too, and [freshListings] is what the figure on the
     * button is counted from.
     */
    val listingReadAt: Map<Int, Long> = emptyMap(),
) {
    /** The price of one issue in one grade, in the shape the domain's valuation asks for. */
    fun of(typeId: Int, issueId: Int, grade: String): Double? =
        prices[PriceKey(typeId, issueId, grade)]

    /** When this issue was priced, or null if this phone has never asked about it. */
    fun readAt(typeId: Int, issueId: Int): Long? = readAt[typeId to issueId]

    /**
     * The listings the **pass** would honour right now, which is what a spend is counted against.
     *
     * Same rows as [listings] with the ninety days of `LISTING_LIFETIME_MILLIS` applied, so the ceiling a
     * gesture prints is the ceiling the pass then spends. Rounding a spend **down** is the one direction
     * that sentence must never err in (ADR 0030 §3).
     */
    fun freshListings(nowMillis: Long): IssueListings = IssueListings(
        listedTypeIds = listings.listedTypeIds.filterTo(mutableSetOf()) { typeId ->
            listingReadAt[typeId]?.let { nowMillis - it < LISTING_LIFETIME_MILLIS } == true
        },
        issueIdByTypeAndYear = listings.issueIdByTypeAndYear,
    )
}

fun priceBook(
    rows: List<IssuePriceEntity>,
    spot: SilverSpot?,
    listings: IssueListings = IssueListings.EMPTY,
    reads: List<IssuePriceReadEntity> = emptyList(),
    listingReads: List<TypeIssueReadEntity> = emptyList(),
): PriceBook = PriceBook(
    prices = rows.associate { PriceKey(it.typeId, it.issueId, it.grade) to it.eur },
    spot = spot,
    listings = listings,
    readAt = reads.associate { (it.typeId to it.issueId) to it.readAt },
    listingReadAt = listingReads.associate { it.typeId to it.readAt },
)
