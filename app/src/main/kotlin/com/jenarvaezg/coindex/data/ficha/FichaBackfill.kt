package com.jenarvaezg.coindex.data.ficha

import com.jenarvaezg.coindex.data.db.TypeMetaDao

/**
 * How many bodies are read and written as one transaction: bounds the JSON held in memory and the
 * number of `fsync`s, and keeps the pass resumable if the app is killed halfway.
 */
private const val BATCH = 200

/**
 * Fills the columns of every ficha that no reading, or an older one, wrote (#221), from the body
 * `TypeMetaEntity.raw` keeps, since a cached type is never fetched again. It runs again after
 * [FICHA_READING] is bumped. Once caught up it costs one `COUNT`, and since the marker is the
 * version, a ficha with genuinely empty fields is read only once.
 */
class FichaBackfill(private val typeMeta: TypeMetaDao) {
    /** Returns how many rows were read. */
    suspend fun run(): Int {
        if (typeMeta.countReadBefore(FICHA_READING) == 0) return 0
        var read = 0
        while (true) {
            // Each batch marks its rows, so the next query returns the next ones without an offset.
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
