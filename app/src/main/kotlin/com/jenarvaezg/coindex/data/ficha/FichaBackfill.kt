package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.db.TypeMetaDao

/**
 * How many bodies are read and written as one unit.
 *
 * The whole cache is a couple of thousand fichas of a few kilobytes each, so a pass over all of
 * them at once would hold megabytes of JSON and — worse — commit two thousand separate
 * transactions, each one an `fsync`, on the very first launch after the update. A batch bounds
 * both, and committing per batch rather than per pass keeps the work resumable: an app killed
 * halfway resumes where it got to instead of starting again.
 */
private const val BATCH = 200

/**
 * Reads the body of every ficha whose columns an older reading wrote — or none did (#221).
 *
 * A cached type is never fetched again, which is the point of the cache, so the five columns of
 * version 6 would stay empty for ever on the two phones that already have the app: no issuer name
 * on any card, no metal on any chip, no diameter in the printed notebook and no QR. Nothing has to
 * be re-fetched, because `TypeMetaEntity.raw` keeps the whole ficha as Numista sent it — the same
 * bargain `TypeThumbnailBackfill` was built on.
 *
 * It also runs after [FICHA_READING] is bumped, which is what makes a column no worse than the
 * read-on-every-pass it replaced: improving what the body says about a type costs one pass at the
 * next start, not one API call per row.
 *
 * It costs one `COUNT` once the cache has been read, and unlike the thumbnails it converges: the
 * marker is the version, not «is the column null», so a ficha that genuinely has no composition is
 * read once and never again.
 */
class FichaBackfill(private val typeMeta: TypeMetaDao) {
    /** Returns how many rows were read. */
    suspend fun run(): Int {
        if (typeMeta.countReadBefore(FICHA_READING) == 0) return 0
        var read = 0
        while (true) {
            // Every batch marks its own rows, so the next query answers with the next ones: the
            // loop needs no offset and cannot re-read what it has just written.
            val batch = typeMeta.rawReadBefore(FICHA_READING, BATCH)
            if (batch.isEmpty()) return read
            typeMeta.setReadings(
                batch.associate { row -> row.typeId to readFichaBody(row.raw) },
                FICHA_READING,
            )
            read += batch.size
        }
    }
}
