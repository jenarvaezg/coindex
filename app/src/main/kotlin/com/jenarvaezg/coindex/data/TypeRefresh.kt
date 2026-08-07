package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.ficha.readFichaBody
import com.jenarvaezg.coindex.data.numista.NumistaClient
import kotlinx.serialization.json.Json

/** What one refresh found. It always costs one call, so there is no count to report. */
data class TypeRefreshReport(val typeId: Int, val changed: Boolean)

/**
 * Asks Numista again for one type's ficha and writes it over the cached one (#185, ADR 0025).
 *
 * The type cache is permanent by design — a ficha costs an API call and catalog data barely ever
 * moves — and that was the right call for everything except the case that matters: **the data was
 * wrong and somebody fixed it**. A corrected family, a weight the mint published, a submission the
 * referee finally accepted; none of it had any way of reaching the two phones that exist, because a
 * cached type was never asked for again and the asset snapshot never overwrites a synced row
 * (ADR 0017).
 *
 * One type, one call, and the collector asks for it. There is no batch here and no schedule: the
 * cheapest way to spend a month's budget in an afternoon is to refresh what nobody said was wrong.
 */
class TypeRefresh(
    private val typeMeta: TypeMetaDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches the ficha and stores it. Throws whatever the client throws — an exhausted budget, a
     * dead network, a type Numista no longer publishes — and in every one of those cases the ficha
     * already on the phone is left exactly as it was: a refresh that fails is never worse than not
     * having asked.
     */
    suspend fun refresh(client: NumistaClient, typeId: Int): TypeRefreshReport {
        val cached = typeMeta.byId(typeId)
        val type = client.fetchType(typeId)
        val ficha = typeMetaEntity(typeId, type.value, type.raw, nowMillis())
        typeMeta.overwrite(ficha)
        return TypeRefreshReport(typeId, changed = cached == null || differ(cached, ficha))
    }

    /**
     * Whether the ficha says anything different from the one that was there.
     *
     * The columns are compared as columns, and the bodies as **parsed JSON** rather than as bytes.
     * Comparing the two strings would report a change on every seeded ficha, because the snapshot
     * stores the asset re-encoded by this app and a refresh stores Numista's own body. A parsed
     * [kotlinx.serialization.json.JsonObject] is a `Map`, so key order and whitespace stop counting
     * and the fields start — and the issuer's name, the composition prose, the diameter and the
     * category are inside it, so a corrected metal is a change even though no column moved.
     *
     * The cached row is re-read before the columns are compared. Those five *are* columns since
     * version 6 (#221), and a row the backfill has not reached yet holds nulls in them: without
     * this, the first refresh after an update would report «cambió» over a ficha that says exactly
     * the same thing.
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
