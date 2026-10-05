package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.RejectionCause
import com.jenarvaezg.coindex.data.RejectionWall
import com.jenarvaezg.coindex.data.rejectionCauseFor
import com.jenarvaezg.coindex.data.db.IssuePriceEntity
import com.jenarvaezg.coindex.data.db.IssuePriceReadEntity
import com.jenarvaezg.coindex.data.db.PriceDao
import com.jenarvaezg.coindex.data.db.TypeIssueEntity
import com.jenarvaezg.coindex.data.db.TypeIssueReadEntity
import com.jenarvaezg.coindex.data.numista.IssueDto
import com.jenarvaezg.coindex.data.numista.IssuePricesResponse
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Why the prices are not being brought right now. Each one is shown in settings. */
enum class ValuationRefusal {
    /** A sync is running, and both spend the same budget (ADR 0028 §6). */
    Syncing,

    /** No key yet, before onboarding; not an error. */
    NoApiKey,

    /** The month's allowance is gone. The pass writes nothing and «Este teléfono» says why. */
    BudgetExhausted,

    /** Numista could not be reached. The next pass retries; nothing was written (ADR 0025). */
    Offline,

    /**
     * Numista is turning the calls away, and the pass stopped rather than spend the month on it
     * (#560): a `429` (throttling, or the key's monthly quota, which the local gate of ADR 0003
     * can't see when another phone shares the key), a `401` or `403` (the key refused), or
     * [BARREN_STREAK_LIMIT] answers in a row that wrote nothing.
     *
     * The label only says that Numista is refusing; whether the key is wrong is for the next sync's
     * error to say. The refusal is remembered across launches ([RejectionWall], #579), for as long as
     * its [RejectionCause] dictates.
     */
    Rejected,
}

/**
 * What this phone holds of the collection's prices.
 *
 * @param wanted how many issues the collection is valued by.
 * @param missing how many of those were never answered or are older than ninety days. While it is
 *   not zero the money section stays hidden: a partial total would be false, not incomplete
 *   (ADR 0028 §7).
 * @param spotRead when the silver spot was last read, or null if never.
 * @param held why nothing is being brought now, or null while it is.
 */
data class ValuationStatus(
    val wanted: Int = 0,
    val missing: Int = 0,
    val spotRead: Long? = null,
    val held: ValuationRefusal? = null,
) {
    /** Nothing left to ask, which includes a collection whose pieces carry no issue at all. */
    val settled: Boolean get() = missing == 0

    /**
     * Whether the missing money deserves a line on a notebook screen (#519): only when it waits on
     * the collector or the calendar (offline, no key, budget gone, Numista refusing). With a pass
     * running or a sync ahead of it, the money arrives on its own. It doesn't say which cause: those
     * sentences belong to settings (ADR 0026 §5).
     */
    val waiting: Boolean
        get() = !settled && held != null && held != ValuationRefusal.Syncing
}

/** How often, in issues, the visible count is updated while the pass runs. */
const val VALUATION_PROGRESS_EVERY: Int = 25

/**
 * Whatever brings Numista's catalog prices onto the phone (ADR 0028). An interface so the rules
 * around it, in [ValuationLoop], can be tested without network or database.
 */
interface ValuationPass {
    suspend fun run(
        plan: ValuationPlan,
        held: ValuationRefusal?,
        onStatus: (ValuationStatus) -> Unit = {},
    ): ValuationStatus
}

/**
 * Asks Numista for the prices of the owned issues and of the holes within reach, under the same
 * rules as the photograph prefetch (ADR 0024):
 *
 * - It asks only for what is missing or expired, so a launch with everything cached costs nothing.
 * - Three states: a price is stored; an issue with no prices is stored as such, or it would be asked
 *   forever; a failure writes nothing and is retried next time.
 * - Resumable: one issue is one transaction, so being cut short only loses the calls not yet made.
 * - Silent: one line in settings, to tell «still arriving» from «no network».
 *
 * The spot goes first and outside all that: two keyless calls to hosts other than `api.numista.com`,
 * neither counted against the budget (ADR 0003) nor held back by a refusal.
 */
class NumistaValuationPass(
    private val prices: PriceDao,
    private val client: () -> NumistaClient?,
    private val spot: SpotStore,
    private val wall: RejectionWall,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ValuationPass {
    override suspend fun run(
        plan: ValuationPlan,
        held: ValuationRefusal?,
        onStatus: (ValuationStatus) -> Unit,
    ): ValuationStatus = withContext(Dispatchers.IO) {
        val spotRead = spot.refresh()?.readAtMillis
        var status = status(plan, spotRead, held)
        onStatus(status)
        val numista = client()
        if (held != null || plan.isEmpty) return@withContext status
        if (numista == null) return@withContext status.copy(held = ValuationRefusal.NoApiKey)
        // After the spot, which is keyless (ADR 0028 §9), and before the first Numista call. A wall
        // still standing reports `Rejected`, like a fresh refusal.
        if (wall.standing() != null) return@withContext status.copy(held = ValuationRefusal.Rejected)

        val now = nowMillis()
        val reads = prices.reads()
        val listings = storedListings(now)
        val issues = ownedIssuesToAsk(plan, reads, now) +
            resolvedHoleIssues(plan, reads, now, listings)
        var asked = 0
        var stopped: Stop? = null
        val streak = BarrenStreak()
        for (issue in issues) {
            stopped = askOne(numista, issue.typeId, issue.issueId, streak)
            if (stopped != null) break
            asked++
            if (asked % VALUATION_PROGRESS_EVERY == 0) {
                status = status(plan, spotRead, null)
                onStatus(status)
            }
        }
        if (stopped == null) {
            stopped = askHoles(
                numista,
                holeIssuesToAsk(plan, reads, now, listings),
                freshIssues(reads, now),
                streak,
            )
        }
        // Written after the pass, so the wall stores why it stopped; a pass that got through
        // without a refusal takes the wall down.
        val cause = stopped?.cause
        if (cause != null) wall.raise(cause) else wall.clear()
        // Recounted from the table: a failed issue is still missing, and one answered without
        // prices no longer is.
        status(plan, spot.stored()?.readAtMillis, stopped?.refusal)
    }

    /**
     * Lists each type still to look up, once per type, and prices the year each hole wants. The
     * listing is stored before pricing, empty or not (#452), so the next pass doesn't list it again;
     * holes whose price is already fresh are skipped.
     */
    private suspend fun askHoles(
        numista: NumistaClient,
        lookups: Map<Int, List<PlateHole>>,
        fresh: Set<Pair<Int, Int>>,
        streak: BarrenStreak,
    ): Stop? {
        for ((typeId, holes) in lookups) {
            val listing = try {
                // Entries without an id are dropped, as `storeListing` drops them, so this pass and
                // the next pick the same issue for a hole.
                numista.fetchIssues(typeId).value.filter { it.id != null }
            } catch (error: NumistaException) {
                // A type Numista doesn't have (`404`) costs its lookup and nothing else.
                if (error is NumistaException.Api && error.status == HTTP_NOT_FOUND) continue
                val stop = stopFor(error) ?: streak.noteBarren()
                if (stop != null) return stop
                continue
            }
            // A listing neither breaks nor feeds the streak. A wall can stand in front of `/prices`
            // alone (listings 200, prices 500), and resetting here would let the pass alternate
            // stored and barren down the whole plan (#560).
            storeListing(typeId, listing)
            for (hole in holes) {
                val issueId = listing
                    .firstOrNull { issue ->
                        hole.year != null &&
                            (issue.year == hole.year || issue.gregorianYear == hole.year)
                    }
                    ?.id
                    ?: continue
                if ((typeId to issueId) in fresh) continue
                askOne(numista, typeId, issueId, streak)?.let { return it }
            }
        }
        return null
    }

    /**
     * Asks about one issue and stores the answer, or returns why the pass has to stop. A `404` means
     * Numista has no prices for the issue and is stored as such, as ADR 0024 reads a photograph's
     * `404`. Other errors that aren't budget, network or a refusal skip the issue without a row,
     * until [BarrenStreak] calls it a wall.
     */
    private suspend fun askOne(
        numista: NumistaClient,
        typeId: Int,
        issueId: Int,
        streak: BarrenStreak,
    ): Stop? {
        val answer = try {
            numista.fetchIssuePrices(typeId, issueId).value
        } catch (error: NumistaException) {
            if (error is NumistaException.Api && error.status == HTTP_NOT_FOUND) {
                store(typeId, issueId, IssuePricesResponse())
                streak.noteStored()
                return null
            }
            return stopFor(error) ?: streak.noteBarren()
        }
        store(typeId, issueId, answer)
        streak.noteStored()
        return null
    }

    /** The stored listings not yet expired, read once per pass (#452). */
    private suspend fun storedListings(now: Long): IssueListings =
        IssueListings.of(prices.typeIssueReads(), prices.typeIssues(), now)

    /** Stores one type's listing, an empty one too, or the lookup would be repeated forever. */
    private suspend fun storeListing(typeId: Int, listing: List<IssueDto>) {
        prices.putListing(
            read = TypeIssueReadEntity(typeId, nowMillis()),
            issues = listing.mapIndexedNotNull { position, issue ->
                issue.id?.let {
                    TypeIssueEntity(typeId, it, position, issue.year, issue.gregorianYear)
                }
            },
        )
    }

    private suspend fun store(typeId: Int, issueId: Int, answer: IssuePricesResponse) {
        val rows = answer.prices
            .orEmpty()
            .mapNotNull { price ->
                val grade = price.grade?.lowercase()?.takeIf(String::isNotBlank)
                    ?: return@mapNotNull null
                val eur = price.price?.takeIf { it.isFinite() && it > 0.0 } ?: return@mapNotNull null
                IssuePriceEntity(typeId, issueId, grade, eur)
            }
            .distinctBy { it.grade }
        prices.putIssue(
            read = IssuePriceReadEntity(typeId, issueId, nowMillis(), rows.isNotEmpty()),
            prices = rows,
        )
    }

    private suspend fun status(
        plan: ValuationPlan,
        spotRead: Long?,
        held: ValuationRefusal?,
    ): ValuationStatus = ValuationStatus(
        wanted = plan.owned.size,
        missing = ownedIssuesToAsk(plan, prices.reads(), nowMillis()).size,
        spotRead = spotRead,
        held = held,
    )
}

private const val HTTP_NOT_FOUND = 404

/**
 * How many answers in a row may write nothing before the pass reads them as a wall (#560). Fewer
 * would stop a month's pass on ordinary bad luck (a malformed body, a passing `500`) until the next
 * launch; more would spend calls to learn what the fifth already said.
 *
 * It counts rows written, not statuses, so `404`s (stored as no-price, ADR 0028 §4) never trip it.
 */
internal const val BARREN_STREAK_LIMIT: Int = 5

/** The run of answers that wrote nothing. One per pass, so a new pass never starts mid-streak. */
private class BarrenStreak {
    private var run = 0

    /** An answer that stored a price, or the absence of one, ends the run. */
    fun noteStored() {
        run = 0
    }

    /** Notes an answer that wrote nothing: null to carry on, or the refusal that stops the pass. */
    fun noteBarren(): Stop? {
        run++
        return if (run >= BARREN_STREAK_LIMIT) {
            Stop(ValuationRefusal.Rejected, RejectionCause.Unreadable)
        } else {
            null
        }
    }
}

/**
 * Which errors stop a whole pass and which are one issue's bad luck. Budget, network and Numista's
 * refusals (#560) stop it, because the next call would fail the same way. A malformed body or an
 * unexpected status returns null (skip and go on), and [BarrenStreak] decides when a run of those is
 * a wall. Which refusal it was, and so how long the wall stands, is [rejectionCauseFor]'s call
 * (#600): `429` is both the throttle and the quota.
 */
private fun stopFor(error: NumistaException): Stop? = when (error) {
    is NumistaException.BudgetExhausted -> Stop(ValuationRefusal.BudgetExhausted)
    is NumistaException.Transport -> Stop(ValuationRefusal.Offline)
    is NumistaException.Api ->
        rejectionCauseFor(error.status, error.body)?.let { Stop(ValuationRefusal.Rejected, it) }
    else -> null
}

/**
 * Why a pass stopped: the [refusal] goes to the settings line and the [cause] to the wall. Only
 * `Rejected` has a cause; network and budget need no memory, since the next launch can tell for free.
 */
private data class Stop(val refusal: ValuationRefusal, val cause: RejectionCause? = null)
