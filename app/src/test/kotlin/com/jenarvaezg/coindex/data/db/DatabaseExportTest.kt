package com.jenarvaezg.coindex.data.db

import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * The raw base leaving the phone as one file, and named so the load rule can be read (#548).
 *
 * Two things are worth a test here and neither of them is Android. The **name** carries the rule the
 * issue states — the APK that loads a dump must be of a version equal to or later than the one that
 * wrote it — so the version comes first and the day after it; a dump nobody can date to a version is
 * a dump nobody can safely load. And the **order**: the WAL is folded back into the base *before* the
 * bytes are copied, because a copy taken first is a base missing the last transactions, which is
 * exactly the failure `scripts/avd-db.sh` was written to avoid.
 *
 * The checkpoint arrives as a lambda for that reason: what it does on a real phone is one `PRAGMA`,
 * and what a test needs to know is that it ran, and when.
 */
class DatabaseExportTest {
    private val at = LocalDateTime.of(2026, 8, 16, 14, 30, 7)

    @Test
    fun `the name says which APK wrote it and on what day`() {
        assertEquals("coindex-1.4.8-2026-08-16.db", databaseExportFileName("1.4.8", at))
    }

    /**
     * The version is the whole point of the name, so its absence is written down rather than
     * quietly dropped: a file called `coindex-2026-08-16.db` reads like a complete name.
     */
    @Test
    fun `a dump with no version to declare says so instead of losing the field`() {
        assertEquals("coindex-sin-version-2026-08-16.db", databaseExportFileName("", at))
    }

    @Test
    fun `the base is checkpointed before it is copied, never after`() = runTest {
        val directory = temporaryDirectory()
        val source = File(directory, "coindex.db").apply { writeText("sin la última transacción") }
        val export = DatabaseExport(
            source = source,
            directory = File(directory, "salida"),
            versionName = { "1.4.8" },
            // What a checkpoint does: it moves what was only in the log into the base itself.
            checkpoint = { source.writeText("con la última transacción") },
        )

        val copy = export.write(at)

        assertEquals("con la última transacción", copy.readText())
    }

    @Test
    fun `the copy lands in a directory the export makes for itself`() = runTest {
        val directory = temporaryDirectory()
        val source = File(directory, "coindex.db").apply { writeText("la colección") }
        val target = File(File(directory, "sin"), "crear")
        val export = DatabaseExport(source, target, { "1.4.8" }, checkpoint = {})

        val copy = export.write(at)

        assertTrue(copy.exists())
        assertEquals(File(target, "coindex-1.4.8-2026-08-16.db"), copy)
    }

    /**
     * Two exports on one day is the normal case — a coin was just added — and the second is the one
     * that is wanted. Overwriting is the honest outcome, as it is for the notebook.
     */
    @Test
    fun `a second export the same day replaces the first`() = runTest {
        val directory = temporaryDirectory()
        val source = File(directory, "coindex.db").apply { writeText("primera") }
        val export = DatabaseExport(source, File(directory, "salida"), { "1.4.8" }, checkpoint = {})

        export.write(at)
        source.writeText("segunda")
        val copy = export.write(at)

        assertEquals("segunda", copy.readText())
    }

    /**
     * The cache holds one dump and not one per day of exporting: a base is a few megabytes and this
     * directory is nobody's archive.
     */
    @Test
    fun `an export clears the dumps of other days`() = runTest {
        val directory = temporaryDirectory()
        val source = File(directory, "coindex.db").apply { writeText("la colección") }
        val output = File(directory, "salida")
        val export = DatabaseExport(source, output, { "1.4.8" }, checkpoint = {})

        export.write(at.minusDays(3))
        val copy = export.write(at)

        assertEquals(listOf(copy.name), output.listFiles().orEmpty().map { it.name })
    }

    /**
     * There is no base on a phone that has never opened one, and «se exportó» over an empty share
     * sheet would be the worst of the three outcomes.
     */
    @Test
    fun `a base that is not there fails saying so`() = runTest {
        val directory = temporaryDirectory()
        val export = DatabaseExport(
            source = File(directory, "coindex.db"),
            directory = File(directory, "salida"),
            versionName = { "1.4.8" },
            checkpoint = {},
        )

        val failure = assertFailsWith<IllegalStateException> { export.write(at) }

        assertTrue("coindex.db" in failure.message.orEmpty())
    }

    private fun temporaryDirectory(): File = Files.createTempDirectory("coindex-export").toFile()
}
