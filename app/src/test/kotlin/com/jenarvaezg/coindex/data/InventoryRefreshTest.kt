package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

/** 8 de agosto de 2026, 08:31 en Madrid. */
private const val NOW = 1_786_170_660_000L
private const val HOUR = 60L * 60 * 1_000

/**
 * The inventory refreshing itself once a day has passed (#605). Mostly what it doesn't do, unlike
 * the manual sync it borrows from: ask within the day, ask into a wall, fetch unlimited fichas or
 * show an error.
 */
class InventoryRefreshTest {
    private val items = FakeCollectedItemDao()
    private val types = FakeTypeMetaDao()
    private val calls = FakeApiCallDao()
    private val log = StoredSyncLog(FakeNamedValues())
    private val wall = StoredRejectionWall(FakeNamedValues()) { NOW }
    private val sync = CollectionSync(
        SyncService(items, types, ApiCallLedger(calls) { NOW }) { NOW },
        log,
        wall,
    ) { NOW }

    private fun refresh(startDelayMillis: Long = 0L, now: Long = NOW) =
        InventoryRefresh(sync, wall, startDelayMillis) { now }

    /** A collection of [pieces] distinct types, enough to hit the ficha cap. */
    private fun client(pieces: Int = 1, collectionStatus: HttpStatusCode = HttpStatusCode.OK): NumistaClient {
        val body = buildString {
            append("""{"item_count": $pieces, "items": [""")
            append(
                (1..pieces).joinToString(",") { n ->
                    """{"id": $n, "quantity": 1, "type": {"id": ${10_000 + n}, "title": "Pieza $n"},
                       "issue": {"year": 1929, "gregorian_year": 1929}}"""
                },
            )
            append("]}")
        }
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.contains("oauth_token") -> respond(
                    """{"access_token":"t","expires_in":600}""",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
                path.contains("collected_items") -> respond(
                    if (collectionStatus == HttpStatusCode.OK) body else "",
                    collectionStatus,
                    headersOf("Content-Type", "application/json"),
                )
                else -> respond(
                    """{"id": 10001, "title": "Pieza", "weight": 25.0}""",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
            }
        }
        val budget = object : CallBudget {
            override suspend fun reserve(endpoint: String) {
                calls.record(ApiCallEntity(endpoint = endpoint, calledAt = NOW))
            }
        }
        return NumistaClient(HttpClient(engine), "key", budget, "https://api.example/v3") { NOW }
    }

    @Test
    fun `a launch a day after the last sync brings the inventory`() = runTest {
        log.last = SyncRecord(atMillis = NOW - 25 * HOUR, collectionItems = 1, typesFetched = 0, callsSpent = 2)

        assertTrue(refresh().run(client(), userId = 2104))

        assertEquals(1, items.rows.value.size)
        assertEquals(NOW, log.last?.atMillis, "y la línea durable pasa a decir la verdad")
    }

    @Test
    fun `a launch an hour after the last sync asks for nothing`() = runTest {
        log.last = SyncRecord(atMillis = NOW - HOUR, collectionItems = 1, typesFetched = 0, callsSpent = 2)

        assertFalse(refresh().run(client(), userId = 2104))

        assertTrue(calls.calls.isEmpty(), "ni el token se pide: ${calls.calls.map { it.endpoint }}")
    }

    /** Unlike the manual sync, which is how the collector checks the key works again (#579). */
    @Test
    fun `it does not ask into a wall that is standing`() = runTest {
        wall.raise(RejectionCause.Quota)

        assertFalse(refresh().run(client(), userId = 2104))

        assertTrue(calls.calls.isEmpty())
    }

    /**
     * Nobody watches this sync, and a cache emptied by a reinstall would turn one launch into
     * hundreds of consultas. The inventory is one call and never capped; other fichas wait.
     */
    @Test
    fun `an automatic refresh buys ten fichas and leaves the rest`() = runTest {
        refresh().run(client(pieces = 30), userId = 2104)

        assertEquals(30, items.rows.value.size, "el inventario entero, que es lo barato")
        assertEquals(AUTOMATIC_FICHA_LIMIT, types.rows.value.size)
        assertEquals(AUTOMATIC_FICHA_LIMIT, log.last?.typesFetched)
    }

    /**
     * The messages of `syncErrorLabel` are only for the manual sync, and the record of the last
     * sync survives because a failed refresh did not happen.
     */
    @Test
    fun `a refresh that could not reach Numista says nothing and breaks nothing`() = runTest {
        val earlier = SyncRecord(atMillis = NOW - 30 * HOUR, collectionItems = 58, typesFetched = 0, callsSpent = 2)
        log.last = earlier

        assertFalse(refresh().run(client(collectionStatus = HttpStatusCode.InternalServerError), userId = 2104))

        assertEquals(earlier, log.last)
        assertTrue(items.rows.value.isEmpty())
    }

    /**
     * The refresh waits two seconds for the first screen, and the valuation pass checks for a sync
     * one second in; a claim raised with the first call would let the pass spend budget the refresh
     * needs and be cancelled mid-consulta (ADR 0028 §6).
     */
    @Test
    fun `the claim is raised before the delay and dropped after the sync`() = runTest {
        val refresh = refresh(startDelayMillis = 2_000L)
        assertFalse(refresh.inFlight)

        val running = launch { refresh.run(client(), userId = 2104) }
        advanceTimeBy(1_000L)
        assertTrue(refresh.inFlight, "a un segundo del arranque el pase ya tiene que verlo")

        running.join()
        assertFalse(refresh.inFlight)
    }

    @Test
    fun `a phone that has never synced refreshes on its first launch`() = runTest {
        assertNull(log.last)

        assertTrue(refresh().run(client(), userId = 2104))
    }
}
