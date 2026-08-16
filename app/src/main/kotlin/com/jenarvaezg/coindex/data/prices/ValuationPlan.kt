package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbum
import com.jenarvaezg.coindex.domain.CollectionCatalogAlbumMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMember
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Curation
import com.jenarvaezg.coindex.domain.ShowcasePlate
import com.jenarvaezg.coindex.domain.WishedSlot
import com.jenarvaezg.coindex.domain.buildCollectionCatalogAlbum

/**
 * How many slots from closing a plate has to be for its holes to be worth valuing (ADR 0028 §1).
 *
 * **Not a saving, a rule.** ADR 0026 §10 wrote that the cost of completing a plate is actionable per
 * plate and a reproach once totalled, and this is that read forwards: a plate with 51 holes does not
 * have a cost of completion, it has a reproach of 51 slots. It is not that they are expensive to ask
 * about; it is that the number that came back could not be shown.
 *
 * Where it falls, over the father's 49 plates: at 1-3 it is 12 plates and 28 holes; at **1-10** it is
 * 28 plates and 138 holes; at 1-15, 34 plates and 221 holes. And it falls clean — what stays outside
 * are the bullion runs where he owns a single coin.
 */
const val HOLE_THRESHOLD_SLOTS: Int = 10

/**
 * Whether a plate's holes are a cost of closing it, or the reproach of §1.
 *
 * The one reading of [HOLE_THRESHOLD_SLOTS], asked by the two places that must agree about it: the
 * pass, which decides whose prices to spend calls on, and the plate's header, which decides whether
 * it has a second figure to print (#493). Written twice they would drift, and what would be seen is a
 * plate that says a cost of closing built out of prices nobody ever asked for.
 *
 * **Zero leaves with the same clause**: a plate already closed has nothing to cost, and no cost of
 * zero to redact either.
 */
fun holesAreWithinReach(holes: Int): Boolean = holes in 1..HOLE_THRESHOLD_SLOTS

/** A catalog price is read again after thirty days. Catalog prices move slowly (ADR 0028 §5). */
const val PRICE_LIFETIME_MILLIS: Long = 30L * 24 * 60 * 60 * 1_000

/**
 * A type's issue listing is read again after ninety days (#452).
 *
 * Three times a price's life, because the catalogue moves slower than the market: which issue is the
 * 1905 of a type is a fact that only changes when Numista publishes a new one. Not «never», though —
 * an open date run grows a slot every January, and a listing that never expired would leave that new
 * hole silently unpriceable for the life of the phone. Over the father's 102 listed types this is
 * about one lookup a day amortised, against the 102 per cold start it replaces.
 */
const val LISTING_LIFETIME_MILLIS: Long = 90L * 24 * 60 * 60 * 1_000

/** One issue the collector owns a piece of, addressable straight away. */
data class OwnedIssue(val typeId: Int, val issueId: Int)

/**
 * One empty slot of a plate within reach of closing.
 *
 * [issueIds] is what the curated file already declares — an issue run names its issues (ADR 0014), so
 * those holes cost **one** call and not two. When it is empty the issue has to be looked up by
 * [year] through `/types/{id}/issues`, which is the second call the threshold exists to ration.
 */
data class PlateHole(
    val catalogId: String,
    val typeId: Int,
    val year: Int?,
    val issueIds: List<Int> = emptyList(),
)

/**
 * Everything one pass may ask Numista about, before anything already on the phone is subtracted.
 *
 * The two halves are counted apart because they cost differently and were decided differently: the
 * owned issues are 223 calls and are the whole of the money the page shows, and the holes are 264 more
 * and are the cost of closing a plate, which lives in the plate's header and never totalled.
 */
data class ValuationPlan(val owned: List<OwnedIssue>, val holes: List<PlateHole>) {
    val isEmpty: Boolean get() = owned.isEmpty() && holes.isEmpty()
}

/**
 * What this collection gives the pass to ask about (ADR 0028 §1).
 *
 * The owned half is every piece that carries an issue id, which since `Mappers.issueIdFromRaw` is
 * every piece the collector recorded an issue for — no migration and no extra call to find out.
 *
 * The holes half walks only the plates that are **open**: a catalog with no evidence has no plate to
 * put a cost of completion in the header of, so asking about its slots would be spending the budget on
 * a screen the collector cannot reach.
 */
fun valuationPlan(
    items: List<CollectedItem>,
    curation: Curation,
    evidencedCatalogIds: Set<String>,
    /**
     * The casillas the collector marked, which enter the plan **whatever their plate's shape**
     * (ADR 0029 §4).
     *
     * A mark lifts both filters above, and each for its own reason. The threshold of §1 is a rule
     * about whether a number deserves to be shown, and a mark answers exactly that question — of the
     * 51 holes, this one — so a marked slot of a plate past the threshold is priced and the plate's
     * own «Coste de cerrar» still is not. And the evidence filter goes too, which is #282's decision
     * 1 narrowed by name: what the collector marks gets priced, wherever it comes from, so the father
     * never has to work out which régime his coin is under.
     */
    wishes: List<WishedSlot> = emptyList(),
): ValuationPlan = ValuationPlan(
    owned = items
        .mapNotNull { item -> item.issueId?.let { OwnedIssue(item.typeId, it) } }
        .distinct(),
    // Distinct because the two halves overlap on purpose: marking a hole of a plate already within
    // reach must not ask for its price twice, and it is the same hole either way.
    holes = (
        curation.catalogs
            .filter { it.id in evidencedCatalogIds }
            .flatMap { catalog -> plateHoles(catalog, items) } + wishHoles(wishes)
        ).distinct(),
)

/**
 * The marked casillas as holes of the plan (ADR 0029 §4).
 *
 * Built off the resolved slot and not off the wish row, so the issues it addresses a price to are the
 * curated file's — the same ones the plate's own holes carry, which is what keeps a marked hole of a
 * plate within reach from being asked about twice.
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
 * What the marked casillas cost a month, which is the figure Ajustes prints beside the budget.
 *
 * **The ceiling of a cold month and not an average**, and it is measured with the same arithmetic the
 * pass itself uses — an empty phone, nothing read yet — rather than with a second formula: one
 * `/prices` per marked hole, plus one `/types/{id}/issues` per type whose curated file names no issue.
 * That is the «1-2 llamadas por deseo» of ADR 0029 §5 read exactly, and it is where the gesture's «+2
 * consultas al mes» comes from.
 *
 * It really is monthly for the prices, which expire in thirty days (ADR 0028 §5), and generous for the
 * listings, which last ninety: a marked slot that needed a lookup costs it once a quarter and is
 * counted here every month. Rounding a spend **up** is the direction this number has to err in.
 */
fun wishCallsPerMonth(wishes: List<WishedSlot>): Int = valuationCallCount(
    plan = ValuationPlan(owned = emptyList(), holes = wishHoles(wishes)),
    reads = emptyList(),
    nowMillis = 0L,
)

/**
 * Everything one plate of the shelf window would be asked about, and nothing else (ADR 0030 §3).
 *
 * The plan of a **gesture** and not of a pass: it holds no owned issue — the collector owns nothing of
 * this catalog, which is what put it in the window — and it holds **every** empty casilla rather than
 * the ones within reach, because the threshold of ADR 0028 §1 is the rule of the reproach and a plate
 * that is not yours reproaches nothing (ADR 0030 §7).
 *
 * It is fed to the same [ValuationPass] the launch pass uses, which is what keeps there from being a
 * second writer, a second table or a second price: the rows land in `issue_prices` and
 * `issue_price_reads`, and a marked casilla of this plate is refreshed by the monthly pass afterwards
 * exactly as ADR 0029 §4 requires.
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
 * What tasar this plate would spend right now, which is what the gesture prints (ADR 0030 §3).
 *
 * **The pass's own arithmetic and not a second one**: [valuationCallCount] is what a settings line
 * already says about the month, and a gesture that counted its calls its own way would promise a number
 * the pass then did not spend. What it takes is the book a **screen** holds — [PriceBook.readAt] is the
 * same `issue_price_reads` the pass queries — because this is asked while the collector is looking at
 * the plate and must not open the database to say a word. And the listings it discounts are
 * [PriceBook.freshListings]'s, the ones the pass would honour: a type listed four months ago is listed
 * for the screen and not for the spend, and counting it would print a ceiling under what the pass then
 * spends (ADR 0030 §3).
 *
 * `hasPrices` is filled with `true` and never read: what decides whether an issue is asked about again
 * is its date alone (see [freshIssues]), and an issue Numista answered with no price is as much on the
 * phone as a priced one (ADR 0028 §4).
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
 * The empty casillas of a plate that are a cost of closing rather than the reproach of §1.
 *
 * The **one** walk of an album's holes, asked by the two places that have to agree about them: the
 * pass, which decides whose prices to spend calls on, and the plate's header, which adds up the
 * prices that came back (#493). Empty for a closed plate and empty over [HOLE_THRESHOLD_SLOTS], which
 * are the two ways of having no cost to say — see [holesAreWithinReach].
 */
fun holesWithinReach(album: CollectionCatalogAlbum): List<CollectionCatalogAlbumMember> {
    val missing = album.members.filter { it.status is CollectionCatalogMemberStatus.Missing }
    return if (holesAreWithinReach(missing.size)) missing else emptyList()
}

private fun plateHoles(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
): List<PlateHole> = holesWithinReach(buildCollectionCatalogAlbum(catalog, items))
    .mapNotNull { hole ->
        val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
        PlateHole(catalog.id, typeId, hole.member.year, hole.member.numistaIssueIds)
    }

/**
 * The issues of the plan that are missing from the phone or older than thirty days.
 *
 * **Expired is asked again but never deleted** (ADR 0028 §5): this decides what to *ask for*, and the
 * row it leaves behind keeps being the one the page reads until a newer answer replaces it.
 *
 * An issue Numista answered for and had no price for is on the phone as much as a priced one — it is a
 * row of `issue_price_reads` with no prices — which is what stops those 19 issues of his 223 being
 * asked about again on every single pass, for ever.
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
 * What this phone already knows of Numista's issue listings (#452).
 *
 * The listing was the one call of the three that left no trace: `issue_price_reads` recorded what
 * `/prices` answered and nothing recorded what `/types/{id}/issues` did, so a hole whose curated file
 * does not name its issue — 111 of the father's 121 — could never recognise the price it already
 * held, and its type was listed again on every pass. Measured over his collection, that was 102
 * listings and 111 prices on every cold start: 213 of the 1.999 calls Numista let him make in August.
 *
 * **Two fields and not one**, for the reason [IssuePriceReadEntity] is two tables: a type Numista
 * listed with no issue matching the hole's year still counts as listed, or an empty answer is
 * indistinguishable from an unasked one and costs the lookup for ever.
 *
 * **Expiry is the factory's business and not this type's** (#493). [of] holds what is still fresh by
 * [LISTING_LIFETIME_MILLIS], because the pass must not answer a hole from a stale map: that listing is
 * about to be replaced in this same pass, and it would price the wrong issue and then the right one,
 * paying twice for the mistake. [held] keeps every row the phone has, because a screen has nothing to
 * spend and ADR 0028 §5 does not empty a page on expiry. The two readings differ in one line, and it
 * is one line rather than two types so that neither can grow a rule the other does not have.
 */
data class IssueListings(
    /** The types this phone has listed, whatever the listing said. */
    val listedTypeIds: Set<Int> = emptySet(),
    /** Which issue is a given year of a given type, as the stored listings answered. */
    val issueIdByTypeAndYear: Map<Pair<Int, Int>, Int> = emptyMap(),
) {
    /**
     * The issue a hole is priced by: what its curated file declares, or what a stored listing
     * answered for its year.
     *
     * **One rule and one reading of it.** The file first — an issue run names its issues (ADR 0014) —
     * and the *first* of them when it names several, which is the choice the casilla itself makes: a
     * slot holding the curved and the straight nine of the 1969 peseta is one slot, and closing it
     * costs the price of one of them. A hole of neither kind has nothing to address a price to.
     */
    fun issueOf(hole: PlateHole): Int? = issueOf(hole.typeId, hole.year, hole.issueIds)

    /**
     * The same rule for an empty casilla of an album, which is what a plate's header adds up (#493).
     *
     * Read here and not in the screen that needed it: what the pass spent its calls on and what the
     * header totals have to be **the same issue**, or the plate would print a cost assembled out of
     * prices addressed to other coins.
     */
    fun issueOf(member: CollectionCatalogMember): Int? =
        member.numistaTypeId?.let { issueOf(it, member.year, member.numistaIssueIds) }

    private fun issueOf(typeId: Int, year: Int?, declared: List<Int>): Int? =
        declared.firstOrNull() ?: year?.let { issueIdByTypeAndYear[typeId to it] }

    companion object {
        /** What a caller with nothing stored passes, and what the phone holds before its first pass. */
        val EMPTY: IssueListings = IssueListings()

        /**
         * The two tables read as the one question the plan asks of them, expiry included.
         *
         * [issues] must arrive in the order Numista listed them, because **the first match wins**: a
         * year can have more than one issue, and the pass that looked the type up priced the first of
         * them. Reading it back any other way would address the price to a different issue and miss
         * the one already on the phone.
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

        /**
         * Every listing this phone holds, **expiry included**, which is what a screen reads (#493).
         *
         * A different question from [of], and the seam is what the answer is spent on. The pass must
         * not price a hole off a stale listing, because that listing is about to be replaced in this
         * same pass and the wrong issue would be paid for twice. A plate's header has nothing to
         * spend: it either says the cost of closing out of the rows already on the phone or it says
         * nothing at all — and ADR 0028 §5 settled which of the two an expired row gets, for the
         * price beside it as much as for the listing that addresses it: *expired is asked again and
         * never deleted*, and the old answer keeps being the one the page reads.
         */
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
                    // Both readings of the year reach the same issue: a plate built on the Hijri
                    // 1316 finds it, and one built on the 1899 beside it finds it too.
                    issue.year?.let { putIfAbsent(issue.typeId to it, issue.issueId) }
                    issue.gregorianYear?.let { putIfAbsent(issue.typeId to it, issue.issueId) }
                }
            },
        )
    }
}

/**
 * The holes of the plan still to ask about, grouped by type.
 *
 * Grouped because a plate's holes are years of **one** type nine times out of ten, and one
 * `/types/{id}/issues` answers every year of it: asking per hole would turn the 126 lookups his 138
 * holes need into 138.
 *
 * Three kinds of hole leave, and only the third is new: one whose curated file already names its
 * issues, one whose price is already fresh, and one whose **type has already been listed** — whether
 * or not the listing named its year. [resolvedHoleIssues] is where the first two go on to be priced.
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
        // Nothing declared, which is the same question `IssueListings.issueOf` asks of the file
        // first: a hole whose curated file names an issue is already addressable and never listed.
        .filter { hole -> hole.issueIds.isEmpty() }
        .filter { hole -> hole.year != null }
        .groupBy { it.typeId }
}

/**
 * The holes whose issue is already known, and which therefore cost one call and not two.
 *
 * Known two ways, and it is [IssueListings.issueOf] that knows them: the curated file names it — an
 * issue run declares its issues (ADR 0014) — or a stored listing answered for its year (#452). A hole
 * of neither kind has nothing to address a price to, and stays [holeIssuesToAsk]'s business until it
 * has.
 *
 * Kept as its own reading rather than folded into [holeIssuesToAsk] because the two answer different
 * questions — «which types have to be listed» against «which issues can be priced straight away» —
 * and one function returning both is a caller that has to remember which half it is holding.
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
 * The issues whose price this phone holds and has not expired, as `(typeId, issueId)`.
 *
 * Public because the pass needs it too: a listing that has just arrived can name an issue whose price
 * is already fresh, and asking for it again is the 111 calls of #452.
 */
fun freshIssues(
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): Set<Pair<Int, Int>> = reads
    .filter { nowMillis - it.readAt < PRICE_LIFETIME_MILLIS }
    .map { it.typeId to it.issueId }
    .toSet()

/**
 * How many calls a pass would spend right now, which is what the settings line says.
 *
 * One per owned issue, one per hole whose issue is already known, one per type still to be listed,
 * and one per hole of those types. It is an upper bound and not an estimate: a type whose listing
 * answers no matching year spends the lookup and no price call.
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
