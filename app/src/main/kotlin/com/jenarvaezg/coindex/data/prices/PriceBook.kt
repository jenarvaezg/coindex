package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.domain.SilverSpot

/** One price's address: an issue in a grade. */
data class PriceKey(val typeId: Int, val issueId: Int, val grade: String)

/**
 * Every price this phone holds, the silver spot and the issue listings that address hole prices
 * (#452, #493), read as one value so a total, its dates and the issue each casilla resolves to all
 * come from the same moment.
 */
data class PriceBook(
    val prices: Map<PriceKey, Double> = emptyMap(),
    val spot: SilverSpot? = null,
    val listings: IssueListings = IssueListings.EMPTY,
    /**
     * When each issue's price reached this phone, by `(typeId, issueId)`: `issue_price_reads.readAt`.
     * Shown beside amounts no pass refreshes, such as the shelf window's (ADR 0030 §4).
     */
    val readAt: Map<Pair<Int, Int>, Long> = emptyMap(),
    /**
     * When each type's listing was read, by `typeId`. [listings] ignores expiry (#493), but a gesture
     * that announces its calls must count expired listings as calls, so [freshListings] uses these
     * dates (ADR 0030 §3).
     */
    val listingReadAt: Map<Int, Long> = emptyMap(),
) {
    /** The price of one issue in one grade, in the shape the domain's valuation asks for. */
    fun of(typeId: Int, issueId: Int, grade: String): Double? =
        prices[PriceKey(typeId, issueId, grade)]

    /** When this issue was priced, or null if this phone has never asked about it. */
    fun readAt(typeId: Int, issueId: Int): Long? = readAt[typeId to issueId]

    /**
     * The listings the pass would honour now: [listings] with `LISTING_LIFETIME_MILLIS` applied, as
     * `IssueListings.of` does, so the gesture never promises fewer calls than the pass spends
     * (ADR 0030 §3). The year map is cut too, or an expired type would count both its lookup and a
     * price per hole.
     */
    fun freshListings(nowMillis: Long): IssueListings {
        val listed = listings.listedTypeIds.filterTo(mutableSetOf()) { typeId ->
            listingReadAt[typeId]?.let { nowMillis - it < LISTING_LIFETIME_MILLIS } == true
        }
        return IssueListings(
            listedTypeIds = listed,
            issueIdByTypeAndYear = listings.issueIdByTypeAndYear
                .filterKeys { (typeId, _) -> typeId in listed },
        )
    }
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
