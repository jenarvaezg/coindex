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
 * One file carries the whole collection across the share sheet (#548).
 *
 * `DatabaseExportTest` pins the naming and the order with a checkpoint it can hold in its hand; this
 * is the half that needs a real SQLite underneath, because the risk is not in the copying — it is in
 * whether `PRAGMA wal_checkpoint(TRUNCATE)` does anything at all. Room opens in WAL mode, so a
 * transaction lives in `…-wal` until something folds it back in, and a dump taken without that is a
 * base missing exactly the coins that were added last. `scripts/avd-db.sh` carries three files to
 * sidestep this; the share sheet carries one, so the fold has to happen here.
 *
 * The assertion is the row read back **out of the copy**, opened as its own database: a `-wal` of
 * zero bytes would also be true of a checkpoint that quietly did nothing to a base that never had
 * anything to write.
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
     * A checkpoint somebody else was in the way of is a failure and not a quiet older dump.
     *
     * `PRAGMA wal_checkpoint` does not throw: it answers `busy = 1` and leaves the log where it was.
     * A write in flight is exactly what a sync, the call ledger or the prefetch look like from here,
     * and all three run while the collector is sitting on Ajustes — so the base that reached the
     * share sheet would be the collection as of some earlier moment, with no sign of it anywhere.
     *
     * The other connection holds a **write** and not a read: an Android cursor fills its window and
     * lets the snapshot go, so a reader parked on `moveToFirst()` blocks nothing (which is how this
     * test was written first, and it went green against a checkpoint that reported `busy = 0`).
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
        // BEGIN IMMEDIATE: the write lock is taken now and held until this transaction ends, which
        // is what a checkpoint cannot get past.
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
