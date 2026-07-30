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
    private val schema = File(
        "schemas/com.jenarvaezg.coindex.data.db.CoindexDatabase/2.json",
    )

    private fun exportedCreateSql(): Map<String, String> {
        assertTrue(schema.exists(), "falta el esquema exportado ${schema.absolutePath}")
        return Json.parseToJsonElement(schema.readText())
            .jsonObject.getValue("database")
            .jsonObject.getValue("entities")
            .jsonArray
            .associate { entity ->
                val table = entity.jsonObject.getValue("tableName").jsonPrimitive.content
                val sql = entity.jsonObject.getValue("createSql").jsonPrimitive.content
                table to sql.replace("\${TABLE_NAME}", table)
            }
    }

    @Test
    fun `the added tables are created exactly as Room declares them`() {
        val exported = exportedCreateSql()
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
            exportedCreateSql().keys,
        )
    }
}
