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

/** What one top-up did, which is two different things and was worth telling apart (#606). */
data class SeedReport(val added: Int, val overwritten: Int) {
    val touched: Int get() = added + overwritten
}

/**
 * Seeds the permanent type cache from the snapshot the curator records with their own key.
 *
 * That snapshot covers every type the curated catalogs name, so plates can show all designs —
 * including the ones the collector is missing — without any collector spending their own budget on
 * them. It costs the curator hundreds of consultas and it costs the two phones none.
 */
class TypeCacheSeed(
    private val typeMeta: TypeMetaDao,
    /** Where the last applied version is remembered. See [topUp]. */
    private val values: NamedValues,
    /** The `versionCode` of the running APK, which is this snapshot's name (#606). */
    private val installedVersionCode: Int,
    /** The snapshot, read only when there is something to do: it is 2,4 MB of JSON. */
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
     * Tops the cache up with what a curated file names and the phone does not have, and — **once per
     * installed version** — writes the snapshot over the fichas it already had (#606, ADR 0033).
     *
     * It used to seed only into an empty cache, which made the snapshot a **first-install** gift:
     * every catalog curated afterwards shipped its fichas in the asset and none of them ever reached
     * a phone that already had the app. That is most of the plate the collector reported with 7
     * pictures out of 19 (issue #67), and adding the missing ones fixed it.
     *
     * What it did not fix is the ficha that is **there and wrong**. A corrected family, a weight the
     * mint published, a submission the referee finally accepted: the curator re-seeds it with
     * `scripts/seed-type-cache.py --refresh`, the asset travels in the APK, and the row does not
     * move, because an `insertIfAbsent` ignores the conflict by design. Since #185 there is a route,
     * but it is the collector's own gesture over one type he has already seen is wrong — no way to
     * reach the nine Peruvian fichas of #603, and no way at all to reach what he cannot know is
     * wrong.
     *
     * **The version is the clock, and it is the right one.** A snapshot travels inside exactly one
     * APK, so «has this snapshot been applied here?» is «has this `versionCode` been applied here?»,
     * and the answer is one integer in a preferences file. It also protects the gesture without a
     * date: the seed writes once when the update lands, and a ficha the collector refreshes by hand
     * afterwards stays his until the next release. What it cannot see is a hand refresh made between
     * the curator taking the snapshot and the release shipping it — hours, in a repository where the
     * snapshot and the release travel in the same pull request — and in that window what overwrites
     * him is the curated datum, verified against numista.com before it was versioned.
     *
     * [requiredTypeIds] is what the curated files name, which is exactly what a plate can ask to
     * draw. Comparing it against the cached ids costs one column of integers, and the 2,4 MB
     * snapshot is parsed only on the starts that have something to do: a version already applied
     * with nothing missing reads two cheap things and returns.
     */
    suspend fun topUp(requiredTypeIds: Set<Int>): SeedReport {
        val applied = values.int32(KEY_APPLIED_VERSION)
        val cached = typeMeta.cachedTypeIds().toSet()
        val nothingMissing = cached.isNotEmpty() && cached.containsAll(requiredTypeIds)
        if (nothingMissing && applied == installedVersionCode) return SeedReport(0, 0)

        val fichas = readSnapshot()
        val (known, fresh) = fichas.partition { it.typeId in cached }
        typeMeta.insertIfAbsent(fresh)
        // The first install has nothing to overwrite, and saying so costs nothing: `cached` is empty,
        // so `known` is empty and the batch is not even sent.
        val overwritten = if (applied != installedVersionCode) known else emptyList()
        typeMeta.overwrite(overwritten)
        rememberVersion()
        return SeedReport(added = fresh.size, overwritten = overwritten.size)
    }

    // Written after the writes and not before, so a process killed halfway leaves the version
    // unapplied and the next start does the whole of it again. Re-applying a snapshot is idempotent;
    // skipping one is a wrong ficha that stays for a release.
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
