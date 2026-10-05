package com.jenarvaezg.coindex.data.seed

import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.ficha.thumbnails
import kotlinx.serialization.json.Json

/**
 * Fills in the thumbnail URLs of a type cache written before version 3 (#67) from the stored
 * `TypeMetaEntity.raw`, since a cached type is never fetched again. Runs at every start and costs
 * one `COUNT` once done. A ficha with no thumbnail on either face is read again each start; such
 * rows are rare and don't grow, so that is cheaper than a column to mark them.
 */
class TypeThumbnailBackfill(private val typeMeta: TypeMetaDao) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Returns how many rows were given a thumbnail. */
    suspend fun run(): Int {
        if (typeMeta.countWithoutThumbnails() == 0) return 0
        var filled = 0
        typeMeta.rawWithoutThumbnails().forEach { row ->
            val ficha = runCatching {
                json.decodeFromString(NumistaTypeDto.serializer(), row.raw)
            }.getOrNull() ?: return@forEach
            val thumbnails = ficha.thumbnails()
            if (thumbnails.isEmpty) return@forEach
            typeMeta.setThumbnails(row.typeId, thumbnails.obverse, thumbnails.reverse)
            filled += 1
        }
        return filled
    }
}
