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
     * The five verdicts signed by #146 §3, plus the silver 5 Deutsche Mark of #216.
     *
     * Pinned one by one on purpose: adding or dropping a verdict is a curation decision, so it
     * costs an edit here. Two cases the census (#120) filed as solitude stayed out after being
     * contrasted outside Numista — the RAM Koala (N#557132) is the third year of a live annual
     * programme and the Haitian gourdes (N#19085, N#19328) were sold in a four-coin case — and
     * the two unpublished medals (N#578835, N#581856) wait for their referee, because an id a
     * referee may still delete is never versioned.
     *
     * N#1933 is the first verdict where the measurement argued *for* a plate and the collector
     * closed the door anyway: 19 struck years over four mints, so 19 slots by year or 73 by year
     * and mint were both writable, and «no voy a seguir eso» outranks either. The criterion is
     * the same one that filed the Kennedy Half Dollar — bulk circulation silver nobody chases.
     */
    @Test
    fun `the register carries the signed verdicts`() {
        assertEquals(
            listOf(1_933, 6_918, 131_809, 132_242, 291_255, 470_766),
            orphans.orphans.map { it.numistaTypeId }.sorted(),
        )
        assertTrue(
            orphans.orphans.all { it.reason.trim().length > 40 },
            "un veredicto se firma con motivo en prosa, no con una etiqueta",
        )
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
