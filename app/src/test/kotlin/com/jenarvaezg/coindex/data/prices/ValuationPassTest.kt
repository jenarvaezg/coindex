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
     * A hole is looked up by year and then priced, and a year the listing does not have writes nothing.
     *
     * Not a failure — Numista simply has no issue for that year — but not a datum either: what would be
     * stored is a claim about the catalogue, and the curated file is the authority on whether the coin
     * exists (#48).
     */
    @Test
    fun `a hole is listed then priced, and an absent year writes nothing`() = runTest {
        val pass = pass(
            handler = { request ->
                if (request.url.encodedPath.endsWith("/issues")) {
                    respond(ISSUES, HttpStatusCode.OK, JSON)
                } else {
                    respond(PRICED_BODY, HttpStatusCode.OK, JSON)
                }
            },
        )

        pass.run(
            ValuationPlan(
                owned = emptyList(),
                holes = listOf(
                    PlateHole("dates", typeId = 30, year = 1_987),
                    PlateHole("dates", typeId = 30, year = 1_904),
                ),
            ),
            held = null,
        )

        // One listing for the type, and one price for the year it does have.
        assertEquals(
            listOf("/v3/types/30/issues", "/v3/types/30/issues/297/prices"),
            asked,
        )
        assertEquals(listOf(297), prices.reads.value.map { it.issueId })
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

private val PRICED: io.ktor.client.engine.mock.MockRequestHandler =
    { respond(PRICED_BODY, HttpStatusCode.OK, JSON) }

private val EMPTY_PRICES: io.ktor.client.engine.mock.MockRequestHandler =
    { respond("""{"currency":"EUR","prices":[]}""", HttpStatusCode.OK, JSON) }
