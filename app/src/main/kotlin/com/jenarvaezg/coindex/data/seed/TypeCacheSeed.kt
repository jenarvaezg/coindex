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
    private val assets: AssetManager,
    private val typeMeta: TypeMetaDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Inserts the snapshot without overwriting anything already cached. Returns rows added. */
    suspend fun seedIfNeeded(): Int {
        if (typeMeta.count() > 0) return 0
        val text = assets.open(TYPE_CACHE_ASSET).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
        val snapshot = json.parseToJsonElement(text).jsonObject
        val now = System.currentTimeMillis()
        val entities = snapshot.entries.mapNotNull { (typeIdText, element) ->
            val raw = element as? JsonObject ?: return@mapNotNull null
            val typeId = typeIdText.toIntOrNull() ?: return@mapNotNull null
            val dto = runCatching { json.decodeFromJsonElement(NumistaTypeDto.serializer(), raw) }
                .getOrNull() ?: return@mapNotNull null
            typeMetaEntity(typeId, dto, raw.toString(), now)
        }
        typeMeta.insertIfAbsent(entities)
        return entities.size
    }
}

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
)
