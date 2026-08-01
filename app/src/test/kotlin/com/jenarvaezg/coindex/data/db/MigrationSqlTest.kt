package com.jenarvaezg.coindex.data.db

import com.jenarvaezg.coindex.data.CatalogFiles
import com.jenarvaezg.coindex.domain.CatalogSeeds
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

    /**
     * La versión 4 mete el metal en la clave (#40, ADR 0018), y la clave **es** la primary key de
     * las disposiciones, así que la tabla se reconstruye. Es el primer rehacer de este proyecto:
     * si la tabla nueva no es letra por letra la que Room deriva de la entidad, la app revienta al
     * abrir la base de datos del coleccionista.
     */
    @Test
    fun `version 4 rebuilds the dispositions exactly as Room declares them`() {
        assertEquals(
            exportedCreateSql(4).getValue("collection_proposal_preferences"),
            CoindexDatabase.VERSION_4_PREFERENCES_TABLE,
        )
        assertEquals(
            exportedColumns(3, "collection_proposal_preferences") + ("metalCode" to "TEXT"),
            exportedColumns(4, "collection_proposal_preferences"),
        )
    }

    @Test
    fun `version 4 touches the dispositions and nothing else`() {
        assertEquals(exportedCreateSql(3).keys, exportedCreateSql(4).keys)
        (exportedCreateSql(4).keys - "collection_proposal_preferences").forEach { table ->
            assertEquals(
                exportedColumns(3, table),
                exportedColumns(4, table),
                "la versión 4 ha tocado $table",
            )
        }
    }

    /**
     * La lista literal que sobrevive a la migración: las claves de los treinta catálogos que
     * viajaban en esta versión.
     *
     * Se compara contra `data/` **hoy**, que es lo único que puede fallar de forma útil — que
     * alguien curara un catálogo entre escribir la lista y publicarla. En cuanto se cure el
     * treinta y uno esta comprobación deja de valer: una migración es historia congelada y no
     * puede seguir a `data/`, así que el día que se rompa lo correcto es borrar **el test**, no
     * tocar la lista.
     */
    @Test
    fun `the carried-over keys are the catalogs shipped at version 4`() {
        val shipped = CatalogSeeds.parseAll(CatalogFiles.all()).map { catalog ->
            PreservedKey(
                family = catalog.family,
                weightMillioz = catalog.key().storedWeightMillioz(),
                finishCode = catalog.key().finishCode(),
                metalCode = catalog.key().metalCode(),
            )
        }

        assertEquals(shipped.toSet(), CoindexDatabase.PRESERVED_KEYS.toSet())
        // Una clave repetida insertaría la misma fila dos veces y rompería la primary key nueva.
        assertEquals(CoindexDatabase.PRESERVED_KEYS.size, CoindexDatabase.PRESERVED_KEYS.toSet().size)
    }
}
