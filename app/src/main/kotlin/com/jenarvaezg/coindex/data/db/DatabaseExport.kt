package com.jenarvaezg.coindex.data.db

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Where a dump waits for the share sheet, declared as a `cache-path` in `file_paths.xml`. Apart from
 * the `plates/` of the printed exports, so clearing one never touches the other.
 */
const val DATABASE_EXPORT_DIR: String = "db"

/**
 * The dump's type for the share sheet. Not `application/x-sqlite3`, which many mail, chat and cloud
 * apps refuse; whatever reads the file at the end ignores the type.
 */
const val DATABASE_MIME_TYPE: String = "application/octet-stream"

private val EXPORT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/** Stands in for a missing version name, so the gap is visible in the file name. */
private const val UNKNOWN_VERSION = "sin-version"

/**
 * The exported database's file name: the APK version, then the day. The version is there because
 * the APK that loads a dump must be the same version or later, since Room only migrates forwards,
 * and nothing on the loading end (`avd-db.sh`) checks it (#548). By day, not second: a second dump
 * the same day replaces the first, as the notebook's export does.
 */
fun databaseExportFileName(
    versionName: String,
    at: LocalDateTime = LocalDateTime.now(),
): String = "coindex-${versionName.ifEmpty { UNKNOWN_VERSION }}-${EXPORT_DAY.format(at)}.db"

/**
 * A checkpointed copy of the raw database for the share sheet (#548), meant for `sqlite3` or
 * `scripts/avd-db.sh restore` on a Mac; the app has no import. The API key doesn't travel: it is
 * Keystore-encrypted in `SharedPreferences`, not in these tables.
 *
 * [checkpoint] is a lambda so the rest (naming, ordering, copying) is file work a JVM test can run.
 */
class DatabaseExport(
    private val source: File,
    private val directory: File,
    private val versionName: () -> String,
    private val checkpoint: () -> Unit,
) {
    /**
     * Writes the dump and returns it. Checkpoint before copying: in WAL mode the recent transactions
     * are in `coindex.db-wal`, and a single file is only complete once the log is folded in.
     */
    suspend fun write(at: LocalDateTime = LocalDateTime.now()): File = withContext(Dispatchers.IO) {
        checkpoint()
        check(source.isFile) { "no hay ninguna base que exportar en ${source.name}" }
        directory.mkdirs()
        val target = File(directory, databaseExportFileName(versionName(), at))
        // One dump at a time: same-day dumps replace each other by name, and this clears older
        // ones. It is a cache, not an archive.
        directory.listFiles()
            ?.filter { it != target && it.name.endsWith(".db") }
            ?.forEach(File::delete)
        source.copyTo(target, overwrite = true)
    }
}
