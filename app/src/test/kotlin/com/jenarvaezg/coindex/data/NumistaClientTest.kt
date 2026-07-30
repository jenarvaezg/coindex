package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.numista.CallBudget
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
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

private class RecordingBudget(private val cap: Int = 100) : CallBudget {
    val endpoints = mutableListOf<String>()

    override suspend fun reserve(endpoint: String) {
        if (endpoints.size >= cap) throw NumistaException.BudgetExhausted(endpoints.size, cap)
        endpoints += endpoint
    }
}

class NumistaClientTest {
    private fun client(
        budget: CallBudget,
        vararg responses: Pair<String, String>,
        nowMillis: () -> Long = { 0L },
    ): Pair<NumistaClient, MutableList<String>> {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            requested += request.url.toString()
            val body = responses.firstOrNull { (fragment, _) ->
                request.url.encodedPath.contains(fragment)
            }?.second ?: error("respuesta no preparada para ${request.url}")
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }
        val http = HttpClient(engine)
        return NumistaClient(http, "test-key", budget, "https://api.example/v3", nowMillis) to
            requested
    }

    @Test
    fun `the token request always carries the view_collection scope`() = runTest {
        val budget = RecordingBudget()
        val (numista, requested) = client(
            budget,
            "oauth_token" to Fixtures.oauthToken,
            "collected_items" to Fixtures.collectedItems,
        )

        val response = numista.fetchCollectedItems(2104)

        val tokenRequest = requested.first { it.contains("oauth_token") }
        assertTrue(tokenRequest.contains("scope=view_collection"), tokenRequest)
        assertTrue(tokenRequest.contains("grant_type=client_credentials"), tokenRequest)
        assertEquals(2, response.value.items?.size)
        assertEquals(listOf("/oauth_token", "/users/2104/collected_items"), budget.endpoints)
    }

    @Test
    fun `the token is cached across calls and renewed with a margin`() = runTest {
        val budget = RecordingBudget()
        var now = 0L
        val (numista, requested) = client(
            budget,
            "oauth_token" to Fixtures.oauthToken,
            "collected_items" to Fixtures.collectedItems,
            nowMillis = { now },
        )

        numista.fetchCollectedItems(2104)
        numista.fetchCollectedItems(2104)
        assertEquals(1, requested.count { it.contains("oauth_token") })

        // The fixture token lives 7200 s; past its renewal point a new one is requested.
        now = 7_200_000L
        numista.fetchCollectedItems(2104)
        assertEquals(2, requested.count { it.contains("oauth_token") })
    }

    @Test
    fun `type metadata is requested in spanish and parsed from a real capture`() = runTest {
        val (numista, requested) = client(
            RecordingBudget(),
            "types/404044" to Fixtures.type(404_044),
        )

        val type = numista.fetchType(404_044)

        assertTrue(requested.single().contains("lang=es"), requested.single())
        assertEquals(404_044, type.value.id)
        assertTrue(type.raw.isNotEmpty())
    }

    @Test
    fun `an exhausted budget refuses the request instead of sending it`() = runTest {
        val budget = RecordingBudget(cap = 0)
        val (numista, requested) = client(budget, "oauth_token" to Fixtures.oauthToken)

        assertFailsWith<NumistaException.BudgetExhausted> { numista.fetchType(420) }

        assertTrue(requested.isEmpty())
    }

    @Test
    fun `an http error names the endpoint and status`() = runTest {
        val engine = MockEngine {
            respond("no autorizado", HttpStatusCode.Unauthorized)
        }
        val numista = NumistaClient(
            HttpClient(engine),
            "test-key",
            RecordingBudget(),
            "https://api.example/v3",
        ) { 0L }

        val error = assertFailsWith<NumistaException.Api> { numista.fetchType(420) }

        assertEquals(401, error.status)
        assertEquals("/types/420", error.endpoint)
    }

    @Test
    fun `a blank api key is rejected before any budget is spent`() = runTest {
        val budget = RecordingBudget()
        val numista = NumistaClient(
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            "   ",
            budget,
            "https://api.example/v3",
        ) { 0L }

        assertFailsWith<NumistaException.EmptyApiKey> { numista.fetchType(420) }
        assertTrue(budget.endpoints.isEmpty())
    }
}
