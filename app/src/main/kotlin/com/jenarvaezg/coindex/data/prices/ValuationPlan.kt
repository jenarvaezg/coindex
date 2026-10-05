package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.domain.CatalogAlbums
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishedSlot

/**
 * A plate's holes are valued only when it is at most this many slots from closing (ADR 0028 §1).
 * Further out, a closing cost isn't something the collector can act on (ADR 0026 §10).
 */
const val HOLE_THRESHOLD_SLOTS: Int = 10

/**
 * Whether a plate with this many holes has a closing cost. Shared by the pass and the plate header
 * (#493) so they price and print the same plates. A closed plate has none.
 */
fun holesAreWithinReach(holes: Int): Boolean = holes in 1..HOLE_THRESHOLD_SLOTS

/**
 * A catalog price is read again after ninety days (#561), like a listing: Numista's estimates move
 * with the catalogue, and the daily silver spot is what follows the market (ADR 0028 §9). One pass
 * prices the whole collection, so its prices expire together and the next launch re-reads the full
 * plan; a shorter life would make that peak monthly. Expired prices are asked again, never deleted,
 * and every figure shows the age of its price (ADR 0028 §5, #594).
 */
const val PRICE_LIFETIME_MILLIS: Long = 90L * 24 * 60 * 60 * 1_000

/**
 * A type's issue listing is read again after ninety days (#452). Listings change only when Numista
 * adds an issue, but an open date run gains a slot every January, and a listing that never expired
 * would leave that hole unpriceable.
 */
const val LISTING_LIFETIME_MILLIS: Long = 90L * 24 * 60 * 60 * 1_000

/** An issue the collector owns a piece of. */
data class OwnedIssue(val typeId: Int, val issueId: Int)

/**
 * An empty slot of a plate within reach of closing. When the curated file declares [issueIds]
 * (ADR 0014) its price costs one call; otherwise the issue is first looked up by [year] through
 * `/types/{id}/issues`.
 */
data class PlateHole(
    val catalogId: String,
    val typeId: Int,
    val year: Int?,
    val issueIds: List<Int> = emptyList(),
)

/**
 * Everything one pass may ask Numista about, before subtracting what the phone already holds. Owned
 * issues feed the money on the page; holes feed each plate header's closing cost, never totalled.
 */
data class ValuationPlan(val owned: List<OwnedIssue>, val holes: List<PlateHole>) {
    val isEmpty: Boolean get() = owned.isEmpty() && holes.isEmpty()
}

/**
 * What this collection gives the pass to ask about (ADR 0028 §1): every piece with an issue id, plus
 * the holes within reach of open (evidenced) plates. A catalog with no evidence has no plate on
 * screen to show a cost in.
 */
fun valuationPlan(
    items: List<CollectedItem>,
    curation: Curation,
    /** The assembly's albums (#537); the pass reads their holes instead of rebuilding them. */
    albums: CatalogAlbums,
    evidencedCatalogIds: Set<String>,
    /**
     * Marked casillas, priced whatever their plate's size or evidence (ADR 0029 §4). The plate's own
     * «Coste de cerrar» still obeys the threshold.
     */
    wishes: List<WishedSlot> = emptyList(),
): ValuationPlan = ValuationPlan(
    owned = items
        .mapNotNull { item -> item.issueId?.let { OwnedIssue(item.typeId, it) } }
        .distinct(),
    // A marked hole of a plate within reach is in both halves; ask for it once.
    holes = (
        curation.catalogs
            .filter { it.id in evidencedCatalogIds }
            .flatMap { catalog -> plateHoles(catalog, albums[catalog]) } + wishHoles(wishes)
        ).distinct(),
)

/**
 * The marked casillas as holes of the plan (ADR 0029 §4). Built from the resolved slot, so they carry
 * the curated file's issues and deduplicate against the plate's own holes.
 */
fun wishHoles(wishes: List<WishedSlot>): List<PlateHole> = wishes.map { slot ->
    PlateHole(
        catalogId = slot.catalog.id,
        typeId = slot.typeId,
        year = slot.member.year,
        issueIds = slot.member.numistaIssueIds,
    )
}

/**
 * What the marked casillas cost a month, printed in Ajustes beside the budget (ADR 0029 §5). Counted
 * with the pass's own arithmetic on an empty phone: one `/prices` per hole plus one
 * `/types/{id}/issues` per type whose file names no issue. A ceiling: prices and listings last ninety
 * days (ADR 0028 §5), so the real spend is quarterly.
 */
fun wishCallsPerMonth(wishes: List<WishedSlot>): Int = valuationCallCount(
    plan = ValuationPlan(owned = emptyList(), holes = wishHoles(wishes)),
    reads = emptyList(),
    nowMillis = 0L,
)

/**
 * What valuing one plate of the shelf window asks about (ADR 0030 §3): no owned issues, and every
 * empty casilla, because the threshold doesn't apply to a plate that isn't yours (ADR 0030 §7). It
 * runs through the same [ValuationPass], so the rows land in the same tables and the monthly pass
 * refreshes its marked casillas afterwards (ADR 0029 §4).
 */
fun showcaseValuationPlan(plate: ShowcasePlate): ValuationPlan = ValuationPlan(
    owned = emptyList(),
    holes = plate.album.members
        .filter { it.status is CollectionCatalogMemberStatus.Missing }
        .mapNotNull { hole ->
            val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
            PlateHole(plate.catalog.id, typeId, hole.member.year, hole.member.numistaIssueIds)
        }
        .distinct(),
)

/**
 * What valuing this plate would spend now, as the gesture prints it (ADR 0030 §3). Same arithmetic
 * as the pass ([valuationCallCount]) so the promise matches the spend, but fed from the screen's
 * [PriceBook] instead of the database, with only the listings the pass would honour
 * ([PriceBook.freshListings]). `hasPrices` is never read: only the date decides whether an issue is
 * asked again (ADR 0028 §4).
 */
fun showcaseCallCount(
    plate: ShowcasePlate,
    book: PriceBook,
    nowMillis: Long,
): Int = valuationCallCount(
    plan = showcaseValuationPlan(plate),
    reads = book.readAt.map { (issue, readAt) ->
        IssuePriceReadEntity(issue.first, issue.second, readAt, hasPrices = true)
    },
    nowMillis = nowMillis,
    listings = book.freshListings(nowMillis),
)

/**
 * The empty casillas of a plate that have a closing cost: none if it is closed or further than
 * [HOLE_THRESHOLD_SLOTS] from closing. Shared by the pass and the plate header (#493).
 */
fun holesWithinReach(album: CollectionCatalogAlbum): List<CollectionCatalogAlbumMember> {
    val missing = album.members.filter { it.status is CollectionCatalogMemberStatus.Missing }
    return if (holesAreWithinReach(missing.size)) missing else emptyList()
}

private fun plateHoles(
    catalog: CollectionCatalog,
    album: CollectionCatalogAlbum?,
): List<PlateHole> = holesWithinReach(album ?: return emptyList())
    .mapNotNull { hole ->
        val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
        PlateHole(catalog.id, typeId, hole.member.year, hole.member.numistaIssueIds)
    }

/**
 * The owned issues missing from the phone or older than [PRICE_LIFETIME_MILLIS]. Expired rows are
 * asked again but never deleted (ADR 0028 §5). An issue Numista answered without prices counts as
 * read, or it would be asked on every pass.
 */
fun ownedIssuesToAsk(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): List<OwnedIssue> {
    val fresh = freshIssues(reads, nowMillis)
    return plan.owned.filterNot { (it.typeId to it.issueId) in fresh }
}

/**
 * What this phone knows of Numista's issue listings (#452), so a hole whose file names no issue can
 * find the price it already holds without listing its type again.
 *
 * A type listed with no issue for the hole's year still counts as listed, or an empty answer would be
 * asked again forever. [of] drops expired listings, because the pass is about to replace them and
 * must not price the wrong issue first; [held] keeps them, because a screen spends nothing and an
 * expired row is still shown (ADR 0028 §5).
 */
data class IssueListings(
    /** The types this phone has listed, whatever the listing said. */
    val listedTypeIds: Set<Int> = emptySet(),
    /** The issue for a year of a type, per the stored listings. */
    val issueIdByTypeAndYear: Map<Pair<Int, Int>, Int> = emptyMap(),
) {
    /**
     * The issue a hole is priced by: the first its curated file declares (ADR 0014), else what a
     * stored listing answered for its year. Several declared issues share one casilla (the curved and
     * straight nine of the 1969 peseta), and closing it costs one of them.
     */
    fun issueOf(hole: PlateHole): Int? = issueOf(hole.typeId, hole.year, hole.issueIds)

    /**
     * The same rule for an album's empty casilla, so a plate header totals the issues the pass
     * priced (#493).
     */
    fun issueOf(member: CollectionCatalogMember): Int? =
        member.numistaTypeId?.let { issueOf(it, member.year, member.numistaIssueIds) }

    private fun issueOf(typeId: Int, year: Int?, declared: List<Int>): Int? =
        declared.firstOrNull() ?: year?.let { issueIdByTypeAndYear[typeId to it] }

    companion object {
        /** What the phone holds before its first pass. */
        val EMPTY: IssueListings = IssueListings()

        /**
         * The listings still fresh by [LISTING_LIFETIME_MILLIS]. [issues] must come in Numista's
         * order: a year can have several issues and the first match wins, as it did in the pass that
         * listed the type.
         */
        fun of(
            reads: Collection<TypeIssueReadEntity>,
            issues: Collection<TypeIssueEntity>,
            nowMillis: Long,
        ): IssueListings = over(
            listed = reads
                .filter { nowMillis - it.readAt < LISTING_LIFETIME_MILLIS }
                .map { it.typeId }
                .toSet(),
            issues = issues,
        )

        /** Every stored listing, expired included, for screens (#493). */
        fun held(
            reads: Collection<TypeIssueReadEntity>,
            issues: Collection<TypeIssueEntity>,
        ): IssueListings = over(reads.map { it.typeId }.toSet(), issues)

        private fun over(
            listed: Set<Int>,
            issues: Collection<TypeIssueEntity>,
        ): IssueListings = IssueListings(
            listedTypeIds = listed,
            issueIdByTypeAndYear = buildMap {
                for (issue in issues.filter { it.typeId in listed }) {
                    // Indexed by both the Hijri and the Gregorian year, so a plate built on either
                    // finds the issue.
                    issue.year?.let { putIfAbsent(issue.typeId to it, issue.issueId) }
                    issue.gregorianYear?.let { putIfAbsent(issue.typeId to it, issue.issueId) }
                }
            },
        )
    }
}

/**
 * The holes whose type still has to be listed, grouped by type: one `/types/{id}/issues` answers
 * every year of it. Holes whose file names their issues, whose price is fresh or whose type is
 * already listed are left out; [resolvedHoleIssues] prices the ones with a known issue.
 */
fun holeIssuesToAsk(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
    listings: IssueListings = IssueListings.EMPTY,
): Map<Int, List<PlateHole>> {
    val fresh = freshIssues(reads, nowMillis)
    return plan.holes
        .filter { hole -> hole.typeId !in listings.listedTypeIds }
        .filter { hole -> hole.issueIds.none { (hole.typeId to it) in fresh } }
        // A hole whose curated file names an issue is addressable without a listing.
        .filter { hole -> hole.issueIds.isEmpty() }
        .filter { hole -> hole.year != null }
        .groupBy { it.typeId }
}

/**
 * The holes whose issue is already known through [IssueListings.issueOf] and whose price is not
 * fresh, so they cost one call. Separate from [holeIssuesToAsk]: that one answers which types to
 * list, this one which issues to price now.
 */
fun resolvedHoleIssues(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
    listings: IssueListings = IssueListings.EMPTY,
): List<OwnedIssue> {
    val fresh = freshIssues(reads, nowMillis)
    return plan.holes
        .mapNotNull { hole ->
            listings.issueOf(hole)?.let { OwnedIssue(hole.typeId, it) }
        }
        .distinct()
        .filterNot { (it.typeId to it.issueId) in fresh }
}

/**
 * The `(typeId, issueId)` pairs whose price is on the phone and not expired. Public because the pass
 * checks a listing that just arrived against it.
 */
fun freshIssues(
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): Set<Pair<Int, Int>> = reads
    .filter { nowMillis - it.readAt < PRICE_LIFETIME_MILLIS }
    .map { it.typeId to it.issueId }
    .toSet()

/**
 * How many calls a pass would spend now, as the settings line says: one per owned issue, per hole
 * with a known issue, per type to list and per hole of those types. An upper bound: a listing with
 * no matching year spends no price call.
 */
fun valuationCallCount(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
    listings: IssueListings = IssueListings.EMPTY,
): Int {
    val lookups = holeIssuesToAsk(plan, reads, nowMillis, listings)
    return ownedIssuesToAsk(plan, reads, nowMillis).size +
        resolvedHoleIssues(plan, reads, nowMillis, listings).size +
        lookups.size +
        lookups.values.sumOf { it.size }
}
