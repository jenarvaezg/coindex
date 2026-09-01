package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.FakePriceDao
import com.jenarvaezg.coindex.data.db.MetalSpotEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val NOW = 1_754_600_000_000L

/**
 * The pass itself: **three states and not two**, and a failure that writes nothing (ADR 0028 §4).
 *
 * Nothing here touches the network. What is being pinned is the reading of each answer, which is where
 * confusing «Numista has no price for this» with «the call failed» would cost 19 of his 223 issues a call
 * on every pass for the life of the phone.
 */
class ValuationPassTest {
    private val prices = FakePriceDao()
    private val asked = mutableListOf<String>()

    /**
     * Which absences the notebook screens are allowed to mention, and which stay silent (#519).
     *
     * The three that are said wait on the collector or on the calendar; the two that are not fix
     * themselves in seconds while nobody does anything, and a line that appears and vanishes by
     * itself is the furniture ADR 0026 §5 prices.
     */
    @Test
    fun `only an absence nobody can wait out is worth saying`() {
        val held = { refusal: ValuationRefusal? ->
            ValuationStatus(wanted = 10, missing = 4, held = refusal).waiting
        }

        assertTrue(held(ValuationRefusal.Offline))
        assertTrue(held(ValuationRefusal.BudgetExhausted))
        assertTrue(held(ValuationRefusal.NoApiKey))
        assertTrue(held(ValuationRefusal.Rejected))
        assertFalse(held(ValuationRefusal.Syncing))
        assertFalse(held(null))
    }

    /** With nothing left to ask there is a money section, so there is no absence to explain. */
    @Test
    fun `a settled pass is never waiting, whatever is holding it`() {
        val settled = ValuationStatus(
            wanted = 10,
            missing = 0,
            held = ValuationRefusal.Offline,
        )

        assertTrue(settled.settled)
        assertFalse(settled.waiting)
    }

    /** Numista answering with prices: the row and its grades are stored. */
    @Test
    fun `a priced issue is stored grade by grade`() = runTest {
        val pass = pass(PRICED)

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(
            listOf("vf" to 25.1, "unc" to 39.6),
            prices.prices.value.map { it.grade to it.eur },
        )
        assertTrue(prices.reads.value.single().hasPrices)
        assertEquals(0, status.missing)
        assertTrue(status.settled)
    }

    /**
     * Numista answering **without** prices is a datum, and it is stored as one.
     *
     * The row has no prices and the issue has stopped being missing. Without it those 19 issues would be
     * asked for again on every pass, for ever.
     */
    @Test
    fun `an issue Numista has no price for is stored as a datum`() = runTest {
        val pass = pass(EMPTY_PRICES)

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.prices.value.isEmpty())
        assertEquals(false, prices.reads.value.single().hasPrices)
        assertEquals(0, status.missing, "una emisión contestada deja de faltar aunque no traiga precio")
    }

    /** A `404` is Numista saying it has none, which is the same datum by another route. */
    @Test
    fun `a 404 is read as no price and not as a failure`() = runTest {
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })

        pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(false, prices.reads.value.single().hasPrices)
    }

    /**
     * A dead network **writes nothing**, and the next pass retries: ADR 0025's «a refresh that fails is
     * never worse than not having asked», read the right way round.
     */
    @Test
    fun `a network failure writes no row at all`() = runTest {
        val pass = pass(handler = { throw java.io.IOException("sin red") })

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.reads.value.isEmpty())
        assertTrue(prices.prices.value.isEmpty())
        assertEquals(ValuationRefusal.Offline, status.held)
        assertEquals(1, status.missing)
    }

    /**
     * With the budget gone the pass stops on the spot and writes nothing.
     *
     * It stops rather than skipping, because every further call would throw the same way — and the
     * settings line is what turns that into a sentence the collector can act on.
     */
    @Test
    fun `an exhausted budget stops the pass and writes nothing`() = runTest {
        val pass = pass(PRICED, budget = { throw NumistaException.BudgetExhausted(1_500, 1_500) })

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertTrue(prices.reads.value.isEmpty())
        assertEquals(ValuationRefusal.BudgetExhausted, status.held)
        assertTrue(asked.isEmpty(), "el presupuesto se reserva antes de la llamada, así que no sale ninguna")
    }

    /**
     * The `429` stops the pass on its first answer, and that is the whole of #560.
     *
     * Nine passes of the same plan on 11 August 2026 spent 1.484 calls of the father's key and wrote
     * **zero** rows: every one of those answers was read as «this issue has no price, carry on», so the
     * month's allowance went against a wall one call at a time. A throttled key answers the second call
     * exactly as it answered the first, which is the reading the budget has always had.
     */
    @Test
    fun `a throttled key costs one call and not the plan`() = runTest {
        val pass = pass(handler = { respond("", HttpStatusCode.TooManyRequests, JSON) })

        val status = pass.run(plan(*(1..20).map { OwnedIssue(30, it) }.toTypedArray()), held = null)

        assertEquals(1, asked.size, "una consulta, no las veinte del plan")
        assertEquals(ValuationRefusal.Rejected, status.held)
        assertTrue(prices.reads.value.isEmpty())
    }

    /**
     * And so does the `403`, which is the shape the real exhausted quota arrives in.
     *
     * Numista's own month is 2.000 calls and the local gate of ADR 0003 is 1.500, so the gate cannot
     * see a key spent on another phone: `Quota exceeded` comes back as a `403` with budget to spare.
     */
    @Test
    fun `a key Numista refuses stops the pass too`() = runTest {
        val pass = pass(handler = { respond("Quota exceeded", HttpStatusCode.Forbidden, JSON) })

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertEquals(1, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
    }

    /** And the `401`, which is the token the pass could not get and will not get on the next call. */
    @Test
    fun `an unauthorised call stops the pass on the spot`() = runTest {
        val pass = pass(handler = { respond("", HttpStatusCode.Unauthorized, JSON) })

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertEquals(1, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
    }

    /**
     * A `404` on every issue is not a wall: each one leaves a row, so the pass asks the plan out.
     *
     * The other half of #560 and the reason the streak counts rows and not statuses — a collection
     * whose issues Numista simply has no prices for must still cost one call each, once.
     */
    @Test
    fun `a 404 on every issue is a datum and never stops the pass`() = runTest {
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })
        val owned = (1..BARREN_STREAK_LIMIT + 2).map { OwnedIssue(30, it) }

        val status = pass.run(plan(*owned.toTypedArray()), held = null)

        assertEquals(owned.size, asked.size)
        assertNull(status.held)
        assertEquals(owned.size, prices.reads.value.size)
    }

    /**
     * Whatever the status, a run of answers that leave no row is Numista refusing and not bad luck.
     *
     * The `5xx` is nobody's `429`, and one of them is one issue's problem; five in a row is a wall,
     * and the pass has 442 calls to spend against it.
     */
    @Test
    fun `a streak of answers that leave no row stops the pass`() = runTest {
        val pass = pass(handler = { respond("boom", HttpStatusCode.InternalServerError, JSON) })

        val status = pass.run(plan(*(1..20).map { OwnedIssue(30, it) }.toTypedArray()), held = null)

        assertEquals(BARREN_STREAK_LIMIT, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
        assertTrue(prices.reads.value.isEmpty())
    }

    /** One bad answer among good ones is still one issue's bad luck: the row breaks the streak. */
    @Test
    fun `a failure between answers that do land does not stop the pass`() = runTest {
        var answered = 0
        val pass = pass(
            handler = {
                if (answered++ % 2 == 0) {
                    respond("boom", HttpStatusCode.InternalServerError, JSON)
                } else {
                    respond(PRICED_BODY, HttpStatusCode.OK, JSON)
                }
            },
        )
        val owned = (1..2 * BARREN_STREAK_LIMIT).map { OwnedIssue(30, it) }

        val status = pass.run(plan(*owned.toTypedArray()), held = null)

        assertEquals(owned.size, asked.size)
        assertNull(status.held)
    }

    /**
     * A pass that hits the wall keeps everything it wrote before it (ADR 0028 §4).
     *
     * Which is the half of #560 that is not about spending: the father's nine passes wrote nothing
     * because nothing landed, not because a stop rolled anything back — and a stop that did roll back
     * would make the pass unresumable, so the next launch would pay for these three all over again.
     */
    @Test
    fun `what landed before the wall is still there after it`() = runTest {
        var answered = 0
        val pass = pass(
            handler = {
                if (answered++ < 3) {
                    respond(PRICED_BODY, HttpStatusCode.OK, JSON)
                } else {
                    respond("", HttpStatusCode.TooManyRequests, JSON)
                }
            },
        )
        val owned = (1..10).map { OwnedIssue(30, it) }

        val status = pass.run(plan(*owned.toTypedArray()), held = null)

        assertEquals(ValuationRefusal.Rejected, status.held)
        assertEquals(3, prices.reads.value.size, "las tres emisiones contestadas siguen escritas")
        assertEquals(owned.size - 3, status.missing)
    }

    /**
     * A wall in front of `/prices` alone still stops the pass, and the listings do not hide it.
     *
     * The trap the streak was one line away from falling into: a listing that answers 200 while every
     * price answers 500 would reset the count on every type, and a plan of holes would alternate
     * stored/barren to the last of its calls — the exact bill of #560, paid in the half of the plan
     * made of holes rather than of owned issues.
     */
    @Test
    fun `a wall in front of the prices alone is not hidden by the listings`() = runTest {
        val holes = (1..10).map { PlateHole("dates", typeId = it, year = 1_987) }
        val pass = pass(
            handler = { request ->
                if (request.url.encodedPath.endsWith("/issues")) {
                    respond(ISSUES, HttpStatusCode.OK, JSON)
                } else {
                    respond("boom", HttpStatusCode.InternalServerError, JSON)
                }
            },
        )

        val status = pass.run(ValuationPlan(owned = emptyList(), holes = holes), held = null)

        assertEquals(ValuationRefusal.Rejected, status.held)
        assertTrue(
            asked.count { it.endsWith("/prices") } == BARREN_STREAK_LIMIT,
            "el listado de cada tipo no borra la racha de precios: ${asked.count { it.endsWith("/prices") }}",
        )
    }

    /**
     * A type Numista does not list is the `404` of a price read over a listing, and never a wall.
     *
     * Without this the streak would read a run of types the catalogue has dropped as a refusal, which
     * is the one reading ADR 0028 §4 spends its whole table forbidding.
     */
    @Test
    fun `a listing Numista answers with a 404 does not stop the pass`() = runTest {
        val holes = (1..BARREN_STREAK_LIMIT + 2).map { PlateHole("dates", typeId = it, year = 1_987) }
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })

        val status = pass.run(ValuationPlan(owned = emptyList(), holes = holes), held = null)

        assertNull(status.held)
        assertEquals(holes.size, asked.size, "cada tipo cuesta su listado, y ninguno para el pase")
    }

    /** Held by a sync, the pass asks Numista for nothing at all. */
    @Test
    fun `a pass held by a sync asks for nothing`() = runTest {
        val pass = pass(PRICED)

        val status = pass.run(plan(OwnedIssue(30, 297)), held = ValuationRefusal.Syncing)

        assertTrue(asked.isEmpty())
        assertEquals(ValuationRefusal.Syncing, status.held)
    }

    /**
     * The spot is read **before** the refusal is honoured, because it is not Numista.
     *
     * Two keyless calls to hosts that are not `api.numista.com`, so they are outside the budget of
     * ADR 0003 — and a page whose money section is absent still has to be able to say what its silver was
     * worth the day it was read.
     */
    @Test
    fun `the spot is read even when the pass is held`() = runTest {
        val pass = pass(PRICED, spot = { 56.9 })

        val status = pass.run(plan(OwnedIssue(30, 297)), held = ValuationRefusal.Syncing)

        assertEquals(56.9, prices.spots.value.single().eurPerTroyOunce)
        assertEquals(NOW, status.spotRead)
    }

    /** With no API key there is no pass: that is the app before onboarding, not an error to discover. */
    @Test
    fun `with no API key the pass does not run`() = runTest {
        val pass = NumistaValuationPass(prices, { null }, spotStore(read = { 56.9 })) { NOW }

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(ValuationRefusal.NoApiKey, status.held)
        assertTrue(prices.reads.value.isEmpty())
    }

    /**
     * A hole is looked up by year and then priced, and the listing itself is written down (#452).
     *
     * A year the listing does not have still leaves no price row — Numista simply has no issue for it,
     * and the curated file is the authority on whether the coin exists (#48). What is new is that the
     * *listing* is stored, which is a fact about this phone and not a claim about the catalogue.
     */
    @Test
    fun `a hole is listed then priced, and the listing is written down`() = runTest {
        val pass = pass(LISTING_THEN_PRICE)

        pass.run(twoHoles(), held = null)

        // One listing for the type, and one price for the year it does have.
        assertEquals(
            listOf("/v3/types/30/issues", "/v3/types/30/issues/297/prices"),
            asked,
        )
        assertEquals(listOf(297), prices.reads.value.map { it.issueId })
        assertEquals(listOf(30), prices.typeIssueReads.value.map { it.typeId })
        // Las dos emisiones de 1987 que el listado trae, en el orden en que llegaron: el hueco se
        // tasa por la primera, y ese orden es lo que la columna `position` conserva.
        assertEquals(
            listOf(297 to 0, 278_721 to 1),
            prices.typeIssues.value.map { it.issueId to it.position },
        )
    }

    /**
     * And the pass after it asks Numista for **nothing at all** — which is the whole of #452.
     *
     * Before the listing was stored this second run cost the lookup and the price all over again: the
     * hole does not declare its issue, so `hole.issueIds.none { it in fresh }` was `true` over an empty
     * list and there was no way to tell the price it already held from one never asked for. Over the
     * father's collection that was 102 lookups and 111 prices on every cold start — 213 of the 1.999
     * calls Numista let him make in August, after which his app had no budget left for the fichas it
     * was missing (#448).
     */
    @Test
    fun `with the listing stored the next pass asks for nothing`() = runTest {
        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertTrue(asked.isEmpty(), "la segunda pasada no vuelve a comprar lo que ya está guardado")
    }

    /**
     * A listing that **failed** is asked again, because nothing was written down.
     *
     * The same bargain a price makes: «asked and empty» is a datum and is kept, and a dead network is
     * not an answer at all (ADR 0025).
     */
    @Test
    fun `a listing that failed is asked again next pass`() = runTest {
        pass(handler = { throw java.io.IOException("sin red") }).run(twoHoles(), held = null)
        assertTrue(prices.typeIssueReads.value.isEmpty())
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertEquals("/v3/types/30/issues", asked.first())
    }

    /**
     * An entry with no issue id of its own is no candidate — in **both** readings of the listing.
     *
     * `storeListing` cannot keep it, because there is nothing to key a price on. If `askHoles` still
     * counted it as the match, this pass would skip the hole and the next one — reading the stored,
     * filtered listing — would price the issue behind it, so the type would be paid for twice and the
     * two «first match» rules would disagree for ever.
     */
    @Test
    fun `an issue with no id of its own is not the match in either reading`() = runTest {
        val handler: io.ktor.client.engine.mock.MockRequestHandler = { request ->
            if (request.url.encodedPath.endsWith("/issues")) {
                respond(ISSUES_FIRST_WITHOUT_ID, HttpStatusCode.OK, JSON)
            } else {
                respond(PRICED_BODY, HttpStatusCode.OK, JSON)
            }
        }
        pass(handler).run(twoHoles(), held = null)

        assertEquals(listOf(278_721), prices.reads.value.map { it.issueId })
        asked.clear()
        pass(handler).run(twoHoles(), held = null)

        assertTrue(asked.isEmpty(), "las dos lecturas del listado eligen la misma emisión")
    }

    /**
     * Ninety days later the listing is read again, because the catalogue does move — slowly.
     *
     * An open date run grows a slot every January. A listing that never expired would leave that new
     * hole unpriceable for the life of the phone, and `missing` counts owned issues, so nothing on
     * screen would say a word about it.
     */
    @Test
    fun `a listing older than ninety days is read again`() = runTest {
        val old = LISTING_LIFETIME_MILLIS + 1
        pass(LISTING_THEN_PRICE, now = NOW - old).run(twoHoles(), held = null)
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertEquals("/v3/types/30/issues", asked.first())
    }

    /**
     * A type Numista lists with **no** issue for the hole's year is written down as listed all the same.
     *
     * Otherwise an empty answer is indistinguishable from an unasked one and the lookup comes back on
     * every pass for the life of the phone, which is what the old KDoc of `askHoles` accepted.
     */
    @Test
    fun `a listing with no matching year still stops the lookup`() = runTest {
        val onlyHole = ValuationPlan(
            owned = emptyList(),
            holes = listOf(PlateHole("dates", typeId = 30, year = 1_904)),
        )
        pass(LISTING_THEN_PRICE).run(onlyHole, held = null)
        assertTrue(prices.reads.value.isEmpty(), "un año que el listado no tiene no deja precio")
        asked.clear()

        pass(LISTING_THEN_PRICE).run(onlyHole, held = null)

        assertTrue(asked.isEmpty())
    }

    /**
     * A month later the issue is read again, and the grades of the first read are **replaced**.
     *
     * Merged over, a grade Numista has stopped pricing would survive for ever under a fresh date, which is
     * a price with a date that is not its own. And the two halves of this test are the two halves of
     * «caducar no es borrar»: until the second read lands, the first one is still what the page shows.
     */
    @Test
    fun `a month later the issue is read again and its grades are replaced`() = runTest {
        val month = 40L * 24 * 60 * 60 * 1_000
        pass(PRICED, now = NOW - month).run(plan(OwnedIssue(30, 297)), held = null)
        assertEquals(2, prices.prices.value.size, "el precio viejo sigue en el teléfono")

        pass(EMPTY_PRICES).run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.prices.value.isEmpty())
        assertEquals(1, prices.reads.value.size)
    }

    /** A stored spot from yesterday is not read again today unless the day has turned. */
    @Test
    fun `a spot read today is not read again`() = runTest {
        prices.putSpot(MetalSpotEntity(SILVER_SYMBOL, 50.0, NOW - 1_000))
        var reads = 0
        val pass = pass(PRICED, spot = { reads++; 56.9 })

        pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(0, reads)
        assertEquals(50.0, prices.spots.value.single().eurPerTroyOunce)
    }

    /** A spot the reader cannot bring leaves the stored one alone, whatever its age. */
    @Test
    fun `a spot that cannot be read leaves the old one, expired and all`() = runTest {
        prices.putSpot(MetalSpotEntity(SILVER_SYMBOL, 50.0, NOW - 40L * 24 * 60 * 60 * 1_000))
        val pass = pass(PRICED, spot = { null })

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(50.0, prices.spots.value.single().eurPerTroyOunce)
        assertNull(status.spotRead?.takeIf { it == NOW })
    }

    private fun plan(vararg owned: OwnedIssue) =
        ValuationPlan(owned = owned.toList(), holes = emptyList())

    /** Two holes of one type: the year the listing has, and one it does not. */
    private fun twoHoles() = ValuationPlan(
        owned = emptyList(),
        holes = listOf(
            PlateHole("dates", typeId = 30, year = 1_987),
            PlateHole("dates", typeId = 30, year = 1_904),
        ),
    )

    private fun pass(
        handler: io.ktor.client.engine.mock.MockRequestHandler,
        budget: suspend (String) -> Unit = {},
        spot: suspend () -> Double? = { 56.9 },
        now: Long = NOW,
    ): ValuationPass {
        val engine = MockEngine { request ->
            asked += request.url.encodedPath
            handler(this, request)
        }
        val client = NumistaClient(
            httpClient = HttpClient(engine),
            apiKey = "key",
            budget = object : CallBudget {
                override suspend fun reserve(endpoint: String) = budget(endpoint)
            },
        )
        return NumistaValuationPass(prices, { client }, spotStore(spot, now)) { now }
    }

    private fun spotStore(read: suspend () -> Double?, now: Long = NOW) = SpotStore(
        prices,
        object : SpotReader {
            override suspend fun read(): Double? = read()
        },
    ) { now }
}

private val JSON = headersOf(HttpHeaders.ContentType, "application/json")

private const val PRICED_BODY =
    """{"currency":"EUR","prices":[{"grade":"vf","price":25.1},{"grade":"unc","price":39.6}]}"""

private const val ISSUES =
    """[{"id":297,"year":1987,"gregorian_year":1987},{"id":278721,"year":1987}]"""

/** The same 1987 twice, and the first of them with no id Numista can be asked a price for. */
private const val ISSUES_FIRST_WITHOUT_ID =
    """[{"year":1987,"gregorian_year":1987},{"id":278721,"year":1987}]"""

private val PRICED: io.ktor.client.engine.mock.MockRequestHandler =
    { respond(PRICED_BODY, HttpStatusCode.OK, JSON) }

private val EMPTY_PRICES: io.ktor.client.engine.mock.MockRequestHandler =
    { respond("""{"currency":"EUR","prices":[]}""", HttpStatusCode.OK, JSON) }

/** Numista answering both calls a hole costs: the listing of its type, then the price of an issue. */
private val LISTING_THEN_PRICE: io.ktor.client.engine.mock.MockRequestHandler = { request ->
    if (request.url.encodedPath.endsWith("/issues")) {
        respond(ISSUES, HttpStatusCode.OK, JSON)
    } else {
        respond(PRICED_BODY, HttpStatusCode.OK, JSON)
    }
}
