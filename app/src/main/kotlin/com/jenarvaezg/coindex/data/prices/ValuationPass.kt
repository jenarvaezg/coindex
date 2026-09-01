package com.jenarvaezg.coindex.data.prices

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

/** Why the prices are not being brought right now. Each one is said in the settings screen. */
enum class ValuationRefusal {
    /** A sync is in flight, and the two spend the **same** budget (ADR 0028 §6). */
    Syncing,

    /** The freshly installed app before onboarding, which is not an error to discover. */
    NoApiKey,

    /** The month's allowance is gone. The pass writes nothing and settings says why. */
    BudgetExhausted,

    /** Numista could not be reached. The next pass retries; nothing was written (ADR 0025). */
    Offline,

    /**
     * Numista is turning the calls away, and the pass stopped instead of spending the month at it (#560).
     *
     * Three answers arrive here. A `429` is Numista throttling the key; a `403` is the quota **it**
     * counts — 2.000 a month against the 1.500 of the local gate (ADR 0003), which cannot see what
     * another phone spent of the same key. And a run of answers that leave no row, whatever their
     * status, because a pass that writes nothing five times running is not meeting bad luck.
     */
    Rejected,
}

/**
 * What this phone holds of the collection's prices.
 *
 * @param wanted how many issues the collection has to be valued by.
 * @param missing how many of those have never been answered for, or were answered more than thirty
 *   days ago. **This is the gate on the money section**: while it is not zero the total would be
 *   `max(silver, paid)`, which is 60 % of the real figure and therefore false rather than incomplete
 *   (ADR 0028 §7).
 * @param spotRead when the silver spot on this phone was last read, or null if it never was.
 * @param held why nothing is being brought at this moment, or null while it is.
 */
data class ValuationStatus(
    val wanted: Int = 0,
    val missing: Int = 0,
    val spotRead: Long? = null,
    val held: ValuationRefusal? = null,
) {
    /**
     * Whether the market has finished arriving, which is the one question the money section asks.
     *
     * True with nothing left to ask, and that includes the collection whose pieces carry no issue at
     * all: there is no market coming for it, so there is nothing to wait for either.
     */
    val settled: Boolean get() = missing == 0

    /**
     * Whether the missing money is worth a line on a notebook screen (#519).
     *
     * The money section is absent and not zero (ADR 0028 §7), and until now that absence was
     * *silent* everywhere but settings — which left the reader of «Las cifras» with a page that
     * promises «lo que vale» and then has no money on it, and no explanation either.
     *
     * **Not every absence is worth saying**, and this is where the two are told apart. With a pass
     * on its way or a sync in front of it the money is arriving on its own, in seconds, with nobody
     * doing anything: a line that appears and disappears by itself is furniture. The three that
     * are said are the ones waiting on the collector or on the calendar — no network, no
     * credentials, the month's allowance gone.
     *
     * It does **not** say which of the three, and that is deliberate: those five sentences are
     * settings' own, and ADR 0026 §5 exempts settings from the pruning precisely on the promise
     * that none of its explanations appear on a notebook screen.
     */
    val waiting: Boolean
        get() = !settled && held != null && held != ValuationRefusal.Syncing
}

/** How often the collector-visible count is updated while the pass runs. */
const val VALUATION_PROGRESS_EVERY: Int = 25

/**
 * Whatever brings Numista's catalog prices onto the phone (ADR 0028).
 *
 * An interface for the same reason [com.jenarvaezg.coindex.data.photos.PhotoPrefetch] is one: the real
 * one needs a network and a database, and the *rules* around it — when a pass is worth starting, who
 * gives the network back to a sync — are [ValuationLoop]'s.
 */
interface ValuationPass {
    suspend fun run(
        plan: ValuationPlan,
        held: ValuationRefusal?,
        onStatus: (ValuationStatus) -> Unit = {},
    ): ValuationStatus
}

/**
 * Asks Numista for the prices of the issues the collector owns and of the holes within reach.
 *
 * Four properties, and they are the same four the photograph prefetch holds itself to (ADR 0024),
 * which is what makes this its sibling rather than a new mechanism:
 *
 * - **It only asks for what is missing or expired.** With everything cached the second launch of a
 *   month costs nothing at all, which is what makes «every launch» affordable.
 * - **Three states and not two.** A price is stored; an issue Numista answered for and had **no** price
 *   for is stored as a datum, or those 19 issues of his 223 would be asked for again for ever; a
 *   failure writes nothing and is retried next time.
 * - **It is resumable.** One issue is one row written in one transaction, so being cut short costs only
 *   the calls not yet made.
 * - **It is silent.** One line in the settings screen, and only because «faltan y están cayendo» and
 *   «faltan porque no hay red» need different things from the collector.
 *
 * The spot is read first and **outside all of that**: it is two keyless calls to hosts that are not
 * `api.numista.com`, so it is not counted against the budget of ADR 0003 and it is not held back by a
 * refusal that is about the budget.
 */
class NumistaValuationPass(
    private val prices: PriceDao,
    private val client: () -> NumistaClient?,
    private val spot: SpotStore,
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

        val now = nowMillis()
        val reads = prices.reads()
        val listings = storedListings(now)
        val issues = ownedIssuesToAsk(plan, reads, now) +
            resolvedHoleIssues(plan, reads, now, listings)
        var asked = 0
        var stopped: ValuationRefusal? = null
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
        // Counted from the table again rather than from what landed: an issue that failed is still
        // missing, and one that answered with no prices has stopped being missing without a price.
        status(plan, spot.stored()?.readAtMillis, stopped)
    }

    /**
     * Lists the issues of each type still to be looked up, and prices the year each hole wants.
     *
     * One listing per **type** and not per hole: a plate's holes are years of one type nine times out
     * of ten, and one `/types/{id}/issues` answers all of them.
     *
     * **The listing is written down before anything is priced** (#452). It used to be spent and
     * thrown away, on the grounds that «this type has no 1904» is a claim about the catalogue and not
     * about a price — true, and it is not what is stored: what is stored is that *this phone* has
     * read the listing, which is the only thing that stops it reading it again on the next pass, and
     * on every pass after that. Over the father's collection that was 102 lookups per cold start.
     *
     * A hole whose price is already fresh is skipped rather than re-priced, which is the other half
     * of the same bill: 111 prices he had already paid for.
     */
    private suspend fun askHoles(
        numista: NumistaClient,
        lookups: Map<Int, List<PlateHole>>,
        fresh: Set<Pair<Int, Int>>,
        streak: BarrenStreak,
    ): ValuationRefusal? {
        for ((typeId, holes) in lookups) {
            val listing = try {
                // Only the issues that can be addressed: an entry Numista lists with no id of its own
                // is not a candidate, and it must not be one here either — `storeListing` drops it,
                // so counting it as the match would make this pass and the next one disagree about
                // which issue a hole is priced by, and pay for both.
                numista.fetchIssues(typeId).value.filter { it.id != null }
            } catch (error: NumistaException) {
                // The listing is a call like any other, so it counts towards the streak: a type that
                // could not be listed wrote no row either.
                val stop = refusalFor(error) ?: streak.barren()
                if (stop != null) return stop
                continue
            }
            storeListing(typeId, listing)
            streak.stored()
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
     * Asks about one issue and writes what came back, or returns why the pass has to stop.
     *
     * A `404` is **not** a stop and not a failure: it is Numista saying it has no prices for this
     * issue, which is a datum and is stored as one — the same reading ADR 0024 gives a photograph's
     * `404`. Anything else that is not about the budget, the network or a refusal is skipped without a
     * row, and the next pass tries again — unless [BarrenStreak] has seen enough of them in a row to
     * call it a wall.
     */
    private suspend fun askOne(
        numista: NumistaClient,
        typeId: Int,
        issueId: Int,
        streak: BarrenStreak,
    ): ValuationRefusal? {
        val answer = try {
            numista.fetchIssuePrices(typeId, issueId).value
        } catch (error: NumistaException) {
            if (error is NumistaException.Api && error.status == HTTP_NOT_FOUND) {
                store(typeId, issueId, IssuePricesResponse())
                streak.stored()
                return null
            }
            return refusalFor(error) ?: streak.barren()
        }
        store(typeId, issueId, answer)
        streak.stored()
        return null
    }

    /** What the phone has already listed and has not expired, read once per pass (#452). */
    private suspend fun storedListings(now: Long): IssueListings =
        IssueListings.of(prices.typeIssueReads(), prices.typeIssues(), now)

    /**
     * Writes down one type's listing, empty answer included.
     *
     * An empty listing is as much a datum as an empty price: it is the answer that says this phone
     * has nothing left to ask about this type, and without the row the lookup comes back for ever.
     */
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
private const val HTTP_FORBIDDEN = 403
private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * How many answers in a row may leave no row before the pass reads them as a wall (#560).
 *
 * **Five**, and the number is a floor and a ceiling at once. Below it a plan of 442 calls would stop
 * on a run of bad luck that is real — a body Numista serialised wrong, one `500` while a shard of
 * theirs restarts — and stopping there costs the collector a month of prices for nothing, because
 * nothing on the phone will retry until the next launch. Above it the wall is charged for: every
 * answer past the fifth is a call of the month's allowance spent to learn what the fifth already
 * said. Five is 1 % of a cold plan of 442, which is the most a wrong guess in either direction can
 * cost: five calls thrown at a wall, or one healthy pass cut short and resumed at the next launch.
 *
 * The streak counts **rows written and not statuses**, which is what keeps a `404` out of it: an issue
 * Numista has no price for is answered, stored and forgotten (ADR 0028 §4), so a collection of nothing
 * but those still costs one call each, once, and never trips this.
 */
internal const val BARREN_STREAK_LIMIT: Int = 5

/**
 * The run of answers that left no row, which is how the pass tells one absence from a refusal.
 *
 * One per pass and never a field of the pass itself: a streak that survived from one pass to the next
 * would stop a healthy one on its first stumble.
 */
private class BarrenStreak {
    private var run = 0

    /** An answer that landed — a price, an empty price, a listing. The run is over. */
    fun stored() {
        run = 0
    }

    /** An answer that wrote nothing: null to carry on, or the refusal that stops the pass. */
    fun barren(): ValuationRefusal? {
        run++
        return if (run >= BARREN_STREAK_LIMIT) ValuationRefusal.Rejected else null
    }
}

/**
 * Which refusals stop a whole pass, and which are one issue's bad luck.
 *
 * The budget stops it because every further call would throw the same way, and the network stops it
 * because four hundred timeouts in a row is two minutes of a dead radio. **The `429` and the `403`
 * stop it for the first of those reasons and not for a new one** (#560): a throttled key is throttled
 * for the next call too, and a `403` is either the key being refused or Numista's own quota gone —
 * neither of which the next issue is going to fix. A malformed body or an unexpected status is this
 * issue's problem alone: null means «skip it and carry on», and it is [BarrenStreak] that decides how
 * many of those in a row stop being one issue's problem.
 */
private fun refusalFor(error: NumistaException): ValuationRefusal? = when (error) {
    is NumistaException.BudgetExhausted -> ValuationRefusal.BudgetExhausted
    is NumistaException.Transport -> ValuationRefusal.Offline
    is NumistaException.Api -> when (error.status) {
        HTTP_TOO_MANY_REQUESTS, HTTP_FORBIDDEN -> ValuationRefusal.Rejected
        else -> null
    }
    else -> null
}
