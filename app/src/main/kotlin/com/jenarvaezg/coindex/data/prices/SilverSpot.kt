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
 * The troy ounce of silver in euros: `api.gold-api.com` gives the ounce in dollars and
 * `api.frankfurter.dev` the ECB rate. Neither is `api.numista.com`, so neither counts against the
 * budget of ADR 0003, as with the CDN photographs of ADR 0024. Two calls because there is no keyless
 * source in euros, and a key in the APK would be a key in a public repository.
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

    /** One call; any failure is null, so a dead network leaves the money section absent. */
    private suspend inline fun <reified T> fetch(url: String): T? = runCatching {
        val response = httpClient.get(url)
        if (!response.status.isSuccess()) return null
        json.decodeFromString<T>(response.bodyAsText())
    }.getOrNull()
}

/**
 * The last spot this phone read, and whether it is worth reading again. Expired is not deleted
 * (ADR 0028 §5): an old spot is still used, with its date, so an offline phone shows an old total
 * rather than none.
 */
class SpotStore(
    private val prices: PriceDao,
    private val reader: SpotReader,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    /**
     * Reads a fresh spot if the stored one is a day old, and returns whichever is now on the phone;
     * the stored one if the read fails, so it is safe to call on every launch.
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
