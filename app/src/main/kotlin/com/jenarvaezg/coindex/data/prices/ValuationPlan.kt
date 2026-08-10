package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.domain.CollectedItem
import com.jenarvaezg.coindex.domain.CollectionCatalog
import com.jenarvaezg.coindex.domain.CollectionCatalogMemberStatus
import com.jenarvaezg.coindex.domain.Curation
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

/** A catalog price is read again after thirty days. Catalog prices move slowly (ADR 0028 §5). */
const val PRICE_LIFETIME_MILLIS: Long = 30L * 24 * 60 * 60 * 1_000

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
): ValuationPlan = ValuationPlan(
    owned = items
        .mapNotNull { item -> item.issueId?.let { OwnedIssue(item.typeId, it) } }
        .distinct(),
    holes = curation.catalogs
        .filter { it.id in evidencedCatalogIds }
        .flatMap { catalog -> holesWithinReach(catalog, items) },
)

private fun holesWithinReach(
    catalog: CollectionCatalog,
    items: List<CollectedItem>,
): List<PlateHole> {
    val missing = buildCollectionCatalogAlbum(catalog, items)
        .members
        .filter { it.status is CollectionCatalogMemberStatus.Missing }
    // Zero is a plate that is already closed and has nothing to cost; over the threshold is the
    // reproach. Both leave with the same clause.
    if (missing.isEmpty() || missing.size > HOLE_THRESHOLD_SLOTS) return emptyList()
    return missing.mapNotNull { hole ->
        val typeId = hole.member.numistaTypeId ?: return@mapNotNull null
        PlateHole(catalog.id, typeId, hole.member.year, hole.member.numistaIssueIds)
    }
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
    val fresh = freshReads(reads, nowMillis)
    return plan.owned.filterNot { (it.typeId to it.issueId) in fresh }
}

/**
 * The holes of the plan still to ask about, grouped by type.
 *
 * Grouped because a plate's holes are years of **one** type nine times out of ten, and one
 * `/types/{id}/issues` answers every year of it: asking per hole would turn the 126 lookups his 138
 * holes need into 138.
 *
 * A hole whose curated file already names its issues needs no lookup at all, and it is dropped from
 * this map: its prices are asked for directly.
 */
fun holeIssuesToAsk(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): Map<Int, List<PlateHole>> {
    val fresh = freshReads(reads, nowMillis)
    return plan.holes
        .filter { hole -> hole.issueIds.none { (hole.typeId to it) in fresh } }
        .filter { hole -> hole.declaredIssue() == null }
        .filter { hole -> hole.year != null }
        .groupBy { it.typeId }
}

/**
 * The holes whose issue the curated file already declares, and which therefore cost one call.
 *
 * Kept as its own reading rather than folded into [holeIssuesToAsk] because the two answer different
 * questions — «which types have to be listed» against «which issues can be priced straight away» —
 * and one function returning both is a caller that has to remember which half it is holding.
 */
fun declaredHoleIssues(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): List<OwnedIssue> {
    val fresh = freshReads(reads, nowMillis)
    return plan.holes
        .mapNotNull { hole -> hole.declaredIssue()?.let { OwnedIssue(hole.typeId, it) } }
        .distinct()
        .filterNot { (it.typeId to it.issueId) in fresh }
}

/**
 * The issue a hole is priced by when its file names several.
 *
 * The first, and it is the same choice the plate already makes: a slot holding the curved and the
 * straight nine of the 1969 peseta is **one** slot, and the price of closing it is the price of one of
 * them, not of both.
 */
private fun PlateHole.declaredIssue(): Int? = issueIds.firstOrNull()

private fun freshReads(
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): Set<Pair<Int, Int>> = reads
    .filter { nowMillis - it.readAt < PRICE_LIFETIME_MILLIS }
    .map { it.typeId to it.issueId }
    .toSet()

/**
 * How many calls a pass would spend right now, which is what the settings line says.
 *
 * One per owned issue, one per hole whose issue is already declared, one per type still to be listed,
 * and one per hole of those types. It is an upper bound and not an estimate: a type whose listing
 * answers no matching year spends the lookup and no price call.
 */
fun valuationCallCount(
    plan: ValuationPlan,
    reads: Collection<IssuePriceReadEntity>,
    nowMillis: Long,
): Int {
    val lookups = holeIssuesToAsk(plan, reads, nowMillis)
    return ownedIssuesToAsk(plan, reads, nowMillis).size +
        declaredHoleIssues(plan, reads, nowMillis).size +
        lookups.size +
        lookups.values.sumOf { it.size }
}
