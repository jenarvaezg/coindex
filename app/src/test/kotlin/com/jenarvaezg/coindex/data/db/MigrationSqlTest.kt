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

    /** Everything Room writes after a column's name in `CREATE TABLE`: type, `NOT NULL`, default. */
    private fun exportedDeclaration(version: Int, table: String, column: String): String =
        exportedCreateSql(version)
            .getValue(table)
            .substringAfter("`$column` ")
            .substringBefore(",")
            .trim()

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
     * La versión 5 retira las disposiciones (ADR 0021 §7): un `DROP`, hacia delante y sin rescatar
     * nada. La tabla que desaparece tiene que ser exactamente la que el esquema exportado deja de
     * declarar — un `DROP` sobre otro nombre no falla al abrir la base de datos, se queda callado
     * con una tabla huérfana en el móvil del coleccionista.
     */
    @Test
    fun `version 5 drops exactly the table the schema stops declaring`() {
        val dropped = exportedCreateSql(4).keys - exportedCreateSql(5).keys

        assertEquals(setOf("collection_proposal_preferences"), dropped)
        assertEquals(
            dropped.map { "DROP TABLE `$it`" },
            listOf(CoindexDatabase.VERSION_5_DROP),
        )
    }

    /**
     * Las cajas del coleccionista **no** caen con ella (ADR 0021 §11): son lo único que él tecleó,
     * y la colección sincronizada y la caché de tipos siguen costando presupuesto de API.
     */
    @Test
    fun `version 5 leaves the boxes, the snapshot and the type cache intact`() {
        assertEquals(
            setOf(
                "collected_items",
                "type_meta",
                "own_groupings",
                "own_grouping_members",
                "api_call_log",
            ),
            exportedCreateSql(5).keys,
        )
        exportedCreateSql(5).keys.forEach { table ->
            assertEquals(
                exportedColumns(4, table),
                exportedColumns(5, table),
                "la versión 5 ha tocado $table",
            )
        }
    }

    /**
     * La versión 6 mete en columnas los cinco campos que se parseaban del cuerpo en cada lectura
     * (#221). Aditiva y anulable como la 3, con un `readVersion` que **no** es anulable: el cero
     * por omisión es «a esta fila no la ha leído nadie», que es justo lo que hay al otro lado.
     */
    @Test
    fun `version 6 adds the five read columns and the marker exactly as Room declares them`() {
        val added = exportedColumns(6, "type_meta") - exportedColumns(5, "type_meta").keys

        assertEquals(
            mapOf(
                "issuerName" to "TEXT",
                "composition" to "TEXT",
                "sizeMillimetres" to "REAL",
                "category" to "TEXT",
                "numistaUrl" to "TEXT",
                "readVersion" to "INTEGER",
            ),
            added,
        )
        // Declaration and not affinity: `readVersion` is the first `NOT NULL` column this project
        // has ever added, and SQLite refuses one without a default. Both halves — the `NOT NULL`
        // and the `DEFAULT 0` — have to be the ones Room itself writes.
        assertEquals(
            added.keys.map { column ->
                "ALTER TABLE `type_meta` ADD COLUMN `$column` " +
                    exportedDeclaration(6, "type_meta", column)
            },
            CoindexDatabase.VERSION_6_COLUMNS,
        )
    }

    @Test
    fun `version 6 touches the type cache and nothing else`() {
        assertEquals(exportedCreateSql(5).keys, exportedCreateSql(6).keys)
        (exportedCreateSql(6).keys - "type_meta").forEach { table ->
            assertEquals(
                exportedColumns(5, table),
                exportedColumns(6, table),
                "la versión 6 ha tocado $table",
            )
        }
    }

    /**
     * La versión 7 le da sitio al dinero (ADR 0028): tres tablas que el teléfono no tenía y cuatro
     * columnas más en la caché de fichas.
     *
     * Aditiva de punta a punta, y sin una sola llamada: las columnas las rellena después
     * `FichaBackfill` desde los cuerpos que cada fila ya guarda, como la 3 y la 6. El `issue_id` que
     * el #327 esperaba migrar ya se lee del cuerpo guardado, así que la instantánea de la colección
     * no se toca.
     */
    @Test
    fun `version 7 creates the three money tables exactly as Room declares them`() {
        val exported = exportedCreateSql(7)
        val added = exported.keys - exportedCreateSql(6).keys

        assertEquals(setOf("issue_price_reads", "issue_prices", "metal_spot"), added)
        assertEquals(
            listOf("issue_price_reads", "issue_prices", "metal_spot").map(exported::getValue),
            CoindexDatabase.VERSION_7_TABLES,
        )
    }

    @Test
    fun `version 7 adds the four ficha columns exactly as Room declares them`() {
        val added = exportedColumns(7, "type_meta") - exportedColumns(6, "type_meta").keys

        assertEquals(
            mapOf(
                "thicknessMillimetres" to "REAL",
                "demonetized" to "INTEGER",
                "hands" to "TEXT",
                "mints" to "TEXT",
            ),
            added,
        )
        assertEquals(
            added.map { (column, type) -> "ALTER TABLE `type_meta` ADD COLUMN `$column` $type" },
            CoindexDatabase.VERSION_7_COLUMNS,
        )
        // Anulables las cuatro: una columna `NOT NULL` sin defecto no se puede añadir en SQLite, y el
        // silencio de Numista sobre la desmonetización no es un «sigue siendo dinero».
        added.keys.forEach { column ->
            assertTrue(
                "NOT NULL" !in exportedDeclaration(7, "type_meta", column),
                "la columna $column de la versión 7 no es anulable",
            )
        }
    }

    /** Y no toca nada más: la colección sincronizada y las cajas del coleccionista siguen costando. */
    @Test
    fun `version 7 touches the type cache and adds tables, and nothing else`() {
        (exportedCreateSql(6).keys - "type_meta").forEach { table ->
            assertEquals(
                exportedColumns(6, table),
                exportedColumns(7, table),
                "la versión 7 ha tocado $table",
            )
        }
    }
}
