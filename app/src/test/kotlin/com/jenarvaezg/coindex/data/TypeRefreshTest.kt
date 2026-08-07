package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.ApiCallEntity
import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
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
import kotlinx.serialization.json.Json

/** The ficha of N#596807 as it arrived: a draft whose family is a half-typed «The» (#184, #186). */
private const val DRAFT = """{"id": 596807, "title": "1 Onza", "series": "The"}"""

/** The same ficha once the referee published it. */
private const val PUBLISHED =
    """{"id": 596807, "title": "1 Onza", "series": "The Perth Mint's Wedge-tailed Eagle"}"""

private val json = Json { ignoreUnknownKeys = true }

private fun dtoOf(body: String): NumistaTypeDto = json.decodeFromString(body)

class TypeRefreshTest {
    private val types = FakeTypeMetaDao()
    private val calls = FakeApiCallDao()
    private val refresh = TypeRefresh(types) { 1_000L }

    private fun client(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        budget: CallBudget = LoggingBudget(),
    ): NumistaClient {
        val engine = MockEngine {
            respond(body, status, headersOf("Content-Type", "application/json"))
        }
        return NumistaClient(HttpClient(engine), "key", budget, "https://api.example/v3") { 1_000L }
    }

    /** Records into the same log the report counts, like the real gate does. */
    private inner class LoggingBudget(private val exhausted: Boolean = false) : CallBudget {
        override suspend fun reserve(endpoint: String) {
            if (exhausted) throw NumistaException.BudgetExhausted(1_500, 1_500)
            calls.record(ApiCallEntity(endpoint = endpoint, calledAt = 1_000L))
        }
    }

    private suspend fun cacheTheDraft() {
        types.insertIfAbsent(
            typeMetaEntity(596_807, dtoOf(DRAFT), DRAFT, fetchedAt = 100L),
        )
    }

    @Test
    fun `the corrected ficha replaces the cached one and costs a single call`() = runTest {
        cacheTheDraft()

        val report = refresh.refresh(client(PUBLISHED), 596_807)

        // One reserved call and no token: `/types/{id}` needs none.
        assertEquals(listOf("/types/596807"), calls.calls.map { it.endpoint })
        assertTrue(report.changed)
        val row = types.rows.value.single()
        assertEquals("The Perth Mint's Wedge-tailed Eagle", row.family)
        // The mapper reads `raw`, so the stored body has to be the new one too, not the draft's.
        assertTrue(row.raw.contains("Wedge-tailed"), row.raw)
        // And when it was brought, so «esta ficha es de hace ocho meses» stops being a guess.
        assertEquals(1_000L, row.fetchedAt)
    }

    @Test
    fun `a ficha nobody has corrected yet reports no change`() = runTest {
        cacheTheDraft()

        val report = refresh.refresh(client(DRAFT), 596_807)

        assertEquals(false, report.changed)
        // Asked again is still newer than it was: the row is rewritten with today's date.
        assertEquals(1_000L, types.rows.value.single().fetchedAt)
    }

    @Test
    fun `a seeded ficha is not a change just because this app re-encoded the snapshot`() = runTest {
        // What `TypeCacheSeed` stores: the asset element re-encoded by kotlinx — compact, and in
        // whatever key order the snapshot file happens to carry. Byte-comparing that against
        // Numista's own body would have announced a change on every seeded ficha in the cache.
        val reEncoded = json.parseToJsonElement(
            """{"series": "The", "title": "1 Onza", "id": 596807}""",
        ).toString()
        types.insertIfAbsent(typeMetaEntity(596_807, dtoOf(reEncoded), reEncoded, fetchedAt = 100L))

        val report = refresh.refresh(client(DRAFT), 596_807)

        assertEquals(false, report.changed)
    }

    @Test
    fun `a field only the raw body carries still counts as a change`() = runTest {
        // The metal, the issuer's name, the diameter and the category are read out of `raw` on every
        // pass (see Mappers.kt), so a corrected composition is a change the collector can see.
        val withoutMetal = """{"id": 596807, "title": "1 Onza", "series": "The"}"""
        val withMetal =
            """{"id": 596807, "title": "1 Onza", "series": "The",
               "composition": {"text": "Plata .9999"}}"""
        types.insertIfAbsent(
            typeMetaEntity(596_807, dtoOf(withoutMetal), withoutMetal, fetchedAt = 100L),
        )

        val report = refresh.refresh(client(withMetal), 596_807)

        assertTrue(report.changed)
        assertTrue(types.rows.value.single().raw.contains("Plata .9999"))
    }

    @Test
    fun `a type the phone never had is a change, not a comparison against nothing`() = runTest {
        val report = refresh.refresh(client(PUBLISHED), 596_807)

        assertTrue(report.changed)
        assertEquals(596_807, types.rows.value.single().typeId)
    }

    @Test
    fun `an exhausted budget leaves the cached ficha exactly as it was`() = runTest {
        cacheTheDraft()

        assertFailsWith<NumistaException.BudgetExhausted> {
            refresh.refresh(client(PUBLISHED, budget = LoggingBudget(exhausted = true)), 596_807)
        }

        val row = types.rows.value.single()
        assertEquals("The", row.family)
        assertEquals(100L, row.fetchedAt)
        assertTrue(calls.calls.isEmpty())
    }

    @Test
    fun `a type Numista no longer publishes never deletes the ficha on the phone`() = runTest {
        cacheTheDraft()

        assertFailsWith<NumistaException.Api> {
            refresh.refresh(client("""{"error":"not found"}""", HttpStatusCode.NotFound), 596_807)
        }

        assertEquals("The", types.rows.value.single().family)
    }
}
