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
 * It ships empty until a type is judged: the schema is the asset. Structural mistakes fail
 * here; a type that a catalog already claims is a suite collision, never a fatal boot red.
 */
class CuratedOrphansTest {
    private val orphans = OrphanSeeds.parse("orphans.json", OrphanFile.read())
    private val catalogs = CatalogSeeds.parseAll(CatalogFiles.all())

    @Test
    fun `the shipped orphans register parses and validates`() {
        assertNull(orphans.validate())
        assertEquals(1, orphans.schemaVersion)
        assertTrue(orphans.orphans.isEmpty(), "todavía no hay veredictos versionados")
    }

    @Test
    fun `no orphan is also an issued catalog member`() {
        assertEquals(emptyList(), orphanCatalogCollisions(orphans, catalogs))
    }

    @Test
    fun `duplicate type ids are rejected`() {
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
    }

    @Test
    fun `a blank reason is rejected`() {
        val error = assertFailsWith<CatalogSeedException> {
            OrphanSeeds.parse(
                "blank.json",
                orphansJson("""{"numista_type_id": 1885, "reason": "  "}"""),
            )
        }
        assertTrue(error.message!!.contains("1885"), error.message!!)
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

    @Test
    fun `a type claimed by a catalog collides in the suite`() {
        val catalogType = catalogs
            .flatMap { it.members }
            .first { it.isIssued && it.numistaTypeId != null }
            .numistaTypeId!!
        val colliding = OrphanSeeds.parse(
            "collision.json",
            orphansJson(
                """{"numista_type_id": $catalogType, "reason": "no tendría lámina"}""",
            ),
        )
        assertEquals(listOf(catalogType), orphanCatalogCollisions(colliding, catalogs))
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
