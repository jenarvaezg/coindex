package com.jenarvaezg.coindex.data.db

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where a dump waits for the share sheet, declared as a `cache-path` in `file_paths.xml`.
 *
 * Not the `plates/` every other export uses: a plate, a sheet and the notebook are things made for a
 * person to look at, and this is the base itself, made for `sqlite3` and for `scripts/avd-db.sh`.
 * Keeping them apart means clearing one never touches the other.
 */
const val DATABASE_EXPORT_DIR: String = "db"

/**
 * What the dump is announced as to whatever the share sheet hands it to.
 *
 * `application/octet-stream` and not `application/x-sqlite3`: the destination is a mail client, a
 * chat or a cloud folder on the way to a Mac, and the precise type is one half of them decline to
 * accept. Nothing on the receiving end reads the type anyway — `sqlite3` and `avd-db.sh` read the
 * file.
 */
const val DATABASE_MIME_TYPE: String = "application/octet-stream"

private val EXPORT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** What a dump with no version to declare is called, so the gap is read rather than guessed. */
private const val UNKNOWN_VERSION = "sin-version"

/**
 * What the exported base is called: the APK first, then the day.
 *
 * The name carries the one rule a dump has to travel with (#548) — **the APK that loads it must be of
 * a version equal to or later than the one that wrote it**, because Room's migrations only run
 * forwards. Nothing enforces that on the loading end; `avd-db.sh` pushes whatever it is given. So the
 * version is in the file name, where whoever restores it reads it without opening anything.
 *
 * The day and not the second: a dump is the collection as it stood, and two of them in one day are
 * the same answer asked twice. The second replaces the first, as the notebook's export does.
 */
fun databaseExportFileName(
    versionName: String,
    at: LocalDateTime = LocalDateTime.now(),
): String = "coindex-${versionName.ifEmpty { UNKNOWN_VERSION }}-${EXPORT_DAY.format(at)}.db"

/**
 * A copy of the raw base, checkpointed, ready for the share sheet (#548).
 *
 * The destination is a Mac and an emulator, never another phone: `sqlite3` answers a measurement off
 * the file, and `scripts/avd-db.sh restore` seeds an AVD with a collection that would otherwise cost
 * an API budget nobody has left. There is no import in the app, and none is implied by this.
 *
 * **The key does not travel.** It is encrypted against the device Keystore and lives in
 * `SharedPreferences`, not in any of these tables, so a dump costs no one their monthly allowance —
 * the AVD is signed up by hand as it always was, without touching the network.
 *
 * [checkpoint] arrives as a lambda rather than the database itself: what it has to do is fold the
 * write-ahead log back into the base, and everything else here — the naming, the order, the copy —
 * is file work a JVM test can hold in its hands.
 */
class DatabaseExport(
    private val source: File,
    private val directory: File,
    private val versionName: () -> String,
    private val checkpoint: () -> Unit,
) {
    /**
     * Writes the dump and returns it.
     *
     * The checkpoint comes first and the copy second, which is the whole of the correctness here:
     * Room runs in WAL mode, so the base on its own is the collection as of the last checkpoint and
     * the recent transactions are in `coindex.db-wal` beside it. `avd-db.sh` carries all three files
     * for exactly that reason; one file crossing the share sheet can only be right if the log has
     * been folded in before it is read.
     */
    suspend fun write(at: LocalDateTime = LocalDateTime.now()): File = withContext(Dispatchers.IO) {
        checkpoint()
        check(source.isFile) { "no hay ninguna base que exportar en ${source.name}" }
        directory.mkdirs()
        val target = File(directory, databaseExportFileName(versionName(), at))
        // One dump at a time. Two on the same day already replace each other by name; what this
        // clears is the ones from other days, which nobody wants and which would otherwise sit here
        // for ever — the collection weighs a few megabytes and it is a **cache**, not an archive.
        // The bytes have already been where they were going.
        directory.listFiles()
            ?.filter { it != target && it.name.endsWith(".db") }
            ?.forEach(File::delete)
        source.copyTo(target, overwrite = true)
    }
}
