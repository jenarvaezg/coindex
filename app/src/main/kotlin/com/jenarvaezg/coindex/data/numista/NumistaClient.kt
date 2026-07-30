package com.jenarvaezg.coindex.data.numista

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

const val NUMISTA_BASE_URL: String = "https://api.numista.com/v3"
private const val API_KEY_HEADER = "Numista-API-Key"

/** Renew a little before the token actually dies; Numista tokens last about ten minutes. */
private const val TOKEN_MARGIN_MILLIS = 60_000L

sealed class NumistaException(message: String) : Exception(message) {
    class EmptyApiKey : NumistaException("La API key de Numista no puede estar vacía")

    class BudgetExhausted(val used: Int, val budget: Int) : NumistaException(
        "Presupuesto mensual de la API agotado ($used/$budget)",
    )

    class Api(val endpoint: String, val status: Int, val body: String) : NumistaException(
        "Numista devolvió HTTP $status en $endpoint",
    )

    class InvalidResponse(val endpoint: String, val detail: String) : NumistaException(
        "Respuesta inesperada de Numista en $endpoint: $detail",
    )

    class Transport(val endpoint: String, cause: Throwable) : NumistaException(
        "No se pudo contactar con Numista en $endpoint: ${cause.message}",
    )
}

/**
 * Gate consulted before every request. It both rejects calls over the monthly cap and records
 * the ones that go out, so the counter can never drift below reality.
 */
interface CallBudget {
    suspend fun reserve(endpoint: String)
}

/** A parsed response plus the untouched body, which callers persist verbatim. */
data class RawResponse<T>(val value: T, val raw: String)

/**
 * Numista API v3 client.
 *
 * Two headers are needed on collection endpoints: `Numista-API-Key` and a bearer token from
 * `oauth_token` with **`scope=view_collection`** — omitting the scope produces a misleading
 * 401. The token is cached in memory and renewed with a margin, never requested per call.
 */
class NumistaClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val budget: CallBudget,
    private val baseUrl: String = NUMISTA_BASE_URL,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenMutex = Mutex()
    private var cachedToken: CachedToken? = null

    private data class CachedToken(val value: String, val renewAtMillis: Long)

    suspend fun fetchCollectedItems(userId: Long): RawResponse<CollectedItemsResponse> {
        val token = accessToken()
        return request("/users/$userId/collected_items") {
            header(API_KEY_HEADER, apiKey)
            header("Authorization", "Bearer $token")
        }
    }

    suspend fun fetchType(typeId: Int): RawResponse<NumistaTypeDto> =
        request("/types/$typeId") {
            parameter("lang", "es")
            header(API_KEY_HEADER, apiKey)
        }

    private suspend fun accessToken(): String {
        // Holding the lock across the refresh coalesces simultaneous refreshes into one call.
        tokenMutex.withLock {
            val cached = cachedToken
            if (cached != null && cached.renewAtMillis > nowMillis()) {
                return cached.value
            }
            val endpoint = "/oauth_token"
            val response: RawResponse<OAuthTokenResponse> = request(endpoint) {
                parameter("grant_type", "client_credentials")
                parameter("scope", "view_collection")
                header(API_KEY_HEADER, apiKey)
            }
            val value = response.value.accessToken?.takeIf(String::isNotEmpty)
                ?: throw NumistaException.InvalidResponse(endpoint, "falta `access_token`")
            val lifetimeMillis = (response.value.expiresIn?.takeIf { it > 0 }
                ?: throw NumistaException.InvalidResponse(endpoint, "falta `expires_in`")) * 1_000
            val margin = minOf(TOKEN_MARGIN_MILLIS, lifetimeMillis / 2)
            cachedToken = CachedToken(value, nowMillis() + lifetimeMillis - margin)
            return value
        }
    }

    private suspend inline fun <reified T> request(
        endpoint: String,
        crossinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
    ): RawResponse<T> {
        if (apiKey.isBlank()) throw NumistaException.EmptyApiKey()
        budget.reserve(endpoint)
        val response = try {
            httpClient.get("$baseUrl$endpoint") { configure() }
        } catch (error: NumistaException) {
            throw error
        } catch (error: Exception) {
            throw NumistaException.Transport(endpoint, error)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw NumistaException.Api(endpoint, response.status.value, body)
        }
        val parsed = try {
            json.decodeFromString<T>(body)
        } catch (error: IllegalArgumentException) {
            throw NumistaException.InvalidResponse(endpoint, error.message ?: "JSON inválido")
        }
        return RawResponse(parsed, body)
    }
}
