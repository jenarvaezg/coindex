package com.jenarvaezg.coindex.data.seed

import android.content.res.AssetManager
import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.db.TypeMetaEntity
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

private const val TYPE_CACHE_ASSET = "numista-type-cache.json"

/**
 * Seeds the permanent type cache from the snapshot recorded by the frozen Rust
 * implementation.
 *
 * That snapshot cost around 630 API calls to build and covers every type referenced by the
 * curated catalogs, so plates can show all designs — including the ones the collector is
 * missing — without any user spending their own budget on them.
 */
class TypeCacheSeed(
    private val typeMeta: TypeMetaDao,
    /** The snapshot, read only when there is something to add: it is 2.4 MB of JSON. */
    private val snapshot: () -> String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        fun fromAssets(assets: AssetManager, typeMeta: TypeMetaDao): TypeCacheSeed =
            TypeCacheSeed(typeMeta) {
                assets.open(TYPE_CACHE_ASSET).use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                }
            }
    }

    /**
     * Tops the cache up with any type a curated file names and the phone does not have yet.
     * Returns the rows added.
     *
     * It used to seed only into an empty cache, which made the snapshot a **first-install**
     * gift: every catalog curated afterwards shipped its fichas in the asset and none of them
     * ever reached a phone that already had the app. And a cached type is never re-fetched, so
     * the missing fichas had no second route in — the cells stayed silhouettes for good. That
     * is most of the plate the collector reported with 7 pictures out of 19 (issue #67).
     *
     * [requiredTypeIds] is what the curated files name, which is exactly what a plate can ask
     * to draw: comparing it against the cached ids costs one column of integers, and the 2.4 MB
     * snapshot is only parsed on the starts that actually have something to add.
     *
     * Nothing is overwritten: the insert ignores conflicts, so a ficha the collector paid API
     * budget to sync stays as it was synced.
     */
    suspend fun topUp(requiredTypeIds: Set<Int>): Int {
        val cached = typeMeta.cachedTypeIds().toSet()
        if (cached.isNotEmpty() && cached.containsAll(requiredTypeIds)) return 0
        val entities = readSnapshot().filterNot { it.typeId in cached }
        typeMeta.insertIfAbsent(entities)
        return entities.size
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

/**
 * The small picture of each face, read from a ficha the same way wherever it is read from.
 *
 * The cache is written from two places — the snapshot on the way in, and the backfill over the
 * fichas already stored — and a plate is only whole if both agree on what a thumbnail is.
 */
data class FichaThumbnails(val obverse: String?, val reverse: String?) {
    val isEmpty: Boolean = obverse == null && reverse == null
}

fun NumistaTypeDto.thumbnails(): FichaThumbnails =
    FichaThumbnails(obverse = obverse?.thumbnail, reverse = reverse?.thumbnail)

/** Maps a Numista type response onto the cache row, keeping the untouched body. */
fun typeMetaEntity(
    typeId: Int,
    dto: NumistaTypeDto,
    raw: String,
    fetchedAt: Long,
): TypeMetaEntity = TypeMetaEntity(
    typeId = typeId,
    title = dto.title,
    family = dto.series,
    issuerCode = dto.issuer?.code,
    minYear = dto.minYear,
    maxYear = dto.maxYear,
    weightGrams = dto.weight,
    obverseUrl = dto.obverse?.picture ?: dto.obverse?.thumbnail,
    reverseUrl = dto.reverse?.picture ?: dto.reverse?.thumbnail,
    raw = raw,
    fetchedAt = fetchedAt,
    obverseThumbnailUrl = dto.thumbnails().obverse,
    reverseThumbnailUrl = dto.thumbnails().reverse,
)
