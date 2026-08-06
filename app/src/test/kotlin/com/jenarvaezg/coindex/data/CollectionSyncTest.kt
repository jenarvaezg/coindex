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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

private const val ONE_ITEM = """
{
  "item_count": 1,
  "items": [
    {"id": 1, "quantity": 1, "type": {"id": 10340, "title": "5 Bolívares"},
     "issue": {"year": 1929, "gregorian_year": 1929}}
  ]
}
"""

/** 7 de agosto de 2026, 10:31 en Madrid. Any fixed instant would do; a real one reads better. */
private const val STAMPED_AT = 1_786_170_660_000L

/**
 * What one sync leaves behind (#220).
 *
 * The stamp is the whole reason this is a class of its own: `System.currentTimeMillis()` read where
 * the record was built made «última sincronización: ayer 19:04» a line no test could reach, while
 * [lastSyncLabel], which prints it, has taken an injected clock since it was written.
 */
class CollectionSyncTest {
    private val items = FakeCollectedItemDao()
    private val types = FakeTypeMetaDao()
    private val calls = FakeApiCallDao()
    private val log = FakeSyncLog()
    private val service = SyncService(items, types, ApiCallLedger(calls) { 1_000L }) { 1_000L }
    private val sync = CollectionSync(service, log) { STAMPED_AT }

    /** A client that answers out of the mock engine, recording its calls like the real gate does. */
    private fun client(collectionStatus: HttpStatusCode = HttpStatusCode.OK): NumistaClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.contains("oauth_token") ->
                    respond(
                        """{"access_token":"t","expires_in":600}""",
                        HttpStatusCode.OK,
                        headersOf("Content-Type", "application/json"),
                    )
                path.contains("collected_items") -> respond(
                    if (collectionStatus == HttpStatusCode.OK) ONE_ITEM else "",
                    collectionStatus,
                    headersOf("Content-Type", "application/json"),
                )
                else -> respond(
                    """{"id": 10340, "title": "Pieza", "weight": 25.0}""",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json"),
                )
            }
        }
        val budget = object : CallBudget {
            override suspend fun reserve(endpoint: String) {
                calls.record(ApiCallEntity(endpoint = endpoint, calledAt = 1_000L))
            }
        }
        return NumistaClient(HttpClient(engine), "key", budget, "https://api.example/v3") { 1_000L }
    }

    @Test
    fun `the record is stamped with the given clock and written down, not just announced`() =
        runTest {
            val outcome = sync.run(client(), userId = 2104)

            val record = assertIs<SyncOutcome.Done>(outcome).record
            assertEquals(STAMPED_AT, record.atMillis)
            assertEquals(1, record.collectionItems)
            assertEquals(1, record.typesFetched)
            // The snackbar is the copy: what the next launch reads has to be there already.
            assertEquals(record, log.last)
            assertEquals(record, sync.last)
        }

    @Test
    fun `the calls the sync spent are the ones it actually recorded`() = runTest {
        val outcome = sync.run(client(), userId = 2104)

        // Token, collection and the one type nobody had cached: three reservations.
        assertEquals(3, assertIs<SyncOutcome.Done>(outcome).record.callsSpent)
        assertEquals(3, calls.calls.size)
    }

    @Test
    fun `a sync that failed writes nothing down, because it did not happen`() = runTest {
        val outcome = sync.run(client(HttpStatusCode.InternalServerError), userId = 2104)

        assertIs<SyncOutcome.Failed>(outcome)
        assertNull(log.last)
    }

    @Test
    fun `a failure keeps the record of the last sync that did happen`() = runTest {
        val earlier = SyncRecord(atMillis = 1_000L, collectionItems = 58, typesFetched = 0, callsSpent = 2)
        log.last = earlier

        sync.run(client(HttpStatusCode.InternalServerError), userId = 2104)

        assertEquals(earlier, log.last)
    }
}
