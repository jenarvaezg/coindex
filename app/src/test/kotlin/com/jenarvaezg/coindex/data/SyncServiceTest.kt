package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.db.CollectedItemEntity
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val TWO_ITEMS_TWO_TYPES = """
{
  "item_count": 2,
  "items": [
    {"id": 1, "quantity": 1, "type": {"id": 11331, "title": "2 Sapèque"},
     "issue": {"year": 1887, "gregorian_year": 1887}},
    {"id": 2, "quantity": 2, "type": {"id": 10340, "title": "5 Bolívares"},
     "issue": {"year": 1929, "gregorian_year": 1929}}
  ]
}
"""

private const val TYPE_BODY = """{"id": %d, "title": "Pieza", "weight": 25.0}"""

class SyncServiceTest {
    private val items = FakeCollectedItemDao()
    private val types = FakeTypeMetaDao()
    private val calls = FakeApiCallDao()
    private val clock = { 1_000L }
    private val service = SyncService(items, types, ApiCallLedger(calls, clock), clock)

    /** A budget that records into the same log the sync reads, like the real gate does. */
    private inner class LoggingBudget(private val failAt: String? = null) : CallBudget {
        override suspend fun reserve(endpoint: String) {
            if (failAt != null && endpoint.contains(failAt)) {
                throw NumistaException.BudgetExhausted(calls.calls.size, calls.calls.size)
            }
            calls.record(ApiCallEntity(endpoint = endpoint, calledAt = 1_000L))
        }
    }

    private fun client(
        collection: String = TWO_ITEMS_TWO_TYPES,
        budget: CallBudget = LoggingBudget(),
    ): Pair<NumistaClient, () -> Int> {
        var typeRequests = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val body = when {
                path.contains("oauth_token") -> """{"access_token":"t","expires_in":600}"""
                path.contains("collected_items") -> collection
                else -> {
                    typeRequests += 1
                    TYPE_BODY.format(path.substringAfterLast('/').toInt())
                }
            }
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val numista = NumistaClient(
            HttpClient(engine),
            "key",
            budget,
            "https://api.example/v3",
        ) { 1_000L }
        return numista to { typeRequests }
    }

    @Test
    fun `a second sync spends zero type metadata calls because everything is cached`() = runTest {
        val (numista, typeRequests) = client()

        val first = service.run(numista, 2104)
        val second = service.run(numista, 2104)

        assertEquals(2, first.typesFetched)
        assertEquals(0, second.typesFetched)
        assertEquals(2, typeRequests())
        // Token cached, so the second sync costs exactly one collection request.
        assertEquals(1, second.callsSpent)
        assertEquals(2, items.rows.value.size)
    }

    @Test
    fun `a response without items never clears the existing snapshot`() = runTest {
        items.rows.value = listOf(
            CollectedItemEntity(
                id = 9, typeId = 90, quantity = 1, title = null, issuerCode = null,
                issueYear = null, gregorianYear = null, grade = null, price = null,
                forSwap = null, collectionName = null, raw = "{}", syncedAt = 0,
            ),
        )
        val (numista, _) = client(collection = """{"item_count": 0}""")

        assertFailsWith<NumistaException.InvalidResponse> { service.run(numista, 2104) }

        assertEquals(9L, items.rows.value.single().id)
    }

    @Test
    fun `an empty collection never clears the existing snapshot either`() = runTest {
        items.rows.value = listOf(
            CollectedItemEntity(
                id = 9, typeId = 90, quantity = 1, title = null, issuerCode = null,
                issueYear = null, gregorianYear = null, grade = null, price = null,
                forSwap = null, collectionName = null, raw = "{}", syncedAt = 0,
            ),
        )
        val (numista, _) = client(collection = """{"item_count": 0, "items": []}""")

        assertFailsWith<NumistaException.InvalidResponse> { service.run(numista, 2104) }

        assertEquals(9L, items.rows.value.single().id)
    }

    @Test
    fun `running out of budget mid-sync keeps the fresh inventory and reports it`() = runTest {
        val (numista, _) = client(budget = LoggingBudget(failAt = "/types/"))

        val report = service.run(numista, 2104)

        assertEquals(2, report.collectionItems)
        assertEquals(0, report.typesFetched)
        assertEquals(2, report.typesStillMissing)
        assertTrue(report.partialFailure!!.contains("Presupuesto"), report.partialFailure!!)
        assertEquals(2, items.rows.value.size)
    }

    @Test
    fun `the stored snapshot keeps every field of the item body and the recorded year`() = runTest {
        val (numista, _) = client()

        service.run(numista, 2104)

        val bolivares = items.rows.value.first { it.typeId == 10_340 }
        assertEquals(2, bolivares.quantity)
        assertEquals(1929, bolivares.issueYear)
        assertTrue(bolivares.raw.contains("\"gregorian_year\":1929"), bolivares.raw)
        assertEquals(25.0, types.rows.value.first { it.typeId == 10_340 }.weightGrams)
    }
}
