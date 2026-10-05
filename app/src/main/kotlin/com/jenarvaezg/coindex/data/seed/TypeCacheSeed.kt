package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.data.NamedValues
import com.jenarvaezg.coindex.data.Stored
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.int32
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.typeMetaEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private const val TYPE_CACHE_ASSET = "numista-type-cache.json"

/** The preferences file that remembers which APK's snapshot has already been written down. */
const val TYPE_SEED_PREFERENCES: String = "coindex-type-seed"

private const val KEY_APPLIED_VERSION = "applied_version_code"

/** What one top-up did: fichas added and fichas overwritten (#606). */
data class SeedReport(val added: Int, val overwritten: Int) {
    val touched: Int get() = added + overwritten
}

/**
 * Seeds the permanent type cache from the snapshot the curator records with their own key. It
 * covers every type the curated catalogs name, so plates show every design, missing ones included,
 * without the collector spending budget on them.
 */
class TypeCacheSeed(
    private val typeMeta: TypeMetaDao,
    /** Where the last applied version is remembered. See [topUp]. */
    private val values: NamedValues,
    /** The `versionCode` of the running APK, which is this snapshot's name (#606). */
    private val installedVersionCode: Int,
    /** The snapshot, read only when there is something to do: it is megabytes of JSON. */
    private val snapshot: () -> String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        fun fromAssets(
            assets: AssetManager,
            typeMeta: TypeMetaDao,
            values: NamedValues,
            installedVersionCode: Int,
        ): TypeCacheSeed = TypeCacheSeed(typeMeta, values, installedVersionCode) {
            assets.open(TYPE_CACHE_ASSET).use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }
    }

    /**
     * Adds the fichas the curated files name ([requiredTypeIds]) that the phone lacks, on every
     * update and not just the first install (#67), and once per installed version writes the
     * snapshot over the fichas already cached (#606, ADR 0033), so corrections the curator re-seeds
     * with `scripts/seed-type-cache.py --refresh` reach every phone.
     *
     * A snapshot ships in exactly one APK, so the `versionCode` marks whether it was applied. A ficha
     * the collector refreshes by hand after the update stays theirs until the next release. The
     * snapshot is parsed only when something is missing or the version is new.
     */
    suspend fun topUp(requiredTypeIds: Set<Int>): SeedReport {
        val applied = values.int32(KEY_APPLIED_VERSION)
        val cached = typeMeta.cachedTypeIds().toSet()
        val nothingMissing = cached.isNotEmpty() && cached.containsAll(requiredTypeIds)
        if (nothingMissing && applied == installedVersionCode) return SeedReport(0, 0)

        val fichas = readSnapshot()
        val (known, fresh) = fichas.partition { it.typeId in cached }
        typeMeta.insertIfAbsent(fresh)
        // On a first install `cached` is empty, so `known` is too and nothing is overwritten.
        val overwritten = if (applied != installedVersionCode) known else emptyList()
        typeMeta.overwrite(overwritten)
        rememberVersion()
        return SeedReport(added = fresh.size, overwritten = overwritten.size)
    }

    // Written last, so a process killed halfway re-applies the whole snapshot next start, which is
    // idempotent; skipping it would leave wrong fichas for a release.
    private fun rememberVersion() {
        if (values.int32(KEY_APPLIED_VERSION) == installedVersionCode) return
        values.write(mapOf(KEY_APPLIED_VERSION to Stored.Int32(installedVersionCode)))
    }

    private fun readSnapshot(): List<TypeMetaEntity> {
        val fichas = json.parseToJsonElement(snapshot()).jsonObject
        val now = System.currentTimeMillis()
        return fichas.entries.mapNotNull { (typeIdText, element) ->
            val raw = element as? JsonObject ?: return@mapNotNull null
            val typeId = typeIdText.toIntOrNull() ?: return@mapNotNull null
            val dto = runCatching { json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw) }
                .getOrNull() ?: return@mapNotNull null
            typeMetaEntity(typeId, dto, raw.toString(), now)
        }
    }
}
