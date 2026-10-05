package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.ficha.readFichaBody
import com.jenarvaezg.coindex.data.numista.NumistaClient
import kotlinx.serialization.json.Json

/** What one refresh found. It always costs one call, so there is no count to report. */
data class TypeRefreshReport(val typeId: Int, val changed: Boolean)

/**
 * Asks Numista again for one type's ficha and writes it over the cached one (#185, ADR 0025): the
 * way a ficha corrected on Numista reaches a phone whose cache would otherwise keep it. One type,
 * one call, on the collector's request; no batch or schedule, which could spend a month's budget on
 * fichas nobody said were wrong.
 */
class TypeRefresh(
    private val typeMeta: TypeMetaDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches the ficha and stores it. Throws whatever the client throws (budget, network, a type
     * Numista no longer publishes), leaving the cached ficha untouched.
     */
    suspend fun refresh(client: NumistaClient, typeId: Int): TypeRefreshReport {
        val cached = typeMeta.byId(typeId)
        val type = client.fetchType(typeId)
        val ficha = typeMetaEntity(typeId, type.value, type.raw, nowMillis())
        typeMeta.overwrite(ficha)
        return TypeRefreshReport(typeId, changed = cached == null || differ(cached, ficha))
    }

    /**
     * Whether the ficha says anything different from the one that was there. Bodies are compared
     * as parsed JSON, not bytes: the seed stores the asset re-encoded and a refresh stores
     * Numista's own body. The cached row is read again first, so columns the backfill hasn't
     * filled yet (#221) don't count as a change.
     */
    private fun differ(cached: TypeMetaEntity, fetched: TypeMetaEntity): Boolean {
        val reread = cached.withReading(readFichaBody(cached.raw))
        val columnsDiffer = reread.copy(fetchedAt = fetched.fetchedAt, raw = fetched.raw) != fetched
        return columnsDiffer || parse(cached.raw) != parse(fetched.raw)
    }

    /** A body that cannot be parsed compares by its text, which is the best it can do. */
    private fun parse(raw: String): Any =
        runCatching { json.parseToJsonElement(raw) }.getOrDefault(raw)
}
