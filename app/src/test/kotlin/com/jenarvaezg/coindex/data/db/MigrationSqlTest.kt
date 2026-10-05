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
 * Each migration must produce exactly the schema Room exports for its version: Room checks the
 * live schema when it opens the database and throws on any difference, on the collector's phone.
 * What is already stored cost API budget, so migrations only add, except for the dispositions.
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
     * La versión 4 mete el metal en la clave (#40, ADR 0018), que es la primary key de las
     * disposiciones, así que la tabla se reconstruye.
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
     * La versión 5 retira las disposiciones (ADR 0021 §7). Un `DROP` sobre otro nombre no falla al
     * abrir la base de datos: deja una tabla huérfana en el móvil.
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

    /** Las cajas las tecleó el coleccionista, y no caen con las disposiciones (ADR 0021 §11). */
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
     * La versión 6 guarda en columnas cinco campos del cuerpo (#221). Son anulables salvo
     * `readVersion`, cuyo cero por omisión significa «fila sin leer».
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
        // Declaration and not affinity: SQLite refuses a `NOT NULL` column without a default, so
        // both the `NOT NULL` and the `DEFAULT 0` of `readVersion` must be the ones Room writes.
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
     * La versión 7 le da sitio al dinero (ADR 0028): tres tablas y cuatro columnas en la caché de
     * fichas, que `FichaBackfill` rellena desde el cuerpo guardado. La instantánea de la colección
     * no se toca: el `issue_id` ya se lee del cuerpo guardado (#327).
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

    /** La versión 8 le da sitio a los listados de emisiones (#452). */
    @Test
    fun `version 8 creates the two listing tables exactly as Room declares them`() {
        val exported = exportedCreateSql(8)
        val added = exported.keys - exportedCreateSql(7).keys

        assertEquals(setOf("type_issue_reads", "type_issues"), added)
        assertEquals(
            listOf("type_issue_reads", "type_issues").map(exported::getValue),
            CoindexDatabase.VERSION_8_TABLES,
        )
    }

    @Test
    fun `version 8 adds tables and touches nothing else`() {
        exportedCreateSql(7).keys.forEach { table ->
            assertEquals(
                exportedColumns(7, table),
                exportedColumns(8, table),
                "la versión 8 ha tocado $table",
            )
        }
    }

    /** La versión 9 le da su columna al año de emisión de una medalla (#460). */
    @Test
    fun `version 9 adds the issued year column exactly as Room declares it`() {
        val added = exportedColumns(9, "type_meta") - exportedColumns(8, "type_meta").keys

        assertEquals(mapOf("issuedYear" to "INTEGER"), added)
        assertEquals(
            added.map { (column, type) -> "ALTER TABLE `type_meta` ADD COLUMN `$column` $type" },
            CoindexDatabase.VERSION_9_COLUMNS,
        )
        // Anulable: la mayoría de las fichas no son medallas y no tienen fecha de emisión que dar.
        assertTrue("NOT NULL" !in exportedDeclaration(9, "type_meta", "issuedYear"))
    }

    @Test
    fun `version 9 touches the type cache and nothing else`() {
        assertEquals(exportedCreateSql(8).keys, exportedCreateSql(9).keys)
        (exportedCreateSql(8).keys - "type_meta").forEach { table ->
            assertEquals(
                exportedColumns(8, table),
                exportedColumns(9, table),
                "la versión 9 ha tocado $table",
            )
        }
    }

    /**
     * La versión 10 le da su tabla a las casillas marcadas (ADR 0029, #497). La clave es la
     * casilla (tipo, año y emisión), no la variante: un `typeId` suelto no marca una lámina entera.
     */
    @Test
    fun `version 10 creates the wishes table exactly as Room declares it`() {
        val exported = exportedCreateSql(10)
        val added = exported.keys - exportedCreateSql(9).keys

        assertEquals(setOf("wishes"), added)
        assertEquals(added.map(exported::getValue), CoindexDatabase.VERSION_10_TABLES)
        // Las tres columnas de la clave son `NOT NULL` porque SQLite no admite un nulo en una primary
        // key: por eso «esta casilla no declara emisión» viaja como el cero de `Mappers`.
        assertEquals(
            mapOf(
                "typeId" to "INTEGER",
                "year" to "INTEGER",
                "issueId" to "INTEGER",
                "markedAt" to "INTEGER",
            ),
            exportedColumns(10, "wishes"),
        )
        listOf("typeId", "year", "issueId").forEach { column ->
            assertTrue(
                "NOT NULL" in exportedDeclaration(10, "wishes", column),
                "la columna $column de la clave admite nulos",
            )
        }
    }

    @Test
    fun `version 10 adds one table and touches nothing else`() {
        exportedCreateSql(9).keys.forEach { table ->
            assertEquals(
                exportedColumns(9, table),
                exportedColumns(10, table),
                "la versión 10 ha tocado $table",
            )
        }
    }
}
