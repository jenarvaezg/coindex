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
     *
     * N#3416 shows the verdict is per denomination, not per metal. Asked about Reichsmark silver
     * as a block, the collector kept the 5 Reichsmark — cured as a plate of its own — and dropped
     * the 2: 24 year-and-mint combinations over four struck years, 129 million pieces, and the
     * piece in the collection has no year recorded, so a by-year plate would have read 0 of 4.
     *
     * The last two are modern one-offs, and each one closed a different door on the way in. The
     * Orwell 2 pounds (N#451057) died of volume — 829 type pages under that denomination, the
     * #154 shape — and the Dune ounce (N#571460) of calendar: four Numista types and all four are
     * 2026, so there is no sequence to count. Dune was offered an ADR 0022 programme file first
     * and it was refused for a measured reason: a programme only surfaces on the plate of a
     * catalog that touches its types, and no plate touches these.
     *
     * The two Mexican silver pieces of #242 are the block where the **father** answered, and both
     * were writable before he did: the 1 Peso «Tepalcate» (N#3550) is 11 struck years without a gap
     * and the 8 Reales of Charles IV (N#18852) is 18, so a date run was on the table for each — for
     * the reales in two forms, by year or by assayer. He follows neither (6 August 2026, through
     * Jose), and the shape had already lost its only alternative: an agrupación declares no weight,
     * so by ADR 0013 as amended it cannot hold six Mexican pieces of five different weights.
     *
     * The sixteen of 6 August 2026 are the largest block ever signed at once, and all sixteen are
     * the **father's answer** to the five questions of #216 — not a measurement of ours. Four shapes
     * were on the table and he closed each one in his own words: the Mexican Olympic 25 pesos of 68
     * («juegos olímpicos, sola»); five pieces of old circulation silver, three of them writable date
     * runs — the ½ rupee over four struck years, the 20 kopeks over three, the 5 kronor over
     * eighteen — that he keeps as souvenirs («las demás las compré por baratas, podría venderlas»);
     * and ten commemoratives of one country each, where the theme that would have joined two of them
     * is abandoned by the only person who ever had it («pensé si, en el tema de animales, pero es que
     * me cansé de Catawiki»). Two neighbours of that block are deliberately **not** here: the
     * Maria Theresa thaler, which he reads as part of a «historia del real» theme with the 8 reales,
     * and the 25 ringgit of Malaysia, whose own ficha says it shipped in a two-coin set with the 15
     * ringgit — both are open questions and an open question is never signed.
     *
     * **Two** Venezuelan silver commemoratives are left of the five #247 signed, and the block is
     * the one where the measurement itself was wrong (#256). The Catálogo Numismático de Venezuela
     * does draw the commemorative boundary — it labels the design and gives every piece a «Motivo
     * conmemorativo» field — so the three 100 bolívares (N#19880, N#27573, N#34721) came out of
     * here and became a plate of four with the 1983 the first pass had missed. What is left is not
     * an absence of boundary but a denomination with one silver slot in it: the 10 bolívares
     * (N#14538) has two commemorative designs and the other, 1930, is gold, and the 75 bolívares
     * (N#18940) is the only design its denomination ever had. One row is not a plate, and the
     * weight was never the argument.
     */
    @Test
    fun `the register carries the signed verdicts`() {
        assertEquals(
            listOf(
                1_933, 1_952, 3_416, 3_550, 3_855, 6_918, 10_613, 11_440, 12_454, 12_994, 14_018,
                14_538, 15_357, 18_852, 18_940, 26_190, 38_130, 44_085, 59_404, 90_456, 98_259,
                131_809, 132_242, 277_960, 291_255, 387_614, 451_057, 470_766, 571_460,
            ),
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
