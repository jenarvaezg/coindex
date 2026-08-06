package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.CatalogSeeds
import com.jenarvaezg.coindex.domain.OrphanSeeds
import com.jenarvaezg.coindex.domain.orphanCatalogCollisions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The orphans register is a curator verdict, not the unclassified residue (#121, #133).
 *
 * A type enters it only once a curator signs it, so the shipped list is short on purpose.
 * Structural mistakes fail here; a type that a catalog already claims is a suite collision,
 * never a fatal boot red.
 */
class CuratedOrphansTest {
    private val orphans = OrphanSeeds.parse("orphans.json", OrphanFile.read())
    private val catalogs = CatalogSeeds.parseAll(CatalogFiles.all())

    @Test
    fun `the shipped orphans register parses and validates`() {
        assertNull(orphans.validate())
        assertEquals(1, orphans.schemaVersion)
    }

    /**
     * Cómo se firma un veredicto, que es lo único de él que el código puede vigilar.
     *
     * **Qué** hay firmado y **por qué** vive en `data/orphans.json`, donde cada entrada lleva su
     * motivo en prosa, y en el issue que lo firmó. Repetir aquí la lista no añadía una afirmación:
     * añadía una segunda copia que diverge —ya lo hizo, con los veredictos que el #256 y el #257
     * reabrieron— y una colisión garantizada con cualquier sesión que firme al mismo tiempo.
     *
     * Lo que sí se vigila es la forma, porque es lo que separa un veredicto de una etiqueta: el
     * motivo es prosa y no una palabra. Las lecciones del registro —que un veredicto de intención
     * se reabre por intención (#257), que una casilla sola no es una lámina (#256), y que contrastar
     * al coleccionista **fuera** de Numista evitó firmar tres falsos (#152, #153)— están en
     * `spec.md §0.7`.
     */
    @Test
    fun `a verdict is signed with prose and not with a label`() {
        assertTrue(orphans.orphans.isNotEmpty())
        assertTrue(
            orphans.orphans.all { it.reason.trim().length > 40 },
            "un veredicto se firma con motivo en prosa, no con una etiqueta",
        )
    }

    @Test
    fun `no orphan is also an issued catalog member`() {
        assertEquals(emptyList(), orphanCatalogCollisions(orphans, catalogs))
    }

    /**
     * El parser convierte en excepción lo que `validate()` devuelve, y el fichero y el id llegan al
     * mensaje: sin eso, un error de curación sale como un fallo de arranque sin dónde mirar. Las
     * reglas en sí son de `CuratedOrphansTest` en `domain/`, que es donde vive `validate()`.
     */
    @Test
    fun `a validation error names the file and the type`() {
        val error = assertFailsWith<CatalogSeedException> {
            OrphanSeeds.parse(
                "dup.json",
                orphansJson(
                    """
                    {"numista_type_id": 1885, "reason": "primera"},
                    {"numista_type_id": 1885, "reason": "segunda"}
                    """.trimIndent(),
                ),
            )
        }
        assertTrue(error.message!!.contains("1885"), error.message!!)
        assertTrue(error.message!!.contains("dup.json"), error.message!!)
    }

    @Test
    fun `an unknown field is rejected`() {
        val error = assertFailsWith<CatalogSeedException> {
            OrphanSeeds.parse(
                "typo.json",
                """
                {
                  "schema_version": 1,
                  "updated_at": "2026-08-03",
                  "orphans": [],
                  "extra": true
                }
                """.trimIndent(),
            )
        }
        assertTrue(error.message!!.contains("typo.json"), error.message!!)
    }

    private fun orphansJson(entries: String): String = """
        {
          "schema_version": 1,
          "updated_at": "2026-08-03",
          "orphans": [
            $entries
          ]
        }
    """.trimIndent()
}
