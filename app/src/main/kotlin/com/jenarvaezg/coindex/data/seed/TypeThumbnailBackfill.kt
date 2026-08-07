package com.jenarvaezg.coindex.data.seed

import com.jenarvaezg.coindex.data.db.TypeMetaDao
import com.jenarvaezg.coindex.data.numista.NumistaTypeDto
import com.jenarvaezg.coindex.data.ficha.thumbnails
import kotlinx.serialization.json.Json

/**
 * Fills in the thumbnail URLs of a type cache written before version 3 (issue #67).
 *
 * A cached type is never requested again — that is the point of the cache — so a collector who
 * already synced would keep asking for the heavy originals for ever, which is exactly the plate
 * with holes in it. Nothing has to be re-fetched: `TypeMetaEntity.raw` keeps the whole ficha as
 * Numista sent it, thumbnails included, which is the case that comment was written for.
 *
 * It runs at every start and costs one `COUNT` once the cache is filled in. What it cannot fix
 * is a ficha with no thumbnail on either face: there is nothing to write, so the row is read
 * again on the next start. That set never grows — every write since version 3 sets both columns
 * — and it is empty in the shipped snapshot, all 687 of whose fichas have both. A handful of
 * small JSON blobs re-read at start is cheaper than a column to remember them by.
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
