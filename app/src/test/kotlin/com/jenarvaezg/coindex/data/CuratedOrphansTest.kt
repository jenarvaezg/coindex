package com.jenarvaezg.coindex.data

import com.jenarvaezg.coindex.domain.CatalogSeedException
import com.jenarvaezg.coindex.domain.OrphanSeeds
import com.jenarvaezg.coindex.domain.orphanCatalogCollisions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The orphans register holds curator verdicts, not the unclassified residue (#121, #133). A type
 * that a catalog already claims fails here, in the suite, and never at boot.
 */
class CuratedOrphansTest {
    private val orphans = OrphanSeeds.parse("orphans.json", OrphanFile.read())
    private val catalogs = SHIPPED_CURATION.catalogs

    @Test
    fun `the shipped orphans register parses and validates`() {
        assertNull(orphans.validate())
        assertEquals(1, orphans.schemaVersion)
    }

    /**
     * Sólo se vigila la forma. Qué hay firmado y por qué vive en `data/orphans.json` y en el issue
     * que lo firmó; una copia de la lista aquí diverge (#256, #257). Lecciones en `spec.md` §0.7 y en el ADR 0020.
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
     * El fichero y el id llegan al mensaje para que un error de curación diga dónde mirar. Las
     * reglas se prueban en el `CuratedOrphansTest` de `domain/`, junto a `validate()`.
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
