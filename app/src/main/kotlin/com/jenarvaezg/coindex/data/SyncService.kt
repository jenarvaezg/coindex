package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.data.db.CollectedItemDao
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.numista.NumistaClient
import com.jenarvaezg.coindex.data.numista.NumistaException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

/** Outcome of one explicit sync. */
data class SyncReport(
    val collectionItems: Int,
    val typesFetched: Int,
    val typesStillMissing: Int,
    val callsSpent: Int,
    /** Set when the collection was saved but type metadata could not be completed. */
    val partialFailure: String? = null,
)

/**
 * Explicit, user-triggered sync. There is no estimator and no dry run: the pre-call budget
 * counter is the source of truth for what a sync costs.
 *
 * The collection snapshot is stored *before* type metadata is fetched, so a sync that runs out
 * of budget half way still leaves a fresh inventory; the types it could not fetch simply show
 * up as unclassified pieces until the next sync completes them.
 */
class SyncService(
    private val collectedItems: CollectedItemDao,
    private val typeMeta: TypeMetaDao,
    private val calls: ApiCallLedger,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param maxFichas how many missing fichas this run may buy. Unlimited for a press, and capped
     *   for the automatic refresh of #605: whoever asked for it is not watching, so a cache emptied
     *   by a reinstall must not turn one launch into two hundred consultas. What is left over is
     *   still counted in [SyncReport.typesStillMissing] and is bought by the next run.
     */
    suspend fun run(client: NumistaClient, userId: Long, maxFichas: Int = Int.MAX_VALUE): SyncReport {
        val callsBefore = recordedCalls()
        val response = client.fetchCollectedItems(userId)
        val items = response.value.items
            ?: throw NumistaException.InvalidResponse(
                "/users/$userId/collected_items",
                "falta el campo `items`; se conserva el inventario anterior",
            )
        val rawItems = runCatching {
            response.raw.let(json::parseToJsonElement).jsonObject["items"] as? JsonArray
        }.getOrNull()
        val syncedAt = nowMillis()
        val entities = items.mapIndexedNotNull { index, item ->
            item.toEntity(rawItems?.getOrNull(index)?.toString() ?: "{}", syncedAt)
        }
        if (entities.isEmpty()) {
            // An empty response would otherwise silently wipe a good snapshot.
            throw NumistaException.InvalidResponse(
                "/users/$userId/collected_items",
                "la colección llegó vacía; se conserva el inventario anterior",
            )
        }
        collectedItems.replaceAll(entities)

        val cached = typeMeta.cachedTypeIds().toSet()
        val missing = entities.map { it.typeId }.distinct().sorted().filterNot { it in cached }
        var fetched = 0
        var failure: String? = null
        for (typeId in missing.take(maxFichas)) {
            try {
                val type = client.fetchType(typeId)
                typeMeta.insertIfAbsent(
                    typeMetaEntity(typeId, type.value, type.raw, nowMillis()),
                )
                fetched += 1
            } catch (error: NumistaException) {
                failure = error.message
                break
            }
        }
        return SyncReport(
            collectionItems = entities.size,
            typesFetched = fetched,
            typesStillMissing = missing.size - fetched,
            callsSpent = recordedCalls() - callsBefore,
            partialFailure = failure,
        )
    }

    /** Calls actually recorded this month, including the OAuth token request. */
    private suspend fun recordedCalls(): Int = calls.spentThisMonth()
}
