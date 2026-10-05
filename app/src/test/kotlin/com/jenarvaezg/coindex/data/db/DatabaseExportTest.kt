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
 * Exporting the raw database as one file (#548). The name leads with the version because only an
 * APK of that version or later may load the dump. The WAL is checkpointed into the base before the
 * copy, or the copy would miss the last transactions (as `scripts/avd-db.sh` guards against); the
 * checkpoint is a lambda so the test can see that it ran, and when.
 */
class DatabaseExportTest {
    private val at = LocalDateTime.of(2026, 8, 16, 14, 30, 7)

    @Test
    fun `the name says which APK wrote it and on what day`() {
        assertEquals("coindex-1.4.8-2026-08-16.db", databaseExportFileName("1.4.8", at))
    }

    /** `coindex-2026-08-16.db` would read like a complete name. */
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

    /** The usual case is a coin just added, and the later export is the one wanted. */
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

    /** A base is a few megabytes, and this cache directory is not an archive. */
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

    /** A phone that never opened the base has none, and claiming «se exportó» would mislead. */
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
