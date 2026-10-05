package com.jenarvaezg.coindex.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One file carries the whole collection across the share sheet (#548). `DatabaseExportTest` pins
 * naming and order with a fake checkpoint; this needs real SQLite to show that
 * `PRAGMA wal_checkpoint(TRUNCATE)` folds the `-wal` back in. Room runs in WAL mode, and a copy
 * taken without the fold misses the latest writes.
 *
 * The row is read back out of the copy, opened as its own database: an empty `-wal` alone would
 * also be true of a base that never had anything to write.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseCheckpointTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "coindex-checkpoint-test.db"
    private val exports = File(context.cacheDir, "coindex-checkpoint-test")

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
        exports.deleteRecursively()
    }

    @Test
    fun theDumpHoldsWhatWasOnlyInTheWriteAheadLog() {
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, CoindexDatabase::class.java, databaseName).build()
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO wishes (typeId, year, issueId, markedAt) VALUES (404044, 2026, 8508, 1)",
        )
        val base = context.getDatabasePath(databaseName)
        val log = File(base.parentFile, "$databaseName-wal")
        assertTrue("la transacción tenía que estar en el diario", log.length() > 0)

        val export = DatabaseExport(
            source = base,
            directory = exports,
            versionName = { "1.4.8" },
            checkpoint = database::checkpoint,
        )
        val dump = runBlocking { export.write() }
        database.close()

        assertEquals(0L, log.length())
        SQLiteDatabase.openDatabase(dump.path, null, SQLiteDatabase.OPEN_READONLY).use { copy ->
            copy.rawQuery("SELECT typeId FROM wishes", null).use { rows ->
                assertTrue("la copia no lleva la marca", rows.moveToFirst())
                assertEquals(404044, rows.getInt(0))
            }
        }
    }

    /**
     * A blocked `PRAGMA wal_checkpoint` doesn't throw: it answers `busy = 1` and leaves the log, so
     * a sync, the call ledger or the prefetch writing meanwhile would silently export a stale base.
     *
     * The other connection holds a write, not a read: an Android cursor fills its window and lets
     * the snapshot go, so a reader parked on `moveToFirst()` blocks nothing.
     */
    @Test
    fun aCheckpointSomethingElseIsHoldingUpFailsOutLoud() {
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, CoindexDatabase::class.java, databaseName).build()
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO wishes (typeId, year, issueId, markedAt) VALUES (404044, 2026, 8508, 1)",
        )
        val base = context.getDatabasePath(databaseName)

        val writer = SQLiteDatabase.openDatabase(base.path, null, SQLiteDatabase.OPEN_READWRITE)
        // BEGIN IMMEDIATE: takes the write lock now and holds it until the transaction ends.
        writer.beginTransactionNonExclusive()
        writer.execSQL("INSERT INTO wishes (typeId, year, issueId, markedAt) VALUES (1, 2, 3, 4)")
        try {
            val failure = runCatching { database.checkpoint() }.exceptionOrNull()

            assertTrue("un checkpoint bloqueado tiene que fallar", failure is IllegalStateException)
            assertTrue(failure!!.message.orEmpty().contains("en uso"))
        } finally {
            writer.endTransaction()
            writer.close()
            database.close()
        }
    }
}
