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
        for (issue in issues) {
            stopped = askOne(numista, issue.typeId, issue.issueId)
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
    ): ValuationRefusal? {
        for ((typeId, holes) in lookups) {
            val listing = try {
                // Only the issues that can be addressed: an entry Numista lists with no id of its own
                // is not a candidate, and it must not be one here either — `storeListing` drops it,
                // so counting it as the match would make this pass and the next one disagree about
                // which issue a hole is priced by, and pay for both.
                numista.fetchIssues(typeId).value.filter { it.id != null }
            } catch (error: NumistaException) {
                return refusalFor(error) ?: continue
            }
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
                askOne(numista, typeId, issueId)?.let { return it }
            }
        }
        return null
    }

    /**
     * Asks about one issue and writes what came back, or returns why the pass has to stop.
     *
     * A `404` is **not** a stop and not a failure: it is Numista saying it has no prices for this
     * issue, which is a datum and is stored as one — the same reading ADR 0024 gives a photograph's
     * `404`. Anything else that is not about the budget or the network is skipped without a row, and
     * the next pass tries again.
     */
    private suspend fun askOne(
        numista: NumistaClient,
        typeId: Int,
        issueId: Int,
    ): ValuationRefusal? {
        val answer = try {
            numista.fetchIssuePrices(typeId, issueId).value
        } catch (error: NumistaException) {
            if (error is NumistaException.Api && error.status == HTTP_NOT_FOUND) {
                store(typeId, issueId, IssuePricesResponse())
                return null
            }
            return refusalFor(error)
        }
        store(typeId, issueId, answer)
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

/**
 * Which refusals stop a whole pass, and which are one issue's bad luck.
 *
 * The budget stops it because every further call would throw the same way, and the network stops it
 * because four hundred timeouts in a row is two minutes of a dead radio. A malformed body or an
 * unexpected status is this issue's problem alone: null means «skip it and carry on».
 */
private fun refusalFor(error: NumistaException): ValuationRefusal? = when (error) {
    is NumistaException.BudgetExhausted -> ValuationRefusal.BudgetExhausted
    is NumistaException.Transport -> ValuationRefusal.Offline
    else -> null
}
