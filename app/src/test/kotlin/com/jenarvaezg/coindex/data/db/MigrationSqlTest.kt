package com.jenarvaezg.coindex.data.db

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The migration to version 2 must create exactly the tables Room derives from the entities.
 *
 * Room compares the live schema against the exported one at open time and throws if they differ
 * by a single keyword — on the phone, on a database holding a collection that cost API budget to
 * fetch, where the only remaining escape is destructive. Comparing the two here means that
 * failure surfaces on this machine instead.
 */
class MigrationSqlTest {
    private fun schema(version: Int) = File(
        "schemas/com.jenarvaezg.coindex.data.db.CoindexDatabase/$version.json",
    )

    private fun entities(version: Int) = schema(version).let { file ->
        assertTrue(file.exists(), "falta el esquema exportado ${file.absolutePath}")
        Json.parseToJsonElement(file.readText())
            .jsonObject.getValue("database")
            .jsonObject.getValue("entities")
            .jsonArray
    }

    private fun exportedCreateSql(version: Int): Map<String, String> =
        entities(version).associate { entity ->
            val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
            val sql = entity.jsonObject.getValue("createSql").jsonPrimitive.content
            table to sql.replace("\${TABLE_NAME}", table)
        }

    /** Column name to its declared SQL type, for one table of one exported version. */
    private fun exportedColumns(version: Int, table: String): Map<String, String> =
        entities(version)
            .first { it.jsonObject.getValue("tableName").jsonPrimitive.content == table }
            .jsonObject.getValue("fields")
            .jsonArray
            .associate { field ->
                field.jsonObject.getValue("columnName").jsonPrimitive.content to
                    field.jsonObject.getValue("affinity").jsonPrimitive.content
            }

    @Test
    fun `the added tables are created exactly as Room declares them`() {
        val exported = exportedCreateSql(2)
        val added = listOf("own_groupings", "own_grouping_members")

        assertEquals(
            added.map(exported::getValue),
            CoindexDatabase.VERSION_2_TABLES,
        )
    }

    @Test
    fun `version 2 adds those tables and nothing else`() {
        // The collection snapshot, the type cache, the dispositions and the call log stay put:
        // this migration is additive or it is a data loss.
        assertEquals(
            setOf(
                "collected_items",
                "type_meta",
                "collection_proposal_preferences",
                "own_groupings",
                "own_grouping_members",
                "api_call_log",
            ),
            exportedCreateSql(2).keys,
        )
    }

    @Test
    fun `version 3 adds the two thumbnail columns exactly as Room declares them`() {
        val added = exportedColumns(3, "type_meta") - exportedColumns(2, "type_meta").keys

        assertEquals(mapOf("obverseThumbnailUrl" to "TEXT", "reverseThumbnailUrl" to "TEXT"), added)
        assertEquals(
            added.keys.map { "ALTER TABLE `type_meta` ADD COLUMN `$it` TEXT" },
            CoindexDatabase.VERSION_3_COLUMNS,
        )
    }

    @Test
    fun `version 3 touches the type cache and nothing else`() {
        assertEquals(exportedCreateSql(2).keys, exportedCreateSql(3).keys)
        (exportedCreateSql(3).keys - "type_meta").forEach { table ->
            assertEquals(
                exportedColumns(2, table),
                exportedColumns(3, table),
                "la versión 3 ha tocado $table",
            )
        }
    }
}
