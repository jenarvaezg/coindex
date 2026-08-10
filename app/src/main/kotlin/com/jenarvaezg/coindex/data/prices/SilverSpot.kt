package com.jenarvaezg.coindex.data.prices

import com.jenarvaezg.coindex.data.db.MetalSpotEntity
import com.jenarvaezg.coindex.data.db.PriceDao
import com.jenarvaezg.coindex.domain.SilverSpot
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The symbol the one spot row is keyed by. Silver is the only metal either collection holds. */
const val SILVER_SYMBOL: String = "XAG"

/** A spot older than this is read again. A day, because it is two keyless calls (ADR 0028 §5). */
private const val SPOT_LIFETIME_MILLIS = 24L * 60 * 60 * 1_000

private const val SILVER_URL = "https://api.gold-api.com/price/XAG"
private const val RATE_URL = "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR"

@Serializable
private data class GoldApiPrice(val price: Double? = null, @SerialName("symbol") val symbol: String? = null)

@Serializable
private data class FrankfurterRates(val rates: Map<String, Double>? = null)

/**
 * The troy ounce of silver in euros, from two calls that cost no API budget.
 *
 * `api.gold-api.com` gives the ounce in dollars and `api.frankfurter.dev` the ECB rate; **neither is
 * `api.numista.com`**, so neither is counted against the budget of ADR 0003 — the same distinction
 * ADR 0024 draws for CDN photographs, and it must not start being counted as one.
 *
 * Two calls and not one because there is no keyless source for the ounce in euros. The alternative was
 * a keyed provider, and a key in the APK of a two-user app is a key in a public repository.
 */
interface SpotReader {
    /** The spot right now, or null when either call fails. A failure writes nothing (ADR 0028 §4). */
    suspend fun read(): Double?
}

class HttpSpotReader(private val httpClient: HttpClient) : SpotReader {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): Double? {
        val usdPerOunce = fetch<GoldApiPrice>(SILVER_URL)?.price ?: return null
        val eurPerUsd = fetch<FrankfurterRates>(RATE_URL)?.rates?.get("EUR") ?: return null
        return (usdPerOunce * eurPerUsd).takeIf { it.isFinite() && it > 0.0 }
    }

    /**
     * One call, and a failure of any kind is null.
     *
     * Nothing here throws: the spot is the one number on the page that arrives on its own, and a
     * network that is down has to leave the money section absent rather than take the app with it.
     */
    private suspend inline fun <reified T> fetch(url: String): T? = runCatching {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) return null
        json.decodeFromString<T>(response.bodyAsText())
    }.getOrNull()
}

/**
 * The last spot this phone read, and whether it is worth reading again.
 *
 * **Expired is not deleted** (ADR 0028 §5): a spot from last month is still handed out, with the date
 * it was brought, and a phone with no network says an old total instead of emptying itself. That lies
 * very little — a 3 % swing in silver moves the total by 1,9 %, because the catalogue rules the mix.
 */
class SpotStore(
    private val prices: PriceDao,
    private val reader: SpotReader,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    /**
     * Reads a fresh spot if the stored one is a day old, and returns whichever one is now on the phone.
     *
     * The stored value is returned unchanged when the read fails, which is what makes this safe to call
     * on every launch.
     */
    suspend fun refresh(): SilverSpot? {
        val stored = stored()
        val now = nowMillis()
        if (stored != null && now - stored.readAtMillis < SPOT_LIFETIME_MILLIS) return stored
        val fresh = reader.read() ?: return stored
        prices.putSpot(MetalSpotEntity(SILVER_SYMBOL, fresh, now))
        return SilverSpot(fresh, now)
    }

    /** Whatever is on the phone, without asking anybody. */
    suspend fun stored(): SilverSpot? = prices.spot(SILVER_SYMBOL)?.toDomain()
}

fun MetalSpotEntity.toDomain(): SilverSpot = SilverSpot(eurPerTroyOunce, readAt)
