package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.FakeNamedValues
import com.jenarvaezg.coindex.data.FakePriceDao
import com.jenarvaezg.coindex.data.RejectionCause
import com.jenarvaezg.coindex.data.RejectionWall
import com.jenarvaezg.coindex.data.startOfMonthMillis
import com.jenarvaezg.coindex.data.StoredRejectionWall
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
private const val DAY = 24L * 60 * 60 * 1_000
private const val HOUR = 60L * 60 * 1_000

/**
 * How the valuation pass reads each answer (ADR 0028 §4): prices, no prices and a failure are three
 * outcomes, and a failure writes nothing. Taking «no price» for a failure would re-ask those issues
 * on every pass.
 */
class ValuationPassTest {
    private val prices = FakePriceDao()
    private val asked = mutableListOf<String>()

    /**
     * Only refusals that wait on the collector or the calendar are mentioned (#519); a sync clears
     * by itself in seconds (ADR 0026 §5).
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

    /** With nothing left to ask there is no absence to explain. */
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

    /** The issue stops being missing, so it is not asked for again on every pass. */
    @Test
    fun `an issue Numista has no price for is stored as a datum`() = runTest {
        val pass = pass(EMPTY_PRICES)

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.prices.value.isEmpty())
        assertEquals(false, prices.reads.value.single().hasPrices)
        assertEquals(0, status.missing, "una emisión contestada deja de faltar aunque no traiga precio")
    }

    @Test
    fun `a 404 is read as no price and not as a failure`() = runTest {
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })

        pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(false, prices.reads.value.single().hasPrices)
    }

    /** So the next pass retries it (ADR 0025). */
    @Test
    fun `a network failure writes no row at all`() = runTest {
        val pass = pass(handler = { throw java.io.IOException("sin red") })

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.reads.value.isEmpty())
        assertTrue(prices.prices.value.isEmpty())
        assertEquals(ValuationRefusal.Offline, status.held)
        assertEquals(1, status.missing)
    }

    /** It stops rather than skipping: every further call would throw the same way. */
    @Test
    fun `an exhausted budget stops the pass and writes nothing`() = runTest {
        val pass = pass(PRICED, budget = { throw NumistaException.BudgetExhausted(1_500, 1_500) })

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertTrue(prices.reads.value.isEmpty())
        assertEquals(ValuationRefusal.BudgetExhausted, status.held)
        assertTrue(asked.isEmpty(), "el presupuesto se reserva antes de la llamada, así que no sale ninguna")
    }

    /**
     * A throttled key answers every call alike; reading each `429` as «no price» once spent a whole
     * month's quota writing nothing (#560).
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
     * On the routes the app calls, a `403` is a revoked key or Cloudflare rather than the quota, so
     * its wall is the credential's (#600).
     */
    @Test
    fun `a key Numista refuses stops the pass too`() = runTest {
        val wall = StoredRejectionWall(FakeNamedValues()) { NOW }
        val pass = pass(handler = { respond("nope", HttpStatusCode.Forbidden, JSON) }, wall = wall)

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertEquals(1, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
        assertEquals(RejectionCause.Credentials, wall.standing())
    }

    /**
     * A `429` is either the exhausted month or the throttle, and only the body tells them apart
     * (#600). The month can run out with local budget to spare when the key is spent on another
     * phone (ADR 0003); any other `429` is the throttle, held for hours rather than a month.
     */
    @Test
    fun `only the body tells the exhausted month from the throttle`() = runTest {
        val exhausted = StoredRejectionWall(FakeNamedValues()) { NOW }
        pass(handler = { respond("Quota exceeded", HttpStatusCode.TooManyRequests, JSON) }, wall = exhausted)
            .run(plan(OwnedIssue(30, 297)), held = null)

        val throttled = StoredRejectionWall(FakeNamedValues()) { NOW }
        pass(handler = { respond("Too many simultaneous requests", HttpStatusCode.TooManyRequests, JSON) }, wall = throttled)
            .run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(RejectionCause.Quota, exhausted.standing(), "el mes agotado espera al día 1")
        assertEquals(RejectionCause.Throttled, throttled.standing(), "el throttle son horas, no un mes")
    }

    /** A `401` would be the same on the next call. */
    @Test
    fun `an unauthorised call stops the pass on the spot`() = runTest {
        val pass = pass(handler = { respond("", HttpStatusCode.Unauthorized, JSON) })

        val status = pass.run(plan(OwnedIssue(30, 297), OwnedIssue(30, 298)), held = null)

        assertEquals(1, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
    }

    /**
     * The wall is remembered (#579), so a later pass reads it instead of paying to rediscover it.
     * Asserted on the budget rather than the engine because
     * [com.jenarvaezg.coindex.data.CallBudgetGate] records a call before sending it.
     */
    @Test
    fun `two passes against a refused key cost one call between them`() = runTest {
        val reserved = mutableListOf<String>()
        val wall = StoredRejectionWall(FakeNamedValues()) { NOW }
        val refused: io.ktor.client.engine.mock.MockRequestHandler =
            { respond("Quota exceeded", HttpStatusCode.TooManyRequests, JSON) }

        pass(refused, budget = { reserved += it }, wall = wall)
            .run(plan(OwnedIssue(30, 297)), held = null)
        val second = pass(refused, budget = { reserved += it }, wall = wall)
            .run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(1, reserved.size, "el segundo pase lee la pared en vez de comprarla")
        assertEquals(ValuationRefusal.Rejected, second.held, "y dice lo mismo que diría pagando")
        assertEquals(RejectionCause.Quota, wall.standing())
    }

    /** The quota is Numista's calendar month, so its wall falls on the 1st. */
    @Test
    fun `the first of the next month buys another call`() = runTest {
        var clock = NOW
        val wall = StoredRejectionWall(FakeNamedValues()) { clock }
        val refused: io.ktor.client.engine.mock.MockRequestHandler =
            { respond("Quota exceeded", HttpStatusCode.TooManyRequests, JSON) }
        pass(refused, now = clock, wall = wall).run(plan(OwnedIssue(30, 297)), held = null)

        clock = startOfMonthMillis(NOW + 40 * DAY)
        pass(PRICED, now = clock, wall = wall).run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(2, asked.size, "la pared de la cuota cae con el mes, no con un plazo de horas")
        assertEquals(listOf("vf" to 25.1, "unc" to 39.6), prices.prices.value.map { it.grade to it.eur })
    }

    /**
     * Reaching Numista proves nothing is in the way, so the stored wall is cleared; a stale entry
     * could be believed by a later version that changes a wall's lifetime.
     */
    @Test
    fun `a pass that goes through leaves no wall behind`() = runTest {
        var clock = NOW
        val values = FakeNamedValues()
        val wall = StoredRejectionWall(values) { clock }
        wall.raise(RejectionCause.Throttled)

        clock = NOW + 7 * HOUR
        pass(PRICED, now = clock, wall = wall).run(plan(OwnedIssue(30, 297)), held = null)

        assertNull(wall.standing())
        assertTrue(values.entries.isEmpty(), "la pared caducada no se queda en el fichero: ${values.entries}")
    }

    /** Each `404` leaves a row, which is why the streak counts rows and not statuses (#560). */
    @Test
    fun `a 404 on every issue is a datum and never stops the pass`() = runTest {
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })
        val owned = (1..BARREN_STREAK_LIMIT + 2).map { OwnedIssue(30, it) }

        val status = pass.run(plan(*owned.toTypedArray()), held = null)

        assertEquals(owned.size, asked.size)
        assertNull(status.held)
        assertEquals(owned.size, prices.reads.value.size)
    }

    /** One `5xx` is one issue's problem; a run of answers with no row is a wall. */
    @Test
    fun `a streak of answers that leave no row stops the pass`() = runTest {
        val pass = pass(handler = { respond("boom", HttpStatusCode.InternalServerError, JSON) })

        val status = pass.run(plan(*(1..20).map { OwnedIssue(30, it) }.toTypedArray()), held = null)

        assertEquals(BARREN_STREAK_LIMIT, asked.size)
        assertEquals(ValuationRefusal.Rejected, status.held)
        assertTrue(prices.reads.value.isEmpty())
    }

    /** Every row that lands breaks the streak. */
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

    /** A rollback would make the next launch pay again for what landed (ADR 0028 §4). */
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
     * A listing answering 200 must not reset the streak, or a plan of holes would alternate stored
     * and barren answers to its last call (#560).
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

    /** A type Numista no longer lists is a datum, never a refusal (ADR 0028 §4). */
    @Test
    fun `a listing Numista answers with a 404 does not stop the pass`() = runTest {
        val holes = (1..BARREN_STREAK_LIMIT + 2).map { PlateHole("dates", typeId = it, year = 1_987) }
        val pass = pass(handler = { respond("no", HttpStatusCode.NotFound, JSON) })

        val status = pass.run(ValuationPlan(owned = emptyList(), holes = holes), held = null)

        assertNull(status.held)
        assertEquals(holes.size, asked.size, "cada tipo cuesta su listado, y ninguno para el pase")
    }

    @Test
    fun `a pass held by a sync asks for nothing`() = runTest {
        val pass = pass(PRICED)

        val status = pass.run(plan(OwnedIssue(30, 297)), held = ValuationRefusal.Syncing)

        assertTrue(asked.isEmpty())
        assertEquals(ValuationRefusal.Syncing, status.held)
    }

    /** The spot is two keyless calls outside Numista and its budget (ADR 0003). */
    @Test
    fun `the spot is read even when the pass is held`() = runTest {
        val pass = pass(PRICED, spot = { 56.9 })

        val status = pass.run(plan(OwnedIssue(30, 297)), held = ValuationRefusal.Syncing)

        assertEquals(56.9, prices.spots.value.single().eurPerTroyOunce)
        assertEquals(NOW, status.spotRead)
    }

    /** No key is the app before onboarding, not an error. */
    @Test
    fun `with no API key the pass does not run`() = runTest {
        val wall = StoredRejectionWall(FakeNamedValues()) { NOW }
        val pass = NumistaValuationPass(prices, { null }, spotStore(read = { 56.9 }), wall) { NOW }

        val status = pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(ValuationRefusal.NoApiKey, status.held)
        assertTrue(prices.reads.value.isEmpty())
    }

    /**
     * The listing is stored (#452). A year it lacks leaves no price row; whether that coin exists
     * is for the curated file to say (#48).
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
        // El hueco se tasa por la primera emisión del año; `position` guarda el orden de llegada.
        assertEquals(
            listOf(297 to 0, 278_721 to 1),
            prices.typeIssues.value.map { it.issueId to it.position },
        )
    }

    /** A hole does not declare its issue; the stored listing says which price it has (#452). */
    @Test
    fun `with the listing stored the next pass asks for nothing`() = runTest {
        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertTrue(asked.isEmpty(), "la segunda pasada no vuelve a comprar lo que ya está guardado")
    }

    /** «Asked and empty» is kept; a dead network is no answer at all (ADR 0025). */
    @Test
    fun `a listing that failed is asked again next pass`() = runTest {
        pass(handler = { throw java.io.IOException("sin red") }).run(twoHoles(), held = null)
        assertTrue(prices.typeIssueReads.value.isEmpty())
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertEquals("/v3/types/30/issues", asked.first())
    }

    /**
     * `storeListing` drops an entry with no issue id, so `askHoles` must skip it too: otherwise the
     * next pass, reading the stored listing, would match another issue and pay again.
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
     * An open date run grows a slot every January, and `missing` only counts owned issues, so a
     * listing that never expired would leave the new hole unpriced without anything saying so.
     */
    @Test
    fun `a listing older than ninety days is read again`() = runTest {
        val old = LISTING_LIFETIME_MILLIS + 1
        pass(LISTING_THEN_PRICE, now = NOW - old).run(twoHoles(), held = null)
        asked.clear()

        pass(LISTING_THEN_PRICE).run(twoHoles(), held = null)

        assertEquals("/v3/types/30/issues", asked.first())
    }

    /** Otherwise an empty answer would look unasked and the lookup would repeat on every pass. */
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
     * Merging would keep a grade Numista no longer prices under a fresh date. Until the new read
     * lands, the expired one is still what the page shows. The age comes from
     * [PRICE_LIFETIME_MILLIS] because #561 changed it.
     */
    @Test
    fun `a quarter later the issue is read again and its grades are replaced`() = runTest {
        val expired = PRICE_LIFETIME_MILLIS + 1
        pass(PRICED, now = NOW - expired).run(plan(OwnedIssue(30, 297)), held = null)
        assertEquals(2, prices.prices.value.size, "el precio viejo sigue en el teléfono")

        pass(EMPTY_PRICES).run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(prices.prices.value.isEmpty())
        assertEquals(1, prices.reads.value.size)
    }

    /** Prices outlive a month since #561 removed the monthly refresh. */
    @Test
    fun `a month later the issue is not read again`() = runTest {
        pass(PRICED, now = NOW - 40 * DAY).run(plan(OwnedIssue(30, 297)), held = null)
        asked.clear()

        pass(EMPTY_PRICES).run(plan(OwnedIssue(30, 297)), held = null)

        assertTrue(asked.isEmpty())
        assertEquals(2, prices.prices.value.size)
    }

    @Test
    fun `a spot read today is not read again`() = runTest {
        prices.putSpot(MetalSpotEntity(SILVER_SYMBOL, 50.0, NOW - 1_000))
        var reads = 0
        val pass = pass(PRICED, spot = { reads++; 56.9 })

        pass.run(plan(OwnedIssue(30, 297)), held = null)

        assertEquals(0, reads)
        assertEquals(50.0, prices.spots.value.single().eurPerTroyOunce)
    }

    @Test
    fun `a spot that cannot be read leaves the old one, expired and all`() = runTest {
        prices.putSpot(MetalSpotEntity(SILVER_SYMBOL, 50.0, NOW - 40 * DAY))
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
        wall: RejectionWall = StoredRejectionWall(FakeNamedValues()) { now },
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
        return NumistaValuationPass(prices, { client }, spotStore(spot, now), wall) { now }
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
